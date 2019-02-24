/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.merge.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand.FastForwardMode;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Merge a Branch
 */
public class MergeBranchAction extends AbstractModelAction {
    
    protected static final int MERGE_STATUS_ERROR = -1;
    protected static final int MERGE_STATUS_OK = 0;
    protected static final int MERGE_STATUS_UP_TO_DATE = 1;
    protected static final int MERGE_STATUS_MERGE_CANCEL = 2;

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
        
        int response = MessageDialog.open(MessageDialog.QUESTION,
                fWindow.getShell(),
                Messages.MergeBranchAction_1,
                Messages.MergeBranchAction_5,
                SWT.NONE,
                Messages.MergeBranchAction_6,
                Messages.MergeBranchAction_7,
                Messages.MergeBranchAction_8);
        
        // Cancel
        if(response == -1 || response == 2) {
            return;
        }

        try {
            if(response == 0) {
                doOnlineMerge(fBranchInfo);
            }
            
            if(response == 1) {
                doLocalMerge(fBranchInfo);
            }
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.MergeBranchAction_1, ex);
        }
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
        notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
    }
    
    private void doLocalMerge(BranchInfo branchToMerge) throws Exception {
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        getRepository().exportModelToGraficoFiles();

        // Then offer to Commit
        if(getRepository().hasChangesToCommit()) {
            if(!offerToCommitChanges()) {
                return;
            }
        }

        // Store currentBranch first
        BranchInfo currentBranch = getRepository().getBranchStatus().getCurrentLocalBranch();

        // Merge
        merge(currentBranch, branchToMerge);
    }
    
    private void doOnlineMerge(BranchInfo branchToMerge) throws Exception {
        // Store currentBranch first
        BranchInfo currentBranch = getRepository().getBranchStatus().getCurrentLocalBranch();
        
        PushModelAction pushAction = new PushModelAction(fWindow, getRepository().locateModel());

        // Init
        UsernamePassword up = pushAction.init();
        if(up == null) {
            return;
        }
        
        // Pull
        int pullStatus = pushAction.pull(up);
        
        // Push
        if(pullStatus == RefreshModelAction.PULL_STATUS_OK || pullStatus == RefreshModelAction.PULL_STATUS_UP_TO_DATE) {
            pushAction.push(up);
        }
        else {
            return;
        }
        
        // Switch to other branch
        SwitchBranchAction switchBranchAction = new SwitchBranchAction(fWindow);
        switchBranchAction.setRepository(getRepository());
        switchBranchAction.switchBranch(branchToMerge, true);
        
        // Pull
        pullStatus = pushAction.pull(up);
        
        // Push
        if(pullStatus == RefreshModelAction.PULL_STATUS_OK || pullStatus == RefreshModelAction.PULL_STATUS_UP_TO_DATE) {
            pushAction.push(up);
        }
        else {
            return;
        }
        
        // Switch back
        switchBranchAction.switchBranch(currentBranch, true);
        
        // Merge
        merge(currentBranch, branchToMerge);
        
        // Final Push on this branch
        pushAction.push(up);
        
        // Ask user to delete branch (if not master)
        DeleteBranchAction deleteBranchAction = new DeleteBranchAction(fWindow);
        deleteBranchAction.setRepository(getRepository());
        deleteBranchAction.setBranch(branchToMerge);
        if(deleteBranchAction.shouldBeEnabled()) {
            boolean doDeleteBranch = MessageDialog.openQuestion(fWindow.getShell(),
                    Messages.MergeBranchAction_1,
                    NLS.bind(Messages.MergeBranchAction_9, branchToMerge.getShortName()));

            if(doDeleteBranch) {
                // Branch will have been pushed at this point so BranchInfo is no longer valid to determine if it's just a local branch
                deleteBranchAction.deleteBranch(branchToMerge, true);
            }
        }
    }
    
    private int merge(BranchInfo currentBranch, BranchInfo branchToMerge) throws Exception {
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            ObjectId mergeBase = git.getRepository().resolve(branchToMerge.getShortName());
            
            String mergeMessage = NLS.bind(Messages.MergeBranchAction_2, branchToMerge.getShortName(), currentBranch.getShortName());
            
            MergeResult mergeResult = git.merge()
                    .include(mergeBase)
                    .setCommit(true)
                    .setFastForward(FastForwardMode.FF)
                    .setStrategy(MergeStrategy.RECURSIVE)
                    .setSquash(false)
                    .setMessage(mergeMessage)
                    .call();
            
            MergeStatus status = mergeResult.getMergeStatus();
            
            // Conflict
            if(status == MergeStatus.CONFLICTING) {
                Exception[] exception = new Exception[1];
                
                // Try to handle the merge conflict
                MergeConflictHandler handler = new MergeConflictHandler(mergeResult, branchToMerge.getShortName(),
                        getRepository(), fWindow.getShell());
                
                IProgressService ps = PlatformUI.getWorkbench().getProgressService();
                ps.busyCursorWhile(new IRunnableWithProgress() {
                    @Override
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
                        return MERGE_STATUS_MERGE_CANCEL;
                    }
                    
                    throw exception[0];
                }
                
                String dialogMessage = NLS.bind(Messages.MergeBranchAction_10,
                        branchToMerge.getShortName(), currentBranch.getShortName());
                
                boolean result = handler.openConflictsDialog(dialogMessage);
                
                if(result) {
                    handler.merge();
                }
                // User cancelled - so we reset
                else {
                    handler.resetToLocalState();
                    notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                    return MERGE_STATUS_MERGE_CANCEL;
                }
            }
            
            // Reload the model from the Grafico XML files
            GraficoModelLoader loader = new GraficoModelLoader(getRepository());
            loader.loadModel();
            
            // Do a commit if needed
            if(getRepository().hasChangesToCommit()) {
                mergeMessage = NLS.bind(Messages.MergeBranchAction_3, branchToMerge.getShortName(), currentBranch.getShortName());
                
                // Did we restore any missing objects?
                String restoredObjects = loader.getRestoredObjectsAsString();
                
                // Add to commit message
                if(restoredObjects != null) {
                    mergeMessage += "\n\n" + Messages.RefreshModelAction_3 + "\n" + restoredObjects; //$NON-NLS-1$ //$NON-NLS-2$
                }

                // IMPORTANT!!! "amend" has to be false after a merge conflict or else the commit will be orphaned
                getRepository().commitChanges(mergeMessage, false);
            }
        }
        
        return MERGE_STATUS_OK;
    }
    
    private boolean isBranchRefSameAsCurrentBranchRef(BranchInfo branchInfo) {
        try {
            BranchInfo currentLocalBranch = getRepository().getBranchStatus().getCurrentLocalBranch();
            return currentLocalBranch != null && currentLocalBranch.getRef().getObjectId().equals(branchInfo.getRef().getObjectId());
        }
        catch(IOException | GitAPIException ex) {
            ex.printStackTrace();
        }
        
        return false;
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
                && !isBranchRefSameAsCurrentBranchRef(fBranchInfo) // Not same ref
                && super.shouldBeEnabled();
    }

}
