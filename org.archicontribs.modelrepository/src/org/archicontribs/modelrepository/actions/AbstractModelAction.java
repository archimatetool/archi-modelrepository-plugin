/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.dialogs.CommitDialog;
import org.archicontribs.modelrepository.grafico.GraficoModelExporter;
import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Abstract ModelAction
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractModelAction extends Action implements IGraficoModelAction {
	
	private File fGitRepoFolder;
	
	protected IWorkbenchWindow fWindow;
	
	protected AbstractModelAction(IWorkbenchWindow window) {
	    fWindow = window;
	}
	 
	@Override
    public void setLocalRepositoryFolder(File folder) {
        fGitRepoFolder = folder;
        setEnabled(GraficoUtils.isGitRepository(folder));
	}
	
	@Override
    public File getLocalRepositoryFolder() {
	    return fGitRepoFolder;
	}
	
	@Override
    public File getLocalGitFolder() {
	    return new File(fGitRepoFolder, ".git"); //$NON-NLS-1$
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
     * @param localRepoFolder
     * @return the model or null if there are no Grafico files
     * @throws IOException
     */
    protected IArchimateModel loadModelFromGraficoFiles(File localRepoFolder) throws IOException {
        GraficoModelImporter importer = new GraficoModelImporter();
        IArchimateModel graficoModel = importer.importLocalGitRepositoryAsModel(localRepoFolder);
        
        if(graficoModel != null) {
            File tmpFile = GraficoUtils.getModelFileName(localRepoFolder);
            graficoModel.setFile(tmpFile);
            
            // Errors
            if(importer.getResolveStatus() != null) {
                ErrorDialog.openError(fWindow.getShell(),
                        Messages.AbstractModelAction_3,
                        Messages.AbstractModelAction_4,
                        importer.getResolveStatus());

            }
            
            // Open it, this will do the necessary checks and add a command stack and an archive manager
            IEditorModelManager.INSTANCE.openModel(graficoModel);
            
            // And Save it to the temp file
            IEditorModelManager.INSTANCE.saveModel(graficoModel);
        }
        
        return graficoModel;
    }
    
    /**
     * Export the model to Grafico files
     * @param model
     * @param localRepoFolder
     */
    protected void exportModelToGraficoFiles(IArchimateModel model, File localRepoFolder) {
        try {
            GraficoModelExporter exporter = new GraficoModelExporter();
            exporter.exportModelToLocalGitRepository(model, localRepoFolder);
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
            PersonIdent personIdent = new PersonIdent(userName, userEmail);
            
            // Store Prefs
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().setValue(IPreferenceConstants.PREFS_COMMIT_USER_NAME, userName);
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().setValue(IPreferenceConstants.PREFS_COMMIT_USER_EMAIL, userEmail);

            try {
                GraficoUtils.commitChanges(getLocalRepositoryFolder(), personIdent, commitMessage);
            }
            catch(IOException | GitAPIException ex) {
                displayErrorDialog(Messages.AbstractModelAction_6, ex);
                return false;
            }
            
            return true;
        }
        
        return false;
    }
    
    @Override
    public void dispose() {
    }
}
