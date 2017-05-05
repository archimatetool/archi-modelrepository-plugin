/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;

import org.eclipse.jface.action.Action;

public abstract class AbstractModelAction extends Action {
	
	private File fGitRepo;
	 
	public void setGitRepository(File folder) {
		setEnabled(folder != null && (new File(folder, ".git")).exists()); //$NON-NLS-1$
        fGitRepo = isEnabled() ? folder : null;
	}
	
	public File getGitRepository() {
	    return fGitRepo;
	}
}
