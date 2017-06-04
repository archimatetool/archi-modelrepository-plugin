/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Revert a particular commit
 */
public class RevertCommitAction extends AbstractModelAction {
    
    private RevCommit fCommit;
	
    public RevertCommitAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REVERT));
        setText(Messages.RevertCommitAction_0);
        setToolTipText(Messages.RevertCommitAction_0);
    }

    public void setCommit(RevCommit commit) {
        fCommit = commit;
        setEnabled(fCommit != null);
    }
    
    @Override
    public void run() {
        // This will either return the already open model or will actually open it
        // TODO We need to load a model without opening it in the models tree. But this will need a new API in IEditorModelManager
        IArchimateModel model = IEditorModelManager.INSTANCE.openModel(GraficoUtils.getModelFileName(getLocalRepositoryFolder()));
        
        if(model == null) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.RevertCommitAction_1,
                    Messages.RevertCommitAction_2);
            return;
        }

        // Offer to save it if dirty
        if(IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        exportModelToGraficoFiles(model, getLocalRepositoryFolder());
        
        // Then offer to Commit
        try {
            if(GraficoUtils.hasChangesToCommit(getLocalRepositoryFolder())) {
                if(!offerToCommitChanges()) {
                    return;
                }
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RevertCommitAction_3, ex);
            return;
        }
        
        // Close the model
        // TODO this needs changing in the Archi API
        try {
            IEditorModelManager.INSTANCE.closeModel(model);
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.RefreshModelAction_5, ex);
        }
        
        // Revert
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            RevertCommand revertCommand = git.revert();
            revertCommand.include(fCommit);
            revertCommand.call();
            
            MergeResult mergeResult = revertCommand.getFailingResult();
            if(mergeResult != null) {
                MergeConflictHandler handler = new MergeConflictHandler(mergeResult, getLocalRepositoryFolder(), fWindow.getShell());
                boolean result = handler.checkForMergeConflicts();
                if(result) {
                    handler.mergeAndCommit(Messages.RevertCommitAction_4);
                }
                else {
                    // User cancelled - we assume user has committed all changes so we can reset
                    handler.resetToLocalState();
                    return;
                }
            }
            else {
                loadModelFromGraficoFiles(getLocalRepositoryFolder());
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RevertCommitAction_1, ex);
        }

    }
    
    @Override
    protected boolean shouldBeEnabled() {
        return fCommit != null && getLocalRepositoryFolder() != null;
    }
}
