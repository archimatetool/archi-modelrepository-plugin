/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.merge.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Merge a Branch
 */
public class MergeBranchAction extends AbstractModelAction {
    
    private BranchInfo fBranchInfo;
	
    public MergeBranchAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_MERGE));
        setText(Messages.MergeBranchAction_0);
        setToolTipText(Messages.MergeBranchAction_1);
    }

    @Override
    public void run() {
        if(!shouldBeEnabled()) {
            return;
        }

        // Keep a local reference in case of a notification event changing the current branch selection in the UI
        BranchInfo branchInfo = fBranchInfo;
        
        if(!MessageDialog.openConfirm(fWindow.getShell(), Messages.MergeBranchAction_1,
                NLS.bind(Messages.MergeBranchAction_4,
                        branchInfo.getShortName()))) {
            return;
        }
        
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
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.MergeBranchAction_1, ex);
        }
        
        // Then offer to Commit
        try {
            if(getRepository().hasChangesToCommit()) {
                if(!offerToCommitChanges()) {
                    return;
                }
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.MergeBranchAction_1, ex);
        }
        
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            ObjectId mergeBase = git.getRepository().resolve(branchInfo.getShortName());
            
            String message = Messages.MergeBranchAction_2 + " '" +  branchInfo.getShortName() + "'";  //$NON-NLS-1$ //$NON-NLS-2$
            
            MergeResult mergeResult = git.merge()
                    .include(mergeBase)
                    .setCommit(true)
                    // .setFastForward(FastForwardMode.NO_FF)
                    // .setSquash(false).
                    .setMessage(message)
                    .call();
            
            MergeStatus status = mergeResult.getMergeStatus();
            
            // Conflict
            if(status == MergeStatus.CONFLICTING) {
                Exception[] exception = new Exception[1];
                
                // Try to handle the merge conflict
                MergeConflictHandler handler = new MergeConflictHandler(mergeResult, branchInfo.getShortName(),
                        getRepository(), fWindow.getShell());
                
                IProgressService ps = PlatformUI.getWorkbench().getProgressService();
                ps.busyCursorWhile(new IRunnableWithProgress() {
                    public void run(IProgressMonitor pm) {
                        try {
                            handler.init(pm);
                        }
                        catch(IOException | GitAPIException ex) {
                            exception[0] = ex;
                        }
                    }
                });
                
                if(exception[0] != null) {
                    handler.resetToLocalState(); // Clean up

                    if(exception[0] instanceof CanceledException) {
                        return;
                    }
                    
                    throw exception[0];
                }
                
                boolean result = handler.openConflictsDialog();
                if(result) {
                    handler.merge();
                }
                // User cancelled - we assume they committed all changes so we can reset
                else {
                    handler.resetToLocalState();
                    notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                    return;
                }
            }
            
            // Reload the model from the Grafico XML files
            GraficoModelLoader loader = new GraficoModelLoader(getRepository());
            loader.loadModel();
            
            // Do a commit if needed
            if(getRepository().hasChangesToCommit()) {
                String commitMessage = Messages.MergeBranchAction_3;
                
                // Did we restore any missing objects?
                String restoredObjects = loader.getRestoredObjectsAsString();
                
                // Add to commit message
                if(restoredObjects != null) {
                    commitMessage += "\n\n" + Messages.RefreshModelAction_3 + "\n" + restoredObjects; //$NON-NLS-1$ //$NON-NLS-2$
                }

                getRepository().commitChanges(commitMessage, true);
            }

        }
        catch(Exception ex) {
            displayErrorDialog(Messages.MergeBranchAction_1, ex);
        }

        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
    }
    
    public void setBranch(BranchInfo branchInfo) {
        fBranchInfo = branchInfo;
        setEnabled(shouldBeEnabled());
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        return fBranchInfo != null
                && !fBranchInfo.isCurrentBranch() // Not current branch
                && fBranchInfo.isLocal() // Has to be local
                && super.shouldBeEnabled();
    }

}
