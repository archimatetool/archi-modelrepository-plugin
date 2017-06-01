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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Commit Model Action
 * 
 * 1. Offer to save the model
 * 2. Create Grafico files from the model
 * 3. Check if there is anything to Commit
 * 4. Show Commit dialog
 * 5. Commit
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class CommitModelAction extends AbstractModelAction {
    
    private IArchimateModel fModel;
	
    public CommitModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_COMMIT));
        setText(Messages.CommitModelAction_0);
        setToolTipText(Messages.CommitModelAction_0);
    }

    public CommitModelAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        fModel = model;
        if(fModel != null) {
            setLocalRepositoryFolder(GraficoUtils.getLocalGitFolderForModel(fModel));
        }
    }

    @Override
    public void run() {
        IArchimateModel model = fModel;
        
        // This will either return the already open model or will actually open it
        // TODO We need to load a model without opening it in the models tree. But this will need a new API in IEditorModelManager
        if(model == null) {
            model = IEditorModelManager.INSTANCE.openModel(GraficoUtils.getModelFileName(getLocalRepositoryFolder()));
        }
        
        if(model == null) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.CommitModelAction_0,
                    Messages.CommitModelAction_1);
            return;
        }
        
        // Offer to save it if dirty
        // We need to do this to keep grafico and temp files in sync
        if(IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        exportModelToGraficoFiles(model, getLocalRepositoryFolder());
        
        // Then Commit
        try {
            if(GraficoUtils.hasChangesToCommit(getLocalRepositoryFolder())) {
                offerToCommitChanges();
            }
            else {
                MessageDialog.openInformation(fWindow.getShell(),
                        Messages.CommitModelAction_0,
                        Messages.CommitModelAction_2);
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.CommitModelAction_0, ex);
        }
    }
}
