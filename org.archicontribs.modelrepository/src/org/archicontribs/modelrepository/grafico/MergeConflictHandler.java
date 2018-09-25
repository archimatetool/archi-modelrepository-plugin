/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.archicontribs.modelrepository.dialogs.ConflictsDialog;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.PersonIdent;
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
    
    private List<MergeObjectInfo> fMergeObjectInfos;
    
    private IArchimateModel fOurModel, fTheirModel;

    public MergeConflictHandler(MergeResult mergeResult, IArchiRepository repo, Shell shell) {
        fMergeResult = mergeResult;
        fArchiRepo = repo;
        fShell = shell;
    }
    
    public boolean checkForMergeConflicts() throws IOException {
        // This could be null if Rebase is the default behaviour on the repo rather than merge when a Pull is done
        if(fMergeResult == null) {
            throw new IOException("MergeResult was null"); //$NON-NLS-1$
        }
        
        Dialog dialog = new ConflictsDialog(fShell, this);
        return dialog.open() == Window.OK ? true : false;
    }
    
    public MergeResult getMergeResult() {
        return fMergeResult;
    }
    
    public List<MergeObjectInfo> getMergeObjectInfos() throws IOException {
        if(fMergeObjectInfos == null) {
            fMergeObjectInfos = new ArrayList<MergeObjectInfo>();
            for(String xmlPath : fMergeResult.getConflicts().keySet()) {
                fMergeObjectInfos.add(new MergeObjectInfo(xmlPath, fArchiRepo));
            }
        }
        
        return fMergeObjectInfos;
    }
    
    public IArchimateModel getOurModel() throws IOException {
        if(fOurModel == null) {
            fOurModel = extractModel(IGraficoConstants.REFS_HEADS_MASTER);
        }
        return fOurModel;
    }
    
    public IArchimateModel getTheirModel() throws IOException {
        if(fTheirModel == null) {
            fTheirModel = extractModel(IGraficoConstants.ORIGIN_MASTER);
        }
        return fTheirModel;
    }

    private IArchimateModel extractModel(String ref) throws IOException {
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
                throw new IOException("Could not get commit."); //$NON-NLS-1$
            }
            
            // Walk the tree and get the contents of the commit
            try(TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(commit.getTree());
                treeWalk.setRecursive(true);

                while(treeWalk.next()) {
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
    
    public String getConflictsAsString() {
        Map<String, int[][]> allConflicts = fMergeResult.getConflicts();
        
        String message = ""; //$NON-NLS-1$
        
        for(String path : allConflicts.keySet()) {
            message += "File: " + path + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
            
            int[][] c = allConflicts.get(path);
            for(int i = 0; i < c.length; ++i) {
                message += "  Conflict #" + (i + 1) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
                for(int j = 0; j < (c[i].length) - 1; ++j) {
                    if(c[i][j] >= 0) {
                        message += "   Chunk for " + //$NON-NLS-1$
                        fMergeResult.getMergedCommits()[j] +
                        " starts on line #" + //$NON-NLS-1$
                        c[i][j] + "\n"; //$NON-NLS-1$
                    }
                }
            }
            
            message += "\n\n"; //$NON-NLS-1$
        }
        
        return message;
    }
    
    public void merge() throws IOException, GitAPIException {
        List<String> ours = new ArrayList<>();
        List<String> theirs = new ArrayList<>();
        
        for(MergeObjectInfo info : getMergeObjectInfos()) {
            // Ours
            if(info.getChoice() == MergeObjectInfo.OURS) {
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
    
    // Check out conflicting files either from us or them
    private void checkout(Git git, Stage stage, List<String> paths) throws GitAPIException {
        CheckoutCommand checkoutCommand = git.checkout();
        checkoutCommand.setStage(stage);
        checkoutCommand.addPaths(paths);
        checkoutCommand.call();
    }
    
    public void mergeAndCommit(String commitMessage, boolean amend) throws IOException, GitAPIException {
        merge();
        
        try(Git git = Git.open(fArchiRepo.getLocalRepositoryFolder())) {
            // Add to index all files
            AddCommand addCommand = git.add();
            addCommand.addFilepattern("."); //$NON-NLS-1$
            addCommand.setUpdate(false);
            addCommand.call();
            
            // Commit
            CommitCommand commitCommand = git.commit();
            PersonIdent userDetails = fArchiRepo.getUserDetails();
            commitCommand.setAuthor(userDetails);
            commitCommand.setMessage(commitMessage);
            commitCommand.setAmend(amend);
            commitCommand.call();
        }
    }
    
    public IArchiRepository getArchiRepository() {
        return fArchiRepo;
    }
    
    public void resetToRemoteState() throws IOException, GitAPIException {
        resetToState(IGraficoConstants.ORIGIN_MASTER);
    }
    
    public void resetToLocalState() throws IOException, GitAPIException {
        resetToState(IGraficoConstants.REFS_HEADS_MASTER);
    }
    
    private void resetToState(String ref) throws IOException, GitAPIException {
        // Reset HARD  which will lose all changes
        try(Git git = Git.open(fArchiRepo.getLocalRepositoryFolder())) {
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef(ref);
            resetCommand.setMode(ResetType.HARD);
            resetCommand.call();
        }
    }
}
