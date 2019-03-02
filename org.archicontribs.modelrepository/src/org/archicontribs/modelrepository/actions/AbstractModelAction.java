/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.dialogs.CommitDialog;
import org.archicontribs.modelrepository.dialogs.UserNamePasswordDialog;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
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
        
        MessageDialog.openError(fWindow.getShell(),
                title,
                Messages.AbstractModelAction_0 +
                    " " + //$NON-NLS-1$
                    message);
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
     */
    protected UsernamePassword getUsernamePassword() throws IOException {
        // SSH
        if(GraficoUtils.isSSH(getRepository().getOnlineRepositoryURL())) {
            return null;
        }
        
        SimpleCredentialsStorage scs = new SimpleCredentialsStorage(new File(getRepository().getLocalGitFolder(),
                IGraficoConstants.REPO_CREDENTIALS_FILE));

        // Is it stored?
        if(scs.hasCredentialsFile()) {
            return scs.getUsernamePassword();
        }
        
        // Else ask the user
        UserNamePasswordDialog dialog = new UserNamePasswordDialog(fWindow.getShell(), scs);
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
