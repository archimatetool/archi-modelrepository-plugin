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
    
    protected RevCommit fCommit;
	
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
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = GraficoUtils.locateModel(getLocalRepositoryFolder());
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        exportModelToGraficoFiles();
        
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
        
        // Revert
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            RevertCommand revertCommand = doRevertCommand(git);
            
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
                loadModelFromGraficoFiles();
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RevertCommitAction_1, ex);
        }

    }
    
    /**
     * @throws IOException
     */
    protected RevertCommand doRevertCommand(Git git) throws GitAPIException, IOException {
        RevertCommand revertCommand = git.revert();
        revertCommand.include(fCommit);
        revertCommand.call();
        return revertCommand;
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        return fCommit != null && getLocalRepositoryFolder() != null;
    }
}
