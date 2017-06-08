/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
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
    
    public CommitModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_COMMIT));
        setText(Messages.CommitModelAction_0);
        setToolTipText(Messages.CommitModelAction_0);
    }

    public CommitModelAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }

    @Override
    public void run() {
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }

        // Do the Grafico Export first
        exportModelToGraficoFiles();
        
        // Then Commit
        try {
            if(getRepository().hasChangesToCommit()) {
                if(offerToCommitChanges()) {
                    notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                }
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
