/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Merge a Branch
 */
public class MergeBranchAction2 extends AbstractModelAction {
    
    private BranchInfo fBranchToMerge;
	
    public MergeBranchAction2(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_MERGE));
        setText("Experimental Merge");
        setToolTipText(Messages.MergeBranchAction_1);
    }

    @Override
    public void run() {
        if(!shouldBeEnabled()) {
            return;
        }

        // Keep a local reference in case of a notification event changing the current branch selection in the UI
        BranchInfo branchToMerge = fBranchToMerge;

        // Confirmation
        if(!MessageDialog.openConfirm(fWindow.getShell(), Messages.MergeBranchAction_1,
                NLS.bind(Messages.MergeBranchAction_4,
                        branchToMerge.getShortName()))) {
            return;
        }
        
        try {
            // Store currentBranch first
            BranchInfo currentBranch = getRepository().getBranchStatus().getCurrentLocalBranch();
            
            PushModelAction pushAction = new PushModelAction(fWindow, getRepository().locateModel());

            // Init
            UsernamePassword up = pushAction.init();
            if(up == null) {
                return;
            }
            
            // Pull
            int status = pushAction.pull(up);
            
            // Push
            if(status == RefreshModelAction.PULL_STATUS_OK || status == RefreshModelAction.PULL_STATUS_UP_TO_DATE) {
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
            status = pushAction.pull(up);
            
            // Push
            if(status == RefreshModelAction.PULL_STATUS_OK || status == RefreshModelAction.PULL_STATUS_UP_TO_DATE) {
                pushAction.push(up);
            }
            else {
                return;
            }
            
            // Switch back
            switchBranchAction.switchBranch(currentBranch, true);
            
            // Merge
            MergeBranchAction mergeBranchAction = new MergeBranchAction(fWindow);
            mergeBranchAction.setRepository(getRepository());
            mergeBranchAction.merge(branchToMerge);
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.MergeBranchAction_1, ex);
        }
        finally {
            notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
            notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
        }
    }
    
    public void setBranch(BranchInfo branchInfo) {
        fBranchToMerge = branchInfo;
        setEnabled(shouldBeEnabled());
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        return fBranchToMerge != null
                && !fBranchToMerge.isCurrentBranch() // Not current branch
                && fBranchToMerge.isLocal() // Has to be local
                && super.shouldBeEnabled();
    }

}
