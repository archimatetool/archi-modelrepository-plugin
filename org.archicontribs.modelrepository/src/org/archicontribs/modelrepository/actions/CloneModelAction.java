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
        setText(Messages.CloneModelAction_0);
        setToolTipText(Messages.CloneModelAction_1);
    }

    @Override
    public void run() {
    	String repoURL = null;
    	String userName = null;
    	String userPassword = null;
    	
        CloneInputDialog dialog = new CloneInputDialog(fWindow.getShell());
        if(dialog.open() == Window.OK) {
            repoURL = dialog.getURL();
            userName = dialog.getUsername();
            userPassword = dialog.getPassword();
        }
    	
        File localGitFolder = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                GraficoUtils.createLocalGitFolderName(repoURL));
        
        // Folder is not empty
        if(localGitFolder.exists() && localGitFolder.isDirectory() && localGitFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.CloneModelAction_0,
                    Messages.CloneModelAction_2);

            return;
        }
        
        try {
            // Clone
            GraficoUtils.cloneModel(localGitFolder, repoURL, userName, userPassword);
            
            // Load
            GraficoUtils.loadModel(localGitFolder, fWindow.getShell());
        }
        catch(GitAPIException | IOException ex) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.CloneModelAction_0,
                    Messages.CloneModelAction_3 + " " + //$NON-NLS-1$
                    ex.getMessage());
        }
    }
}
