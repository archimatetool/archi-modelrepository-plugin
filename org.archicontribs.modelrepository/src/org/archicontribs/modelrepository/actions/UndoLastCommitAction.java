/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Undo the last commit
 */
public class UndoLastCommitAction extends AbstractModelAction {
    
    public UndoLastCommitAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_UNDO_COMMIT));
        setText(Messages.UndoLastCommitAction_0);
        setToolTipText(Messages.UndoLastCommitAction_0);
    }

    @Override
    public void run() {
        // If the latest commit (i.e. local and remote head are the same) has already been pushed we can't roll back to the previous commit
        try {
            if(getRepository().isHeadAndRemoteSame()) {
                MessageDialog.openError(fWindow.getShell(),
                        Messages.UndoLastCommitAction_0,
                        Messages.UndoLastCommitAction_3);
                return;
            }
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
        }
        
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                Messages.UndoLastCommitAction_0,
                Messages.UndoLastCommitAction_1);

        if(!response) {
            return;
        }
        
        try {
            // Do it!
            getRepository().resetToRef("HEAD^"); //$NON-NLS-1$
            
            // Reload the model from the Grafico XML files
            new GraficoModelLoader(getRepository()).loadModel();
            
            // Save the checksum
            getRepository().saveChecksum();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
        }
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
    }
}
