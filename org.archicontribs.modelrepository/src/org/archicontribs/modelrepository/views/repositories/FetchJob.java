/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.repositories;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticator;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

/**
 * Fetch in Background Job
 * 
 * @author Phillip Beauvoir
 */
public class FetchJob extends Job {
    
    private ModelRepositoryTreeViewer fViewer;

    public FetchJob(ModelRepositoryTreeViewer viewer) {
        super("Fetch Job"); //$NON-NLS-1$
        fViewer = viewer;
        
        // Don't start until we have primary password access
        // So disable background fetch until then
        disablePreference();

        // Preference changed to fetch in background
        IPropertyChangeListener listener = event -> {
            if(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND == event.getProperty()) {
                if(event.getNewValue() == Boolean.TRUE && getState() == Job.NONE) {
                    start();
                }
            }
        };
        
        // Listen to preferences
        ModelRepositoryPlugin.INSTANCE.getPreferenceStore().addPropertyChangeListener(listener);
        
        // Unlisten to preferences
        fViewer.getControl().addDisposeListener(event -> {
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().removePropertyChangeListener(listener);
        });
    }
    
    private void start() {
        // Password primary key not set
        try {
            if(!EncryptedCredentialsStorage.checkPrimaryKeySet()) {
                disablePreference();
                return;
            }
        }
        catch(GeneralSecurityException | IOException ex) {
            ex.printStackTrace();
            disablePreference();
            MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.FetchJob_2, Messages.FetchJob_3);
            return;
        }
        
        if(canRun()) {
            schedule(1000);
        }
    }
    
    private void disablePreference() {
        ModelRepositoryPlugin.INSTANCE.getPreferenceStore().setValue(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND, false);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        // Check first thing on entry
        if(!canRun()) {
            return Status.OK_STATUS;
        }
        
        boolean needsRefresh = false;
        
        for(IArchiRepository repo : fViewer.getRepositories(fViewer.getRootFolder())) {
            // Check also in for loop
            if(!canRun()) {
                return Status.OK_STATUS;
            }
            
            try {
                UsernamePassword npw = null;
                String url = repo.getOnlineRepositoryURL();
                
                if(GraficoUtils.isHTTP(url)) {
                    // Get credentials. In some public repos we can still fetch without needing a password so we try anyway
                    EncryptedCredentialsStorage cs = EncryptedCredentialsStorage.forRepository(repo);
                    npw = cs.getUsernamePassword();
                }

                // Update ProxyAuthenticator
                ProxyAuthenticator.update();

                // Fetch
                FetchResult fetchResult = repo.fetchFromRemote(npw, null, false);

                // We got here, so the tree can be refreshed later
                needsRefresh = true;
                
                // Remote branches might have been deleted or added
                if(!fetchResult.getTrackingRefUpdates().isEmpty() && !fViewer.getControl().isDisposed()) {
                    fViewer.getControl().getDisplay().asyncExec(() -> {
                        RepositoryListenerManager.INSTANCE.fireRepositoryChangedEvent(IRepositoryListener.BRANCHES_CHANGED, repo);
                    });
                }
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
                
                if(ex instanceof TransportException) {
                    if(PlatformUI.isWorkbenchRunning()) {
                        // Disable background fetch
                        disablePreference();
    
                        // Show message
                        Display.getDefault().syncExec(() -> {
                            String message = Messages.FetchJob_0 + " "; //$NON-NLS-1$
                            message += Messages.FetchJob_1 + "\n\n"; //$NON-NLS-1$
                            try {
                                message += repo.getName() + "\n"; //$NON-NLS-1$
                                message += repo.getOnlineRepositoryURL() + "\n"; //$NON-NLS-1$
                            }
                            catch(IOException ex1) {
                                ex1.printStackTrace();
                            }
                            MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.FetchJob_2, message);
                        });
                    }

                    return Status.OK_STATUS;
                }
            }
            // Encrypted password key error
            catch(GeneralSecurityException ex) {
                ex.printStackTrace();
                
                if(PlatformUI.isWorkbenchRunning()) {
                    // Disable background fetch
                    disablePreference();
                    
                    // Show message
                    Display.getDefault().syncExec(() -> {
                        String message = Messages.FetchJob_0 + "\n"; //$NON-NLS-1$
                        MessageDialog.openError(Display.getCurrent().getActiveShell(), Messages.FetchJob_2, message + ex.getMessage());
                    });
                }

                return Status.OK_STATUS;
            }
            finally {
                // Clear ProxyAuthenticator
                ProxyAuthenticator.clear();
            }
        }

        if(needsRefresh) {
            fViewer.refreshInBackground();
        }

        if(canRun()) {
            int seconds = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND_INTERVAL);
            schedule(seconds * 1000); // Schedule again in x milliseconds if possible
        }
        
        return Status.OK_STATUS;
    }
    
    /*
     * Because the Git Fetch process doesn't respond to cancel requests we can't cancel it when it is running.
     * So, if the user closes the app this job might be running. So we will wait for this job to finish
     */
    @Override
    protected void canceling() {
        int timeout = 0;
        final int delay = 100;
        
        try {
            while(getState() == Job.RUNNING) {
                Thread.sleep(delay);
                timeout += delay;
                if(timeout > 30000) { // don't wait longer than this
                    break;
                }
            }
        }
        catch(InterruptedException ex) {
            ex.printStackTrace();
        }
    }
    
    protected boolean canRun() {
        return !fViewer.getControl().isDisposed() &&
                ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND);
    }
    
}
