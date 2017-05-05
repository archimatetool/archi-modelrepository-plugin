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

public class PushModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public PushModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_PUSH_16));
        setText("Push");
        setToolTipText("Push Changes to Remote");
    }

    @Override
    public void run() {
        boolean doPush = MessageDialog.openConfirm(fWindow.getShell(),
                "Push",
                "Push changes?");
        
        if(doPush) {
            File localGitFolder = GraficoUtils.TEST_LOCAL_GIT_FOLDER;
            
            try {
                GraficoUtils.pushToRemote(localGitFolder, GraficoUtils.TEST_USER_LOGIN, GraficoUtils.TEST_USER_PASSWORD);
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
            }
        } 
    }
    
    // TEMPORARY FOR TESTING!
    @Override
    public boolean isEnabled() {
        return true;
    }
}
