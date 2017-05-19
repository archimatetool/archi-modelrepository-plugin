/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Delete local repo folder
 * 
 * @author Phillip Beauvoir
 */
public class DeleteModelAction extends AbstractModelAction {
	
    public DeleteModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_DELETE_16));
        setText("Delete");
        setToolTipText("Delete Local Copy");
    }

    @Override
    public void run() {
        boolean confirm = MessageDialog.openConfirm(fWindow.getShell(), "Delete", "Are you sure you want to delete this local repository?");
        
        if(!confirm) {
            return;
        }
        
        try {
            // Close the model in the tree
            IArchimateModel model = GraficoUtils.locateModel(getLocalRepositoryFolder());
            if(model != null) {
                boolean didClose = IEditorModelManager.INSTANCE.closeModel(model);
                if(!didClose) {
                    return;
                }
            }
            
            // Delete
            FileUtils.deleteFolder(getLocalRepositoryFolder());
        }
        catch(IOException ex) {
            displayErrorDialog("Delete", ex);
        }
    }
}
