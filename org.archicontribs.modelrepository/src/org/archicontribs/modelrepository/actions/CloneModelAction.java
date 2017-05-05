/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Clone a model
 */
public class CloneModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;
	
    public CloneModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE_16));
        setText("Download");
        setToolTipText("Download Remote Model");
    }

    @Override
    public void run() {
        File localGitFolder = GraficoUtils.TEST_LOCAL_GIT_FOLDER;
        
        if(localGitFolder .exists() && localGitFolder.isDirectory() && localGitFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    "Import",
                    "Local folder is not empty. Please delete it and try again!");

            return;
        }
        
        // Clone
        try {
            GraficoUtils.cloneModel(localGitFolder, GraficoUtils.TEST_REPO_URL, GraficoUtils.TEST_USER_LOGIN, GraficoUtils.TEST_USER_PASSWORD);
        }
        catch(GitAPIException | IOException ex) {
            ex.printStackTrace();
        }
        
        // Load
        try {
            GraficoUtils.loadModel(localGitFolder, fWindow.getShell());
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}
