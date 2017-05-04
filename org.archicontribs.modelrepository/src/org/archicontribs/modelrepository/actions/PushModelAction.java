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
import com.archimatetool.model.IArchimateModel;

public class PushModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public PushModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_PUSH_16));
        setText("Push");
        setToolTipText("Push Changes to Remote Model");
    }

    @Override
    public void run() {
        boolean doCommitAndPush = MessageDialog.openConfirm(fWindow.getShell(),
                "Commit and Push",
                "Commit changes and Push?");
        
        if(doCommitAndPush) {
            File localGitFolder = GraficoUtils.TEST_LOCAL_GIT_FOLDER;
            
            IArchimateModel model = null;
            
            // Load if needed
            if(!IEditorModelManager.INSTANCE.isModelLoaded(GraficoUtils.TEST_LOCAL_FILE)) {
                try {
                    model = GraficoUtils.loadModel(localGitFolder, fWindow.getShell());
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
            else {
                // Find it - needs this to be made public!!
                for(IArchimateModel m : IEditorModelManager.INSTANCE.getModels()) {
                    if(GraficoUtils.TEST_LOCAL_FILE.equals(m.getFile())) {
                        model = m;
                        break;
                    }
                }
            }
            
            if(model != null) {
                try {
                    GraficoUtils.commitModel(model, localGitFolder);
                }
                catch(IOException | GitAPIException ex) {
                    ex.printStackTrace();
                }
            }
        } 
    }
    
    // TEMPORARY FOR TESTING!
    @Override
    public boolean isEnabled() {
        return true;
    }
}
