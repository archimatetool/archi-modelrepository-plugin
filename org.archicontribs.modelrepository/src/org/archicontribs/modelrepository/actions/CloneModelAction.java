/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.dialogs.CloneInputDialog;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
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
    	String repoURL = GraficoUtils.TEST_REPO_URL;
    	String userName = GraficoUtils.TEST_USER_LOGIN;
    	String userPassword = GraficoUtils.TEST_USER_PASSWORD;
    	
    	CloneInputDialog dialog = new CloneInputDialog(fWindow.getShell());
    	dialog.create();
        if(dialog.open() == Window.OK) {
            repoURL = dialog.getURL();
            userName = dialog.getUsername();
            userPassword = dialog.getPassword();
    	}
    	
        File localGitFolder = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                GraficoUtils.createLocalGitFolderName(repoURL));
        
        if(localGitFolder.exists() && localGitFolder.isDirectory() && localGitFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    "Import",
                    "Local folder is not empty. Please delete it and try again!");

            return;
        }
        
        // Clone
        try {
            GraficoUtils.cloneModel(localGitFolder, repoURL, userName, userPassword);
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
