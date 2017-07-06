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

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Handler to Resolve Conflict Issues on Grafico Import
 * 
 * @author Phillip Beauvoir
 */
public class ConflictResolutionHandler {

    private static class ProblemPair {
        URI missingObjectURI;
        IIdentifier parentObject;

        ProblemPair(URI missingObjectURI, IIdentifier parentObject) {
            this.missingObjectURI = missingObjectURI;
            this.parentObject = parentObject;
        }
    }
    
    private IArchimateModel fModel;
    
    private List<ProblemPair> fProblems;
    
    public ConflictResolutionHandler(IArchimateModel model) {
        fModel = model;
        fProblems = new ArrayList<ProblemPair>();
    }

    /**
     * Add a proxy problem pair of objects
     */
    void addResolveProblem(URI missingObjectURI, IIdentifier parentObject) {
        fProblems.add(new ProblemPair(missingObjectURI, parentObject));
    }
    
    public IArchimateModel resolveProblemObjects() throws IOException {
        // deleteProblemObjects();
        
        return restoreProblemObjects();
    }
    
    IArchimateModel restoreProblemObjects() throws IOException {
        File localRepoFolder = GraficoUtils.getLocalRepositoryFolderForModel(fModel);
        
        for(ProblemPair problemPair : fProblems) {
            String missingFileName = problemPair.missingObjectURI.lastSegment();
            
            // Find the problem object xml files from the commit history and restore them
            try(Repository repository = Git.open(localRepoFolder).getRepository()) {
                try(RevWalk revWalk = new RevWalk(repository)) {
                    ObjectId id = repository.resolve("refs/heads/master"); //$NON-NLS-1$
                    if(id != null) {
                        revWalk.markStart(revWalk.parseCommit(id)); 
                    }
                    
                    for(RevCommit commit : revWalk ) {
                        RevTree tree = commit.getTree();

                        // now try to find a specific file
                        try(TreeWalk treeWalk = new TreeWalk(repository)) {
                            treeWalk.addTree(tree);
                            treeWalk.setRecursive(true);

                            // Iterate through all files
                            // We can't use a PathFilter for the file name as its path is not correct
                            while(treeWalk.next()) {
                                // File is found
                                if(treeWalk.getPathString().endsWith(missingFileName)) {
                                    // Save file
                                    ObjectId objectId = treeWalk.getObjectId(0);
                                    ObjectLoader loader = repository.open(objectId);

                                    File file = new File(localRepoFolder, treeWalk.getPathString());
                                    file.getParentFile().mkdirs();
                                    
                                    try(FileOutputStream out = new FileOutputStream(file)) {
                                        loader.copyTo(out);
                                    }
                                    
                                    break;
                                }
                            }
                        }
                    }

                    revWalk.dispose();
                }
            }
        }
        
        // Then re-import
        GraficoModelImporter importer = new GraficoModelImporter(localRepoFolder);
        return importer.importAsModel();
    }
    
    void deleteProblemObjects() throws IOException {
        for(ProblemPair problemPair : fProblems) {
            String parentID = problemPair.parentObject.getId();
            
            EObject eObject = ArchimateModelUtils.getObjectByID(fModel, parentID);
            if(eObject != null) {
                EcoreUtil.remove(eObject);
            }
        }
        
        // And re-export to grafico xml files
        GraficoModelExporter exporter = new GraficoModelExporter(fModel, GraficoUtils.getLocalRepositoryFolderForModel(fModel));
        exporter.exportModel();
    }
    
    public boolean hasProblems() {
        return !fProblems.isEmpty();
    }
    
    /**
     * @return A list of error messages
     */
    public List<String> getErrorMessages() {
        List<String> messages = new ArrayList<String>();
        
        for(ProblemPair problemPair : fProblems) {
            String message = NLS.bind(Messages.ResolutionHandler_0,
                    new Object[] { problemPair.missingObjectURI.fragment(), problemPair.parentObject.getClass().getSimpleName(),
                            problemPair.parentObject.getId() });
            
            messages.add(message);
        }
        
        return messages;
    }

    public MultiStatus getResolveStatus() {
        MultiStatus ms = new MultiStatus(ModelRepositoryPlugin.PLUGIN_ID, IStatus.ERROR, Messages.GraficoResolutionHandler_0, null);
        
        for(String message : getErrorMessages()) {
            ms.add(new Status(IStatus.ERROR, ModelRepositoryPlugin.PLUGIN_ID, message));
        }
        
        return ms;
    }
}
