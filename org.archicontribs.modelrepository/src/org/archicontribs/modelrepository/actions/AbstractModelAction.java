package org.archicontribs.modelrepository.actions;

import java.io.File;

import org.eclipse.jface.action.Action;

public abstract class AbstractModelAction extends Action {
	
	protected File fGitRepo;
	 
	public void setGitRepository(File folder) {
		setEnabled(folder != null && (new File(folder, ".git")).exists());
        fGitRepo = isEnabled() ? folder : null;
	}
	
}
