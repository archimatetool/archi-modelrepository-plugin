/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;

import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractModelAction extends Action {
	
	private File fGitRepoFolder;
	 
	public void setGitRepository(File folder) {
        fGitRepoFolder = folder;
        setEnabled(GraficoUtils.isGitRepository(folder));
	}
	
	public File getGitRepository() {
	    return fGitRepoFolder;
	}
	
    protected void displayErrorDialog(Shell shell, String title, Throwable ex) {
        ex.printStackTrace();
        
        MessageDialog.openError(shell,
                title,
                "There was an error:" + " " +
                    ex.getMessage());
    }

}
