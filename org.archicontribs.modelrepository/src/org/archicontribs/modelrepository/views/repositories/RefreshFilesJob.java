/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.repositories;

import java.io.File;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;

/**
 * Refresh Files in Background Job
 * 
 * @author Phillip Beauvoir
 */
class RefreshFilesJob extends Job {
    
    private static RefreshFilesJob instance = new RefreshFilesJob();
    static RefreshFilesJob getInstance() {
        return instance;
    }

    private ModelRepositoryTreeViewer fViewer;
    private long lastModified = 0L; // folder last modified timestamp
    
    private final static int DELAY = 5000; // Every 5 seconds

    /**
     * Preference changed to fetch in background
     */
    private IPropertyChangeListener preferenceChangeListener = event -> {
        if(IPreferenceConstants.PREFS_SCAN_REPOSITORY_FOLDER == event.getProperty()) {
            if(event.getNewValue() == Boolean.TRUE) {
                start();
            }
            else {
                cancel();
            }
        }
    };

    private RefreshFilesJob() {
        super("Refresh File System Job"); //$NON-NLS-1$
    }

    void init(ModelRepositoryTreeViewer viewer) {
        fViewer = viewer;
        
        // On Tree dispose...
        fViewer.getControl().addDisposeListener(event -> {
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().removePropertyChangeListener(preferenceChangeListener);
            cancel();
        });
        
        ModelRepositoryPlugin.INSTANCE.getPreferenceStore().addPropertyChangeListener(preferenceChangeListener);

        start();
    }

    private void start() {
        if(canRun()) {
            schedule(DELAY);
        }
    }
    
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        // If rootFolder has been modifed (child folder added/deleted/renamed) refresh
        File rootFolder = fViewer.getRootFolder();
        
        if(lastModified != 0L && rootFolder.lastModified() != lastModified) {
            fViewer.refreshInBackground();
        }

        lastModified = rootFolder.lastModified();

        if(canRun()) {
            schedule(DELAY);
        }
        
        return Status.OK_STATUS;
    }

    private boolean canRun() {
        return !fViewer.getControl().isDisposed() &&
                ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_SCAN_REPOSITORY_FOLDER);
    }
}
