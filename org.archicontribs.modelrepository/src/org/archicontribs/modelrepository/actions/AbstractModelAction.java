/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.authentication.internal.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.dialogs.CommitDialog;
import org.archicontribs.modelrepository.dialogs.ErrorMessageDialog;
import org.archicontribs.modelrepository.dialogs.UserNamePasswordDialog;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.archicontribs.modelrepository.grafico.ShellArchiRepository;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Abstract ModelAction
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractModelAction extends Action implements IGraficoModelAction {
	
	private IArchiRepository fRepository;
	
	private ShellArchiRepository shellRepository;
	
	protected IWorkbenchWindow fWindow;
	
	protected AbstractModelAction(IWorkbenchWindow window) {
	    fWindow = window;

	    this.shellRepository = new ShellArchiRepository();
	}
	
	@Override
	public void setRepository(IArchiRepository repository) {
	    fRepository = repository;
	    setEnabled(shouldBeEnabled());
	    getShellRepository().setLocalRepoFolder(repository.getLocalRepositoryFolder());
	}
	
	@Override
	public IArchiRepository getRepository() {
	    return fRepository;
	}

	public boolean isShellModeAvailable() {
		return this.shellRepository != null;
	}
	
	public void setShellRepository(ShellArchiRepository shellRepository) {
		this.shellRepository = shellRepository;
	}

	public ShellArchiRepository getShellRepository() {
		return shellRepository;
	}

	@Override
	public void update() {
        setEnabled(shouldBeEnabled());
	}
	 
	/**
	 * @return true if this action should be enabled
	 */
	protected boolean shouldBeEnabled() {
	    return getRepository() != null && getRepository().getLocalRepositoryFolder().exists();
	}
	
    /**
     * Display an errror dialog
     */
    protected void displayErrorDialog(String title, Throwable ex) {
        ex.printStackTrace();
        ErrorMessageDialog.open(fWindow.getShell(), title, Messages.AbstractModelAction_0, ex);
    }

    /**
     * Display an errror dialog
     */
    protected void displayErrorDialog(String title, String message) {
        MessageDialog.openError(fWindow.getShell(),
                title,
                Messages.AbstractModelAction_0 +
                    "\n" + //$NON-NLS-1$
                    message);
    }
    
    protected void displayCredentialsErrorDialog(Throwable ex) {
        ex.printStackTrace();
        displayErrorDialog(Messages.AbstractModelAction_5, Messages.AbstractModelAction_11);
    }

    /**
     * Offer to save the model
     * @param model
     */
    protected boolean offerToSaveModel(IArchimateModel model) {
        boolean doSave = MessageDialog.openConfirm(fWindow.getShell(),
                Messages.AbstractModelAction_1,
                Messages.AbstractModelAction_2);

        if(doSave) {
            try {
                doSave = IEditorModelManager.INSTANCE.saveModel(model);
            }
            catch(IOException ex) {
                doSave = false;
                displayErrorDialog(Messages.AbstractModelAction_1, ex);
            }
        }
        
        return doSave;
    }
    
    /**
     * Offer to Commit changes
     * @return true if successful, false otherwise
     */
    protected boolean offerToCommitChanges() {
        CommitDialog commitDialog = new CommitDialog(fWindow.getShell(), getRepository());
        int response = commitDialog.open();
        
        if(response == Window.OK) {
            String commitMessage = commitDialog.getCommitMessage();
            boolean amend = commitDialog.getAmend();
            
            try {
                if (isShellModeAvailable()) {
                	getShellRepository().commit(commitMessage, amend);
                } else {
	            	getRepository().commitChanges(commitMessage, amend);
	
                }
                // Save the checksum
                getRepository().saveChecksum();
            }
            catch(Exception ex) {
                displayErrorDialog(Messages.AbstractModelAction_6, ex);
                return false;
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Get user name and password from credentials file if prefs set or from dialog
     */
    protected UsernamePassword getUsernamePassword() throws IOException, GeneralSecurityException {
        // SSH
        if(GraficoUtils.isSSH(getRepository().getOnlineRepositoryURL())) {
            return null;
        }
        
        boolean doStoreInCredentialsFile = ModelRepositoryPlugin.getInstance().getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_STORE_REPO_CREDENTIALS);
        
        EncryptedCredentialsStorage cs = EncryptedCredentialsStorage.forRepository(getRepository());

        // Is it stored?
        if(doStoreInCredentialsFile && cs.hasCredentialsFile()) {
            return cs.getUsernamePassword();
        }
        
        // Else ask the user
        UserNamePasswordDialog dialog = new UserNamePasswordDialog(fWindow.getShell(), cs);
        if(dialog.open() == Window.OK) {
            return new UsernamePassword(dialog.getUsername(), dialog.getPassword());
        }

        return null;
    }
    
    /**
     * Save checksum and then notify listeners
     */
    protected void saveChecksumAndNotifyListeners() throws IOException {
        getRepository().saveChecksum(); // This first
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
        notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
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
