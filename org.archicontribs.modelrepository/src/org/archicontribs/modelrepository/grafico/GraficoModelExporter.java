/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobGroup;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModelImageProvider;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IFolderContainer;
import com.archimatetool.model.IIdentifier;


/**
 * Based on the GRAFICO Model Exporter
 * GRAFICO (Git fRiendly Archi FIle COllection) is a way to persist an ArchiMate
 * model in a bunch of XML files (one file per ArchiMate element or view).
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Quentin Varquet
 * @author Phillip Beauvoir
 */
public class GraficoModelExporter {
	
    // Use a ProgressMonitor to cancel running Jobs and track Exception
    private static class ExceptionProgressMonitor extends NullProgressMonitor {
        IOException ex;
        
        void catchException(IOException ex) {
            this.ex = ex;
            setCanceled(true); // Cancel running job on exception
        }
    }
    
	/**
	 * ResourceSet
	 */
	private ResourceSet fResourceSet;
	
    /**
     * Model
     */
    private IArchimateModel fModel;
    
    /**
     * Local repo folder
     */
    private File fLocalRepoFolder;
    
	/**
	 * @param model The model to export
	 * @param folder The root folder in which to write the grafico XML files
	 */
	public GraficoModelExporter(IArchimateModel model, File folder) {
	    if(model == null) {
            throw new IllegalArgumentException("Model cannot be null"); //$NON-NLS-1$
        }
	    if(folder == null) {
            throw new IllegalArgumentException("Folder cannot be null"); //$NON-NLS-1$
        }
	    
	    fModel = model;
	    fLocalRepoFolder = folder;
	}


	public void exportChangedModel() {
		File modelFolder = new File(fLocalRepoFolder, IGraficoConstants.MODEL_FOLDER);
		
		
		// additive
		
		
		
		// subtractive
		
	}
	
    /**
     * Export the IArchimateModel as Grafico files
     * @throws IOException
     */
    public void exportModel() throws IOException {
        // Define target folders for model and images
        // Delete them and re-create them (remark: FileUtils.deleteFolder() does sanity checks)
        File modelFolder = new File(fLocalRepoFolder, IGraficoConstants.MODEL_FOLDER);
        FileUtils.deleteFolder(modelFolder);
        modelFolder.mkdirs();

        File imagesFolder = new File(fLocalRepoFolder, IGraficoConstants.IMAGES_FOLDER);
        FileUtils.deleteFolder(imagesFolder);
        imagesFolder.mkdirs();

        // Save model images (if any): this has to be done on original model (not a copy)
        saveImages();
        
        // Create ResourceSet
        fResourceSet = new ResourceSetImpl();
        fResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMLResourceFactoryImpl()); //$NON-NLS-1$
        // Add a URIConverter that will be used to map full filenames to logical names
        fResourceSet.setURIConverter(new ExtensibleURIConverterImpl());
        
        // Now work on a copy
        IArchimateModel copy = EcoreUtil.copy(fModel);
        
        // Create directory structure and prepare all Resources
        createAndSaveResourceForFolder(copy, modelFolder);

        // Now save all Resources
        int maxThreads = ModelRepositoryPlugin.getInstance().getPreferenceStore().getInt(IPreferenceConstants.PREFS_EXPORT_MAX_THREADS);
        JobGroup jobgroup = new JobGroup("GraficoModelExporter", maxThreads, 1); //$NON-NLS-1$
        
        final ExceptionProgressMonitor pm = new ExceptionProgressMonitor();
        
        for(Resource resource : fResourceSet.getResources()) {
            Job job = new Job("Resource Save Job") { //$NON-NLS-1$
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    try {
                        resource.save(null);
                    }
                    catch(IOException ex) {
                        pm.catchException(ex);
                    }
                    return Status.OK_STATUS;
                }
            };
            
            job.setJobGroup(jobgroup);
            job.schedule();
        }
        
        try {
            jobgroup.join(0, pm);
        }
        catch(OperationCanceledException | InterruptedException ex) {
            ex.printStackTrace();
        }
        
        // Throw on any exception
        if(pm.ex != null) {
            throw pm.ex;
        }
    }
    
    /**
     * For each folder inside model, create a directory and a Resource to save it.
     * For each element, create a Resource to save it
     * 
     * @param folderContainer Model or folder to work on 
     * @param folder Directory in which to generate files
     * @throws IOException
     */
    private void createAndSaveResourceForFolder(IFolderContainer folderContainer, File folder) throws IOException {
        // Save each children folders
        List<IFolder> allFolders = new ArrayList<IFolder>();
        allFolders.addAll(folderContainer.getFolders());
        
        for(IFolder tmpFolder : allFolders) {
            File tmpFolderFile = new File(folder, getNameFor(tmpFolder));
            tmpFolderFile.mkdirs();
            createAndSaveResource(new File(tmpFolderFile, IGraficoConstants.FOLDER_XML), tmpFolder);
            createAndSaveResourceForFolder(tmpFolder, tmpFolderFile);
        }
        
        // Save each children elements
        if(folderContainer instanceof IFolder) {
            // Save each children element
            List<EObject> allElements = new ArrayList<EObject>();
            allElements.addAll(((IFolder)folderContainer).getElements());
            for(EObject tmpElement : allElements) {
                createAndSaveResource(
                        new File(folder, tmpElement.getClass().getSimpleName() + "_" + ((IIdentifier)tmpElement).getId() + ".xml"), //$NON-NLS-1$ //$NON-NLS-2$
                        tmpElement);
            }
        }
        if(folderContainer instanceof IArchimateModel) {
            createAndSaveResource(new File(folder, IGraficoConstants.FOLDER_XML), folderContainer);
        }
    }
    
    /**
     * Generate a proper name for directory creation
     *  
     * @param folder
     * @return
     */
    private String getNameFor(IFolder folder) {
    	return folder.getType() == FolderType.USER ? folder.getId().toString() : folder.getType().toString();
    }
    
    /**
     * Save the model to Resource
     * 
     * @param file
     * @param object
     * @throws IOException
     */
    private void createAndSaveResource(File file, EObject object) throws IOException {
    	// Update the URIConverter
        // Map the logical name (filename) to the physical name (path+filename)
    	// Folders must be declared with absolute path or else the 'folder.xml' file is not created
    	// The model object must be declared with relative path or else concepts reference profiles through absolute path (which are gonna be different for each users)
        URI key = (!(object instanceof IArchimateModel) && file.getName().equals(IGraficoConstants.FOLDER_XML)) ? URI.createFileURI(file.getAbsolutePath()) : URI.createFileURI(file.getName());
        URI value = URI.createFileURI(file.getAbsolutePath());
        fResourceSet.getURIConverter().getURIMap().put(key, value);

        // Create a new resource for selected file and add object to persist
        XMLResource resource = (XMLResource)fResourceSet.createResource(key);
        
        // Use UTF-8 and don't start with an XML declaration
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_ENCODING, "UTF-8"); //$NON-NLS-1$
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_DECLARE_XML, Boolean.FALSE);
        
        // Make the produced XML easy to read
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_FORMATTED, Boolean.TRUE);
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_LINE_WIDTH, Integer.valueOf(5));
        
        // Don't use encoded attribute. Needed to have proper references inside Diagrams
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_USE_ENCODED_ATTRIBUTE_STYLE, Boolean.FALSE);
        
        // Use cache
        resource.getDefaultSaveOptions().put(XMLResource.OPTION_CONFIGURATION_CACHE, Boolean.TRUE);
        
        // Use UNIX line endings to avoid EOL diffs
        resource.getDefaultSaveOptions().put(Resource.OPTION_LINE_DELIMITER, "\n"); //$NON-NLS-1$

        // Add the object to the resource
        resource.getContents().add(object);
    }
    
    /**
     * Extract and save images used inside a model as separate image files
     */
    private void saveImages() throws IOException {
        Set<String> added = new HashSet<>();

        IArchiveManager archiveManager = (IArchiveManager)fModel.getAdapter(IArchiveManager.class);
        if(archiveManager == null) {
            archiveManager = IArchiveManager.FACTORY.createArchiveManager(fModel);
        }
        
        for(Iterator<EObject> iter = fModel.eAllContents(); iter.hasNext();) {
            EObject eObject = iter.next();
            if(eObject instanceof IDiagramModelImageProvider) {
                IDiagramModelImageProvider imageProvider = (IDiagramModelImageProvider)eObject;
                String imagePath = imageProvider.getImagePath();
                
                if(imagePath != null && !added.contains(imagePath)) {
                    byte[] bytes = archiveManager.getBytesFromEntry(imagePath);
                    if(bytes == null) {
                        throw new IOException("Could not get image bytes from image path: " + imagePath); //$NON-NLS-1$
                    }
                    
                    File file = new File(fLocalRepoFolder, imagePath);
                    Files.write(file.toPath(), bytes, StandardOpenOption.CREATE);
                    added.add(imagePath);
                }
            }
        }
    }
}
