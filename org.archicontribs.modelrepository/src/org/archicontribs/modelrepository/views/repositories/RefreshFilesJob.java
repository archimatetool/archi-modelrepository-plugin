/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.repositories;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;

/**
 * Refresh Files in Background Job
 * 
 * @author Phillip Beauvoir
 */
public class RefreshFilesJob extends Job {
    
    private ModelRepositoryTreeViewer fViewer;
    
    private long lastModified = 0L; // last modified

    public RefreshFilesJob(ModelRepositoryTreeViewer viewer) {
        super("Refresh File System Job"); //$NON-NLS-1$
        fViewer = viewer;
        
        fViewer.getControl().addDisposeListener(new DisposeListener() {
            public void widgetDisposed(DisposeEvent e) {
                cancel();
            }
        });
        
        start();
    }

    protected void start() {
        if(canRun()) {
            schedule(5000);
        }
    }
    
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        // Check first thing on entry
        if(!canRun()) {
            return Status.OK_STATUS;
        }
        
        // If rootFolder has been modifed (child folder added/deleted/renamed) refresh
        File rootFolder = fViewer.getRootFolder();
        
        if(lastModified != 0L && rootFolder.lastModified() != lastModified) {
            fViewer.refreshInBackground();
        }

        lastModified = rootFolder.lastModified();

        if(canRun()) {
            schedule(5000);// Schedule again in 5 seconds
        }
        
        return Status.OK_STATUS;
    }

    protected boolean canRun() {
        return !fViewer.getControl().isDisposed();
    }

}
