/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.merge;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.swt.widgets.Shell;

import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Handle Merge Conflicts on a MergeResult
 * 
 * @author Phillip Beauvoir
 */
public class MergeConflictHandler {
    
    private IArchiRepository fArchiRepo;
    private MergeResult fMergeResult;
    private Shell fShell;
    
    private String fTheirRef;
    
    private List<MergeObjectInfo> fMergeObjectInfos;
    
    private IArchimateModel fOurModel, fTheirModel;
    
    private IProgressMonitor fProgressMonitor;

    public MergeConflictHandler(MergeResult mergeResult, String theirRef, IArchiRepository repo, Shell shell) {
        fMergeResult = mergeResult;
        fArchiRepo = repo;
        fTheirRef = theirRef;
        fShell = shell;
    }
    
    public void init(IProgressMonitor pm) throws IOException, GitAPIException {
        // This could be null if Rebase is the default behaviour on the repo rather than merge when a Pull is done
        if(fMergeResult == null) {
            throw new IOException("MergeResult was null"); //$NON-NLS-1$
        }
        
        fProgressMonitor = pm;

        // Our model is the current loaded one
        fOurModel = fArchiRepo.locateModel();
        if(fOurModel == null) {
            throw new IOException(Messages.MergeConflictHandler_0);
        }
        
        // Their model needs to be extracted
        fTheirModel = extractModel(getTheirRef());
        
        // Create Merge Infos
        fMergeObjectInfos = new ArrayList<MergeObjectInfo>();
        for(String xmlPath : fMergeResult.getConflicts().keySet()) {
            fMergeObjectInfos.add(new MergeObjectInfo(xmlPath, this));
        }
    }
    
    public boolean openConflictsDialog() {
        Dialog dialog = new ConflictsDialog(fShell, this);
        return dialog.open() == Window.OK ? true : false;
    }
    
    /**
     * Merge ours and theirs but don't commit
     * @throws IOException
     * @throws GitAPIException
     */
    public void merge() throws IOException, GitAPIException {
        List<String> ours = new ArrayList<>();
        List<String> theirs = new ArrayList<>();
        
        for(MergeObjectInfo info : getMergeObjectInfos()) {
            // Ours
            if(info.getUserChoice() == MergeObjectInfo.OURS) {
                ours.add(info.getXMLPath());
            }
            // Theirs
            else {
                theirs.add(info.getXMLPath());
            }
        }
        
        try(Git git = Git.open(fArchiRepo.getLocalRepositoryFolder())) {
            if(!ours.isEmpty()) {
                checkout(git, Stage.OURS, ours);
            }
            if(!theirs.isEmpty()) {
                checkout(git, Stage.THEIRS, theirs);
            }
        }
    }
    
    String getLocalRef() {
        // We assume that we are at HEAD
        return IGraficoConstants.HEAD;
    }
    
    String getTheirRef() {
        return fTheirRef;
    }
    
    public void resetToLocalState() throws IOException, GitAPIException {
        // Reset HARD  which will lose all changes
        try(Git git = Git.open(fArchiRepo.getLocalRepositoryFolder())) {
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef(getLocalRef());
            resetCommand.setMode(ResetType.HARD);
            resetCommand.call();
        }
    }
    
    // Check out conflicting files either from us or them
    private void checkout(Git git, Stage stage, List<String> paths) throws GitAPIException {
        CheckoutCommand checkoutCommand = git.checkout();
        checkoutCommand.setStage(stage);
        checkoutCommand.addPaths(paths);
        checkoutCommand.call();
    }
    
    IArchiRepository getArchiRepository() {
        return fArchiRepo;
    }
    
    List<MergeObjectInfo> getMergeObjectInfos() {
        return fMergeObjectInfos;
    }
    
    IArchimateModel getOurModel() {
        return fOurModel;
    }
    
    IArchimateModel getTheirModel() {
        return fTheirModel;
    }

    /**
     * Extract a model from either our latest commit or their latest online commit
     * ref = "refs/head/master" or "origin/master"
     * @throws CanceledException 
     */
    private IArchimateModel extractModel(String ref) throws IOException, CanceledException {
        File tmpFolder = new File(System.getProperty("java.io.tmpdir"), "org.archicontribs.modelrepository.tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        FileUtils.deleteFolder(tmpFolder);
        tmpFolder.mkdirs();
        
        try(Repository repository = Git.open(fArchiRepo.getLocalRepositoryFolder()).getRepository()) {
            RevCommit commit = null;
            
            // Get the commit
            // A RevWalk walks over commits based on some filtering that is defined
            try(RevWalk revWalk = new RevWalk(repository)) {
                // We are interested in the origin master branch
                ObjectId objectID = repository.resolve(ref);
                if(objectID != null) {
                    commit = revWalk.parseCommit(objectID);
                }
                
                revWalk.dispose();
            }
            
            if(commit == null) {
                throw new IOException(Messages.MergeConflictHandler_1);
            }
            
            // Walk the tree and get the contents of the commit
            try(TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);

                while(treeWalk.next()) {
                    if(fProgressMonitor != null && fProgressMonitor.isCanceled()) {
                        throw new CanceledException(Messages.MergeConflictHandler_2);
                    }
                    
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    
                    File file = new File(tmpFolder, treeWalk.getPathString());
                    file.getParentFile().mkdirs();
                    
                    try(FileOutputStream out = new FileOutputStream(file)) {
                        loader.copyTo(out);
                    }
                }
            }
        }

        // Load it
        GraficoModelImporter importer = new GraficoModelImporter(tmpFolder);
        IArchimateModel model = importer.importAsModel();
        FileUtils.deleteFolder(tmpFolder);
        return model;
    }
    
}
