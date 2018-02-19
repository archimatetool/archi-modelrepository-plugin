/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Reset to the Remote Commit Action
 * 1. If the local commit == remote commit nothing to do
 * 2. Have you saved your goddam stuff!!!???
 * 3. Export to Grafico
 * 4. Wait! There's stuff that needs committing. Are you sure? If you proceed, you'll lose your stuff, dude.
 * 5. Oh, OK then....blat!
 */
public class ResetToRemoteCommitAction extends UndoLastCommitAction {
    
    public ResetToRemoteCommitAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_RESET));
        setText(Messages.ResetToRemoteCommitAction_0);
        setToolTipText(Messages.ResetToRemoteCommitAction_0);
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
        try {
            getRepository().exportModelToGraficoFiles();
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.ResetToRemoteCommitAction_0, ex);
            return;
        }
        
        try {
            // If there are changes to commit then they'll have to be committed first or abandoned
            if(getRepository().hasChangesToCommit()) {
                if(!MessageDialog.openConfirm(fWindow.getShell(),
                        Messages.ResetToRemoteCommitAction_0,
                        Messages.ResetToRemoteCommitAction_2)) {
                    return;
                }
            }
            // Else, confirm
            else {
                boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                        Messages.ResetToRemoteCommitAction_0,
                        Messages.ResetToRemoteCommitAction_3);

                if(!response) {
                    return;
                }
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.ResetToRemoteCommitAction_4, ex);
            return;
        }
        
        // Do it!
        try {
            getRepository().resetToRef(IGraficoConstants.ORIGIN_MASTER);
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.ResetToRemoteCommitAction_0, ex);
        }

        // Reload the model from the Grafico XML files
        try {
            new GraficoModelLoader(getRepository()).loadModel();

            // Save the checksum
            getRepository().saveChecksum();
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.ResetToRemoteCommitAction_0, ex);
        }
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        try {
            return getRepository() != null && getRepository().getLocalRepositoryFolder().exists() &&
                    getRepository().hasRef(IGraficoConstants.ORIGIN_MASTER) &&
                    !getRepository().isHeadAndRemoteSame();
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        return false;
    }
}
