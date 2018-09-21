/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.lib.EmptyProgressMonitor;

/**
 * JGit ProgressMonitor Wrapper around a IProgressMonitor
 * 
 * @author Phillip Beauvoir
 */
public class ProgressMonitorWrapper extends EmptyProgressMonitor {
    private IProgressMonitor pm;

    public ProgressMonitorWrapper(IProgressMonitor pm) {
        this.pm = pm;
    }
    
    @Override
    public boolean isCancelled() {
        return (pm != null) ? pm.isCanceled() : false;
    }
}