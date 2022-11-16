/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.authentication.internal.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.dialogs.CommitDialog;
import org.archicontribs.modelrepository.dialogs.UserNamePasswordDialog;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.JGitInternalException;
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
     * @param title
     * @param ex
     */
    protected void displayErrorDialog(String title, Throwable ex) {
        ex.printStackTrace();
        
        String message = ex.getMessage();
        
        if(ex instanceof InvocationTargetException) {
            ex = ex.getCause();
        }
        
        if(ex instanceof JGitInternalException) {
            ex = ex.getCause();
        }
        
        if(ex != null) {
            message = ex.getMessage();
        }
        
        displayErrorDialog(title, message);
    }

    /**
     * Display an errror dialog
     * @param title
     * @param message
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
                getRepository().commitChanges(commitMessage, amend);

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
     * POC get username and password from Eclipse SecurePreferences
     */
    protected UsernamePassword getUsernamePasswordFromSecurePreferences() throws IOException, StorageException {
        // SSH
        if(GraficoUtils.isSSH(getRepository().getOnlineRepositoryURL())) {
            return null;
        }

        // Get Secure Prefs root node
        ISecurePreferences rootNode = SecurePreferencesFactory.getDefault();
        // This is the coArchi node for all secure entries. We could clear it with coArchiNode.removeNode()
        ISecurePreferences coArchiNode = rootNode.node(ModelRepositoryPlugin.PLUGIN_ID);
        // This is the child node for this repository
        ISecurePreferences repoNode = coArchiNode.node(URLEncoder.encode(getRepository().getOnlineRepositoryURL(), StandardCharsets.UTF_8));
        
        // Get user name and password
        String username = repoNode.get("username", null); //$NON-NLS-1$
        String password = repoNode.get("password", null); //$NON-NLS-1$
        
        if(username != null) {
            return new UsernamePassword(username, password != null ? password.toCharArray() : "".toCharArray()); //$NON-NLS-1$
        }
        
        // Else ask the user
        UserNamePasswordDialog dialog = new UserNamePasswordDialog(fWindow.getShell(), repoNode);
        if(dialog.open() == Window.OK) {
            return new UsernamePassword(dialog.getUsername(), dialog.getPassword());
        }

        return null;
    }
    
    /**
     * Get user name and password from credentials file if prefs set or from dialog
     */
    protected UsernamePassword getUsernamePassword() throws IOException, GeneralSecurityException {
        // SSH
        if(GraficoUtils.isSSH(getRepository().getOnlineRepositoryURL())) {
            return null;
        }
        
        EncryptedCredentialsStorage cs = EncryptedCredentialsStorage.forRepository(getRepository());

        // Is it stored?
        if(cs.hasCredentialsFile()) {
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
