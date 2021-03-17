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
//
//	@Override
//	public void start(int totalTasks) {
//		//empty
//	}
//
//	/** {@inheritDoc} */
//	@Override
//	public void beginTask(String title, int totalWork) {
//		pm.beginTask(title, totalWork);
//	}
//
//	/** {@inheritDoc} */
//	@Override
//	public void update(int completed) {
//		pm.worked(completed);
//	}
//
//	/** {@inheritDoc} */
//	@Override
//	public void endTask() {
//		pm.done();
//	}
    
    @Override
    public boolean isCancelled() {
        return (pm != null) ? pm.isCanceled() : false;
    }
}