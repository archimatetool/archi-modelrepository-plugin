/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.repositories;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

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
        
        IPropertyChangeListener listener = new IPropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent event) {
                if(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND == event.getProperty()) {
                    if(event.getNewValue() == Boolean.TRUE && getState() == Job.NONE) {
                        start();
                    }
                }
            }
        };
        
        ModelRepositoryPlugin.INSTANCE.getPreferenceStore().addPropertyChangeListener(listener);
        
        fViewer.getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                ModelRepositoryPlugin.INSTANCE.getPreferenceStore().removePropertyChangeListener(listener);
            }
        });
        
        start();
    }
    
    protected void start() {
        if(canRun()) {
            schedule(1000);
        }
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

            // If the user name and password are stored
            SimpleCredentialsStorage scs = new SimpleCredentialsStorage(new File(repo.getLocalGitFolder(), IGraficoConstants.REPO_CREDENTIALS_FILE));
            try {
                String userName = scs.getUsername();
                String userPassword = scs.getPassword();
                if(userName != null && userPassword != null) {
                    repo.fetchFromRemote(userName, userPassword, null, false);
                    needsRefresh = true;
                }
            }
            catch(IOException | GitAPIException ex) {
                // silence is golden
            }
        }

        if(needsRefresh) {
            fViewer.refreshInBackground();
        }

        if(canRun()) {
            schedule(20000); // Schedule again in 20 seconds if possible
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
