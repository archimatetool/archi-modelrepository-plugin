/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.dialogs.CommitDialog;
import org.archicontribs.modelrepository.dialogs.UserNamePasswordDialog;
import org.archicontribs.modelrepository.grafico.GraficoModelExporter;
import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.diagram.DiagramEditorInput;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.ui.services.EditorManager;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Abstract ModelAction
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractModelAction extends Action implements IGraficoModelAction {
	
	private IArchiRepository fRepository;
	
	protected IWorkbenchWindow fWindow;
	
	protected AbstractModelAction(IWorkbenchWindow window) {
	    fWindow = window;
	}
	
	@Override
	public void setRepository(IArchiRepository repository) {
	    fRepository = repository;
	    setEnabled(shouldBeEnabled());
	}
	
	@Override
	public IArchiRepository getRepository() {
	    return fRepository;
	}
	 
	/**
	 * @return true if this action should be enabled
	 */
	protected boolean shouldBeEnabled() {
	    return getRepository() != null;
	}
	
    /**
     * Display an errror dialog
     * @param title
     * @param ex
     */
    protected void displayErrorDialog(String title, Throwable ex) {
        ex.printStackTrace();
        
        MessageDialog.openError(fWindow.getShell(),
                title,
                Messages.AbstractModelAction_0 +
                    " " + //$NON-NLS-1$
                    ex.getMessage());
    }

    /**
     * Offer to save the model
     * @param model
     */
    protected boolean offerToSaveModel(IArchimateModel model) {
        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                Messages.AbstractModelAction_1,
                Messages.AbstractModelAction_2);

        if(response) {
            try {
                IEditorModelManager.INSTANCE.saveModel(model);
            }
            catch(IOException ex) {
                displayErrorDialog(Messages.AbstractModelAction_1, ex);
            }
        }
        
        return response;
    }
    
    /**
     * Load the model from the Grafico XML files
     * @return the model or null if there are no Grafico files
     * @throws IOException
     */
    protected IArchimateModel loadModelFromGraficoFiles() throws IOException {
        GraficoModelImporter importer = new GraficoModelImporter(getRepository().getLocalRepositoryFolder());
        IArchimateModel graficoModel = importer.importAsModel();
        
        if(graficoModel != null) {
            // ids of open diagrams
            List<String> openModelIDs = null;
            
            // Close the real model if it is already open
            IArchimateModel model = fRepository.locateModel();
            if(model != null) {
                openModelIDs = getOpenDiagramModelIdentifiers(model); // Store ids of open diagrams
                IEditorModelManager.INSTANCE.closeModel(model);
            }
            
            // Set file name
            File tmpFile = fRepository.getTempModelFile();
            graficoModel.setFile(tmpFile);
            
            // Import problems occured
            // Show errors for now
            if(importer.hasProblems()) {
                // TODO - remove this when problems are resolved
                ErrorDialog.openError(fWindow.getShell(),
                        Messages.AbstractModelAction_3,
                        Messages.AbstractModelAction_4,
                        importer.getResolveStatus());
                
                // TODO - Delete/Add problem objects
                // importer.deleteProblemObjects();
                
                // And re-export to grafico xml files
                GraficoModelExporter exporter = new GraficoModelExporter(graficoModel, getRepository().getLocalRepositoryFolder());
                exporter.exportModel();
            }
            
            // Open it with the new grafico model, this will do the necessary checks and add a command stack and an archive manager
            IEditorModelManager.INSTANCE.openModel(graficoModel);
            
            // And Save it to the temp file
            IEditorModelManager.INSTANCE.saveModel(graficoModel);
            
            // Re-open editors, if any
            reopenEditors(graficoModel, openModelIDs);
        }
        
        return graficoModel;
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
    
    /**
     * Export the model to Grafico files
     */
    protected void exportModelToGraficoFiles() {
        // Open the model
        IArchimateModel model = IEditorModelManager.INSTANCE.openModel(fRepository.getTempModelFile());
        
        if(model == null) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.AbstractModelAction_7,
                    Messages.AbstractModelAction_8);
            return;
        }
        
        try {
            GraficoModelExporter exporter = new GraficoModelExporter(model, getRepository().getLocalRepositoryFolder());
            exporter.exportModel();
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.AbstractModelAction_5, ex);
        }
    }
    
    /**
     * Offer to Commit changes
     * @return true if successful, false otherwise
     */
    protected boolean offerToCommitChanges() {
        CommitDialog commitDialog = new CommitDialog(fWindow.getShell());
        int response = commitDialog.open();
        
        if(response == Window.OK) {
            String userName = commitDialog.getUserName();
            String userEmail = commitDialog.getUserEmail();
            String commitMessage = commitDialog.getCommitMessage();
            
            // Store Prefs
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().setValue(IPreferenceConstants.PREFS_COMMIT_USER_NAME, userName);
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().setValue(IPreferenceConstants.PREFS_COMMIT_USER_EMAIL, userEmail);

            try {
                getRepository().commitChanges(commitMessage);
            }
            catch(IOException | GitAPIException ex) {
                displayErrorDialog(Messages.AbstractModelAction_6, ex);
                return false;
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Get user name and password from credentials file if prefs set or from dialog
     * @param storageFileName
     * @param shell
     * @return the username and password, or null
     */
    protected UsernamePassword getUserNameAndPasswordFromCredentialsFileOrDialog(Shell shell) {
        boolean doStoreInCredentialsFile = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_STORE_REPO_CREDENTIALS);
        
        SimpleCredentialsStorage scs = new SimpleCredentialsStorage(new File(getRepository().getLocalGitFolder(), IGraficoConstants.REPO_CREDENTIALS_FILE));

        // Is it stored?
        if(doStoreInCredentialsFile && scs.hasCredentialsFile()) {
            try {
                return new UsernamePassword(scs.getUsername(), scs.getPassword());
            }
            catch(IOException ex) {
                displayErrorDialog(Messages.AbstractModelAction_9, ex);
            }
        }
        
        // Else ask the user
        UserNamePasswordDialog dialog = new UserNamePasswordDialog(shell, scs);
        if(dialog.open() == Window.OK) {
            return new UsernamePassword(dialog.getUsername(), dialog.getPassword());
        }

        return null;
    }
    
    /**
     * Notify that the repo changed
     */
    protected void notifyChangeListeners(String eventName) {
        RepositoryListenerManager.INSTANCE.fireRepositoryChangedEvent(eventName, getRepository());
    }
    
    @Override
    public void dispose() {
    }
}
