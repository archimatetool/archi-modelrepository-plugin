/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelReference;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;



/**
 * Based on the GRAFICO Model Importer
 * GRAFICO (Git fRiendly Archi FIle COllection) is a way to persist an ArchiMate
 * model in a bunch of XML files (one file per ArchiMate element or view).
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Quentin Varquet
 * @author Phillip Beauvoir
 */
public class GraficoModelImporter implements IGraficoConstants {
    
	// ID -> Object lookup table
    private Map<String, IIdentifier> fIDLookup;
    
    /**
     * Resolution Handler
     */
    private GraficoResolutionHandler fResolutionHandler;
    
    /**
     * Resource Set
     */
    private ResourceSet fResourceSet;
	
    /**
     * @param gitRepoFolder
     * @return The model
     * @throws IOException
     */
    public IArchimateModel importLocalGitRepositoryAsModel(File gitRepoFolder) throws IOException {
    	if(gitRepoFolder == null) {
            throw new IOException("Folder was null"); //$NON-NLS-1$
        }
    	
    	// Create folders for model and images
    	File modelFolder = new File(gitRepoFolder, MODEL_FOLDER);
        modelFolder.mkdirs();

        File imagesFolder = new File(gitRepoFolder, IMAGES_FOLDER);
    	imagesFolder.mkdirs();
    	
    	// If the top folder.xml does not exist then there is nothing to import, so return null
    	if(!(new File(modelFolder, FOLDER_XML)).isFile()) {
    	    return null;
    	}
    	
    	// Create ResourceSet
    	fResourceSet = new ResourceSetImpl();
    	fResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMLResourceFactoryImpl()); //$NON-NLS-1$
    	
    	// Reset the ID -> Object lookup table
    	fIDLookup = new HashMap<String, IIdentifier>();
    	
        // Load the Model from files (it will contain unresolved proxies)
    	IArchimateModel model = loadModel(modelFolder);
    	
    	// Remove model from its resource (needed to save it back to a .archimate file)
    	fResourceSet.getResource(URI.createFileURI((new File(modelFolder, FOLDER_XML)).getAbsolutePath()), true).getContents().remove(model);
    	
    	// Resolve proxies
    	fResolutionHandler = null;
    	resolveProxies(model);

    	// Load images
    	loadImages(model, imagesFolder);

    	return model;
    }
    
    /**
     * @return The Resolution Handler if any, can be null.
     */
    public GraficoResolutionHandler getResolutionHandler() {
        return fResolutionHandler;
    }
    
    /**
     * Read images from images subfolder and load them into the model
     * 
     * @param model
     * @param folder
     * @throws IOException
     */
    private void loadImages(IArchimateModel model, File folder) throws IOException {
        IArchiveManager archiveManager = IArchiveManager.FACTORY.createArchiveManager(model);
        byte[] bytes;

        // Add all images files
        for(File imageFile : folder.listFiles()) {
            if(imageFile.isFile()) {
                bytes = Files.readAllBytes(imageFile.toPath());
                // /!\ This must match the prefix used in
                // ArchiveManager.createArchiveImagePathname
                archiveManager.addByteContentEntry("images/" + imageFile.getName(), bytes); //$NON-NLS-1$
            }
        }
    }    
   
    /**
     * Iterate through all model objects, and resolve proxies on known classes
     */
    private void resolveProxies(IArchimateModel model) {
        for(Iterator<EObject> iter = model.eAllContents(); iter.hasNext();) {
            EObject eObject = iter.next();

            if(eObject instanceof IArchimateRelationship) {
                // Resolve proxies for Relations
                IArchimateRelationship relation = (IArchimateRelationship)eObject;
                relation.setSource((IArchimateConcept)resolve(model, relation.getSource(), relation));
                relation.setTarget((IArchimateConcept)resolve(model, relation.getTarget(), relation));
            }
            else if(eObject instanceof IDiagramModelArchimateObject) {
                // Resolve proxies for Elements
                IDiagramModelArchimateObject element = (IDiagramModelArchimateObject)eObject;
                element.setArchimateElement((IArchimateElement)resolve(model, element.getArchimateElement(), element));
                // Update cross-references
                element.getArchimateElement().getReferencingDiagramObjects().add(element);
            }
            else if(eObject instanceof IDiagramModelArchimateConnection) {
                // Resolve proxies for Connections
                IDiagramModelArchimateConnection archiConnection = (IDiagramModelArchimateConnection)eObject;
                archiConnection.setArchimateRelationship((IArchimateRelationship)resolve(model, archiConnection.getArchimateRelationship(), archiConnection));
                // Update cross-reference
                archiConnection.getArchimateRelationship().getReferencingDiagramConnections().add(archiConnection);
            }
            else if(eObject instanceof IDiagramModelReference) {
                // Resolve proxies for Model References
                IDiagramModelReference element = (IDiagramModelReference)eObject;
                element.setReferencedModel((IDiagramModel)resolve(model, element.getReferencedModel(), element));
            }
        }
    }

    /**
     * Check if 'object' is a proxy. if yes, replace it with real object from mapping table.
     */
    private EObject resolve(IArchimateModel model, IIdentifier object, IIdentifier parent) {
        if(object != null && object.eIsProxy()) {
            String objectURIFragment = EcoreUtil.getURI(object).fragment();
            
            // Get proxy object
            IIdentifier newObject = fIDLookup.get(objectURIFragment);
            
            // If proxy has not been resolved
            if(newObject == null) {
                // Create resolution handler the first time
                if(fResolutionHandler == null) {
                    fResolutionHandler = new GraficoResolutionHandler(model);
                }
                
                // Add a resolve problem to the handler
                fResolutionHandler.addResolveProblem(object, parent);
            }
            
            return newObject == null ? object : newObject;
        }
        else {
            return object;
        }
    }
    
	private IArchimateModel loadModel(File folder) throws IOException {
		IArchimateModel model = (IArchimateModel)loadElement(new File(folder, FOLDER_XML));
		IFolder tmpFolder;
		
		List<FolderType> folderList = new ArrayList<FolderType>();
		folderList.add(FolderType.STRATEGY);
		folderList.add(FolderType.BUSINESS);
		folderList.add(FolderType.APPLICATION);
		folderList.add(FolderType.TECHNOLOGY);
		folderList.add(FolderType.MOTIVATION);
		folderList.add(FolderType.IMPLEMENTATION_MIGRATION);
		folderList.add(FolderType.OTHER);
		folderList.add(FolderType.RELATIONS);
		folderList.add(FolderType.DIAGRAMS);

		// Loop based on FolderType enumeration
		for(FolderType folderType : folderList) {
		    if((tmpFolder = loadFolder(new File(folder, folderType.toString()))) != null) {
		        model.getFolders().add(tmpFolder);
		    }
		}
		
		return model;
	}
	
	/**
	 * Load each XML file to recreate original object
	 * 
	 * @param folder
	 * @return Model folder
	 * @throws IOException 
	 */
    private IFolder loadFolder(File folder) throws IOException {
        if(!folder.isDirectory() || !(new File(folder, FOLDER_XML)).isFile()) {
            throw new IOException("File is not directory or folder.xml does not exist."); //$NON-NLS-1$
        }

        // Load folder object itself
        IFolder currentFolder = (IFolder)loadElement(new File(folder, FOLDER_XML));

        // Load each elements (except folder.xml) and add them to folder
        for(File fileOrFolder : folder.listFiles()) {
            if(!fileOrFolder.getName().equals(FOLDER_XML)) {
                if(fileOrFolder.isFile()) {
                    currentFolder.getElements().add(loadElement(fileOrFolder));
                }
                else {
                    currentFolder.getFolders().add(loadFolder(fileOrFolder));
                }
            }
        }

        return currentFolder;
    }

    /**
     * Create an eObject from an XML file. Basically load a resource.
     * 
     * @param file
     * @return
     */
    private EObject loadElement(File file) {
        // Create a new resource for selected file and add object to persist
        XMLResource resource = (XMLResource)fResourceSet.getResource(URI.createFileURI(file.getAbsolutePath()), true);
        resource.getDefaultLoadOptions().put(XMLResource.OPTION_ENCODING, "UTF-8"); //$NON-NLS-1$
        
        IIdentifier element = (IIdentifier)resource.getContents().get(0);

        // Update an ID -> Object mapping table (used as a cache to resolve proxies)
        fIDLookup.put(element.getId(), element);

        return element;
    }
}
