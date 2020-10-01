/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.merge.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

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
    }
    
    private void doLocalMerge(BranchInfo branchToMerge) throws IOException, GitAPIException {
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

        // Do main action with PM dialog
        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                try {
                    pmDialog.run(false, true, new IRunnableWithProgress() {
                        @Override
                        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                            try {
                                monitor.beginTask(Messages.MergeBranchAction_11, -1);
                                
                                // Store currentBranch first
                                BranchInfo currentBranch = getRepository().getBranchStatus().getCurrentLocalBranch();
                                merge(currentBranch, branchToMerge, pmDialog);
                            }
                            catch(Exception ex) {
                                pmDialog.getShell().setVisible(false);
                                displayErrorDialog(Messages.MergeBranchAction_1, ex);
                            }
                            finally {
                                try {
                                    saveChecksumAndNotifyListeners();
                                }
                                catch(IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    });
                }
                catch(InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    
    private void doOnlineMerge(BranchInfo branchToMerge) throws IOException, GitAPIException, GeneralSecurityException {
        // Store currentBranch first
        BranchInfo currentBranch = getRepository().getBranchStatus().getCurrentLocalBranch();
        
        PushModelAction pushAction = new PushModelAction(fWindow, getRepository().locateModel());

        // Init
        int status = pushAction.init();
        if(status == RefreshModelAction.USER_CANCEL) {
            return;
        }
        
        // Do this before opening the progress dialog
        UsernamePassword npw = getUsernamePassword();
        
        // Do main action with PM dialog
        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                
                try {
                    pmDialog.run(false, true, new IRunnableWithProgress() {
                        @Override
                        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                            try {
                                monitor.beginTask(Messages.MergeBranchAction_11, -1);
                                
                                // Pull
                                int pullStatus = pushAction.pull(npw, pmDialog);
                                
                                // Push
                                if(pullStatus == RefreshModelAction.PULL_STATUS_OK || pullStatus == RefreshModelAction.PULL_STATUS_UP_TO_DATE) {
                                    pushAction.push(npw, pmDialog);
                                }
                                else {
                                    return;
                                }
                                
                                // Switch to other branch
                                pmDialog.getProgressMonitor().subTask(Messages.MergeBranchAction_14);
                                SwitchBranchAction switchBranchAction = new SwitchBranchAction(fWindow);
                                switchBranchAction.setRepository(getRepository());
                                switchBranchAction.switchBranch(branchToMerge, true);
                                
                                // Pull again
                                pullStatus = pushAction.pull(npw, pmDialog);
                                
                                // Push
                                if(pullStatus == RefreshModelAction.PULL_STATUS_OK || pullStatus == RefreshModelAction.PULL_STATUS_UP_TO_DATE) {
                                    pushAction.push(npw, pmDialog);
                                }
                                else {
                                    return;
                                }
                                
                                // Switch back
                                pmDialog.getProgressMonitor().subTask(Messages.MergeBranchAction_14);
                                switchBranchAction.switchBranch(currentBranch, true);
                                
                                // Merge
                                merge(currentBranch, branchToMerge, pmDialog);
                                
                                // Final Push on this branch
                                pushAction.push(npw, pmDialog);
                                
                                // Ask user to delete branch (if not master)
                                DeleteBranchAction deleteBranchAction = new DeleteBranchAction(fWindow);
                                deleteBranchAction.setRepository(getRepository());
                                deleteBranchAction.setBranch(branchToMerge);
                                
                                if(deleteBranchAction.shouldBeEnabled()) {
                                    pmDialog.getShell().setVisible(false);
                                    boolean doDeleteBranch = MessageDialog.openQuestion(fWindow.getShell(),
                                            Messages.MergeBranchAction_1,
                                            NLS.bind(Messages.MergeBranchAction_9, branchToMerge.getShortName()));

                                    if(doDeleteBranch) {
                                        pmDialog.getShell().setVisible(true);
                                        pmDialog.getProgressMonitor().subTask(Messages.MergeBranchAction_12);
                                        // Branch will have been pushed at this point so BranchInfo is no longer valid to determine if it's just a local branch
                                        deleteBranchAction.deleteBranchAndPush(branchToMerge, npw);
                                    }
                                }
                            }
                            catch(Exception ex) {
                                pmDialog.getShell().setVisible(false);
                                displayErrorDialog(Messages.MergeBranchAction_1, ex);
                            }
                            finally {
                                try {
                                    saveChecksumAndNotifyListeners();
                                }
                                catch(IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    });
                }
                catch(InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });

    }
    
    private int merge(BranchInfo currentBranch, BranchInfo branchToMerge, ProgressMonitorDialog pmDialog) throws GitAPIException, IOException {
        pmDialog.getProgressMonitor().subTask(Messages.MergeBranchAction_13);
        Display.getCurrent().readAndDispatch();  // update dialog

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
                // Try to handle the merge conflict
                MergeConflictHandler handler = new MergeConflictHandler(mergeResult, branchToMerge.getShortName(),
                        getRepository(), fWindow.getShell());
                
                try {
                    handler.init(pmDialog.getProgressMonitor());
                }
                catch(IOException | GitAPIException ex) {
                    handler.resetToLocalState(); // Clean up

                    if(ex instanceof CanceledException) {
                        return MERGE_STATUS_MERGE_CANCEL;
                    }

                    throw ex;
                }
                
                String dialogMessage = NLS.bind(Messages.MergeBranchAction_10,
                        branchToMerge.getShortName(), currentBranch.getShortName());
                
                pmDialog.getShell().setVisible(false);
                
                boolean result = handler.openConflictsDialog(dialogMessage);
                
                pmDialog.getShell().setVisible(true);
                
                if(result) {
                    handler.merge();
                }
                // User cancelled - so we reset
                else {
                    handler.resetToLocalState();
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
