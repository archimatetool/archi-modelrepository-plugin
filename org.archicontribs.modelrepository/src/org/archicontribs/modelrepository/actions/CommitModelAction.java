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
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Commit Model Action
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class CommitModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public CommitModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_COMMIT_16));
        setText("Commit");
        setToolTipText("Commit");
    }

    @Override
    public void run() {
        if(!IEditorModelManager.INSTANCE.isModelLoaded(GraficoUtils.TEST_LOCAL_FILE)) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Commit",
                    "Model is not open. Hit Refresh!");
        }
        
        boolean doCommit = MessageDialog.openConfirm(fWindow.getShell(),
                "Commit",
                "Commit changes?");
        
        if(doCommit) {
            File localGitFolder = GraficoUtils.TEST_LOCAL_GIT_FOLDER;
            
            IArchimateModel model = null;
            
            // Find it - this method should really be API
            for(IArchimateModel m : IEditorModelManager.INSTANCE.getModels()) {
                if(GraficoUtils.TEST_LOCAL_FILE.equals(m.getFile())) {
                    model = m;
                    break;
                }
            }
            
            if(model != null) {
                try {
                    PersonIdent personIdent = new PersonIdent(GraficoUtils.TEST_COMMIT_USER_NAME, GraficoUtils.TEST_COMMIT_USER_EMAIL);
                    String commitMessage = "Test commit message from model repo!";
                    GraficoUtils.commitModel(model, localGitFolder, personIdent, commitMessage);
                }
                catch(IOException | GitAPIException ex) {
                    ex.printStackTrace();
                }
            }
        } 
    }
}
