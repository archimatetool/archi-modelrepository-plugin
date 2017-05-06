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

import com.archimatetool.editor.model.IEditorModelManager;

/**
 * Refresh model action
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class RefreshModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public RefreshModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REFRESH_16));
        setText("Refresh");
        setToolTipText("Refresh Local Copy");
    }

    @Override
    public void run() {
        // TODO we need to prompt user to save/commit changes before a pull and reload
        
        if(IEditorModelManager.INSTANCE.isModelLoaded(getGitRepository())) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Refresh",
                    "Model is already open. Close it and retry.");
        }
        else {
            try {
                GraficoUtils.pullFromRemote(getGitRepository(), GraficoUtils.TEST_USER_LOGIN, GraficoUtils.TEST_USER_PASSWORD);
                GraficoUtils.loadModel(getGitRepository(), fWindow.getShell());
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
            }
        }
    }
}
