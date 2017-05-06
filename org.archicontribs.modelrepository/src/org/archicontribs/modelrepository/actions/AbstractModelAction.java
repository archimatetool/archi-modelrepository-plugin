/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;

import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.action.Action;

public abstract class AbstractModelAction extends Action {
	
	private File fGitRepoFolder;
	 
	public void setGitRepository(File folder) {
        fGitRepoFolder = folder;
        setEnabled(GraficoUtils.isGitRepository(folder));
	}
	
	public File getGitRepository() {
	    return fGitRepoFolder;
	}
}
