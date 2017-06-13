/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.lib.EmptyProgressMonitor;

/**
 * Wrapper class to handle a progress monitor
 * 
 * @author Phillip Beauvoir
 */
public abstract class ProgressHandler extends EmptyProgressMonitor implements IRunnableWithProgress {
    protected IProgressMonitor monitor;

    @Override
    public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        this.monitor = monitor;
    }
    
    @Override
    public void beginTask(String title, int totalWork) {
        if(monitor != null) {
            monitor.subTask(title);
        }
    }

    @Override
    public boolean isCancelled() {
        return (monitor != null) ? monitor.isCanceled() : false;
    }

}