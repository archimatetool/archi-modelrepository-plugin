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
import java.util.Iterator;
import java.util.List;

import org.archicontribs.modelrepository.grafico.GraficoModelImporter.UnresolvedObject;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.diagram.DiagramEditorInput;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.ui.services.EditorManager;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.INameable;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Import a model from Grafico files and handle conflicts, re-opening diagrams and status
 * 
 * @author Phillip Beauvoir
 */
public class GraficoModelLoader {
    
    private IArchiRepository fRepository;
    
    private List<IIdentifier> fRestoredObjects;
    
    public GraficoModelLoader(IArchiRepository repository) {
        fRepository = repository;
    }
    
    /**
     * Load the model
     * @return
     * @throws IOException
     */
    public IArchimateModel loadModel() throws IOException {
        fRestoredObjects = null;
        
        // Store ids of open diagrams
        List<String> openModelIDs = null;
        
        // Close the real model if it is already open - do this first!
        IArchimateModel model = fRepository.locateModel();
        if(model != null) {
            openModelIDs = getOpenDiagramModelIdentifiers(model); // Store ids of open diagrams
            IEditorModelManager.INSTANCE.closeModel(model);
            while(Display.getCurrent().readAndDispatch()); // Stops flickering in tree
        }
        
        // Import Grafico Model
        GraficoModelImporter importer = new GraficoModelImporter(fRepository.getLocalRepositoryFolder());
        
        IArchimateModel[] graficoModel = new IArchimateModel[1];
        IOException[] exception = new IOException[1];
        
        BusyIndicator.showWhile(Display.getCurrent(), () -> {
            try {
                graficoModel[0] = importer.importAsModel();
            }
            catch(IOException ex) {
                exception[0] = ex;
            }
        });
        
        if(exception[0] != null) {
            throw exception[0];
        }
        
        if(graficoModel[0] == null) {
            return null;
        }
        
        // Set file name on the grafico model so we can locate it
        graficoModel[0].setFile(fRepository.getTempModelFile());
        
        // Resolve missing objects
        List<UnresolvedObject> unresolvedObjects = importer.getUnresolvedObjects();
        if(unresolvedObjects != null) {
            graficoModel[0] = restoreProblemObjects(unresolvedObjects);
        }
        
        // Save it
        IEditorModelManager.INSTANCE.saveModel(graficoModel[0]);
        
        // And re-open it if we already had it open
        if(model != null) {
            IEditorModelManager.INSTANCE.openModel(graficoModel[0]);
            reopenEditors(graficoModel[0], openModelIDs);
        }
        
        return graficoModel[0];
    }
    
    /**
     * @return The list of resolved objects as a message string or null
     */
    public String getRestoredObjectsAsString() {
        if(fRestoredObjects == null) {
            return null;
        }
        
        String s = Messages.GraficoModelLoader_0;
        
        for(IIdentifier id : fRestoredObjects) {
            if(id instanceof INameable) {
                String name = ((INameable)id).getName();
                String className = id.eClass().getName();
                s += "\n" + (StringUtils.isSet(name) ? name + " (" + className + ")" : className); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }
        }
        
        return s;
    }
    
    /**
     * Find the problem object xml files from the commit history and restore them
     * @param unresolvedObjects 
     * @return
     * @throws IOException
     */
    private IArchimateModel restoreProblemObjects(List<UnresolvedObject> unresolvedObjects) throws IOException {
        fRestoredObjects = new ArrayList<IIdentifier>();
        
        List<String> restoredIdentifiers = new ArrayList<String>();
        
        try(Repository repository = Git.open(fRepository.getLocalRepositoryFolder()).getRepository()) {
            try(RevWalk revWalk = new RevWalk(repository)) {
                for(UnresolvedObject unresolved : unresolvedObjects) {
                    String missingFileName = unresolved.missingObjectURI.lastSegment();
                    String missingObjectID = unresolved.missingObjectURI.fragment();
                    
                    // Already got this one
                    if(restoredIdentifiers.contains(missingObjectID)) {
                        continue;
                    }
                    
                    boolean found = false;
                    
                    // Reset RevWalk
                    revWalk.reset();
                    ObjectId id = repository.resolve(IGraficoConstants.HEAD);
                    if(id != null) {
                        revWalk.markStart(revWalk.parseCommit(id)); 
                    }
                    
                    // Iterate all commits
                    for(RevCommit commit : revWalk ) {
                        try(TreeWalk treeWalk = new TreeWalk(repository)) {
                            treeWalk.addTree(commit.getTree());
                            treeWalk.setRecursive(true);
                            
                            // Iterate through all files
                            // We can't use a PathFilter for the file name as its path is not correct
                            while(!found && treeWalk.next()) {
                                // File is found
                                if(treeWalk.getPathString().endsWith(missingFileName)) {
                                    // Save file
                                    ObjectId objectId = treeWalk.getObjectId(0);
                                    ObjectLoader loader = repository.open(objectId);

                                    File file = new File(fRepository.getLocalRepositoryFolder(), treeWalk.getPathString());
                                    file.getParentFile().mkdirs();
                                    
                                    try(FileOutputStream out = new FileOutputStream(file)) {
                                        loader.copyTo(out);
                                    }
                                    
                                    restoredIdentifiers.add(missingObjectID);
                                    found = true;
                                }
                            }
                        }
                        
                        if(found) {
                            break;
                        }
                    }
                }
                
                revWalk.dispose();
            }
        }
        
        // Then re-import
        GraficoModelImporter importer = new GraficoModelImporter(fRepository.getLocalRepositoryFolder());
        IArchimateModel graficoModel = importer.importAsModel();
        graficoModel.setFile(fRepository.getTempModelFile()); // do this again
        
        // Collect restored objects
        for(Iterator<EObject> iter = graficoModel.eAllContents(); iter.hasNext();) {
            EObject element = iter.next();
            for(String id : restoredIdentifiers) {
                if(element instanceof IIdentifier && id.equals(((IIdentifier)element).getId())) {
                    fRestoredObjects.add((IIdentifier)element);
                }
            }
        }
        
        return graficoModel;
    }

    @SuppressWarnings("unused")
    private void deleteProblemObjects(List<UnresolvedObject> unresolvedObjects, IArchimateModel model) throws IOException {
        for(UnresolvedObject unresolved : unresolvedObjects) {
            String parentID = unresolved.parentObject.getId();
            
            EObject eObject = ArchimateModelUtils.getObjectByID(model, parentID);
            if(eObject != null) {
                EcoreUtil.remove(eObject);
            }
        }
        
        // And re-export to grafico xml files
        GraficoModelExporter exporter = new GraficoModelExporter(model, fRepository.getLocalRepositoryFolder());
        exporter.exportModel();
    }

    /**
     * @param model
     * @return All open diagram models' ids so we can restore them
     */
    private List<String> getOpenDiagramModelIdentifiers(IArchimateModel model) {
        List<String> list = new ArrayList<String>();
        
        for(IEditorReference ref : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
            try {
                IEditorInput input = ref.getEditorInput();
                if(input instanceof DiagramEditorInput) {
                    IDiagramModel dm = ((DiagramEditorInput)input).getDiagramModel();
                    if(dm.getArchimateModel() == model) {
                        list.add(dm.getId());
                    }
                }
            }
            catch(PartInitException ex) {
                ex.printStackTrace();
            }
        }
        
        return list;
    }
    
    /**
     * Re-open any diagram editors
     * @param model
     * @param ids
     */
    private void reopenEditors(IArchimateModel model, List<String> ids) {
        if(ids != null) {
            for(String id : ids) {
                EObject eObject = ArchimateModelUtils.getObjectByID(model, id);
                if(eObject instanceof IDiagramModel) {
                    EditorManager.openDiagramEditor((IDiagramModel)eObject);
                }
            }
        }
    }

}
