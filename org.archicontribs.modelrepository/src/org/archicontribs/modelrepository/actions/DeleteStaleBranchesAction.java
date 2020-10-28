/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Delete local branches that are no longer on the remote
 */
public class DeleteStaleBranchesAction extends AbstractModelAction {
    
    public DeleteStaleBranchesAction(IWorkbenchWindow window) {
        super(window);
        //setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_DELETE));
        setText(Messages.DeleteStaleBranchesAction_0);
        setToolTipText(getText());
    }

    private List<BranchInfo> branchInfos = new ArrayList<BranchInfo>();

    public void setSelection(IStructuredSelection selection) {
        branchInfos = new ArrayList<BranchInfo>();
        
        for(Object object : selection) {
            if(object instanceof BranchInfo && isStaleBranch((BranchInfo)object)) {
                branchInfos.add((BranchInfo)object);
            }
        }
        
        update();
    }
    
    @Override
    public void run() {
        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                Messages.DeleteStaleBranchesAction_1,
                Messages.DeleteStaleBranchesAction_2);
        if(!response) {
            return;
        }
        
        try {
            deleteBranches();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.DeleteStaleBranchesAction_3, ex);
        }
        finally {
            // Notify listeners
            notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
        }
    }
    
    private void deleteBranches() throws IOException, GitAPIException {
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            for(BranchInfo branchInfo : branchInfos) {
                git.branchDelete().setBranchNames(branchInfo.getLocalBranchNameFor(), // Delete local branch
                        branchInfo.getRemoteBranchNameFor())                          // Delete remote ref
                .setForce(true).call();                                       // Force delete in case of not merged 
            }
        }
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        if(!super.shouldBeEnabled()) {
            return false;
        }
        
        return !branchInfos.isEmpty();
    }
    
//    private List<BranchInfo> getAllStaleBranches() throws IOException, GitAPIException {
//        BranchStatus status = getRepository().getBranchStatus();
//
//        return status.getAllBranches().stream()
//                .filter(branchInfo -> isStaleBranch(branchInfo))
//                .collect(Collectors.toList());
//    }
    
    /**
     * 1. Is a Local branch
     * 2. Is not the current branch
     * 3. Is not the master branch
     * 4. Is being tracked to remote and has no remote ref
     * 5. Has been merged
     * 6. Has no unpushed commits
     */
    private boolean isStaleBranch(BranchInfo branchInfo) {
        return branchInfo.isLocal() &&
                !branchInfo.isCurrentBranch() &&
                !branchInfo.isMasterBranch() &&
                branchInfo.isRemoteDeleted() &&
                branchInfo.isMerged() && 
                !branchInfo.hasUnpushedCommits();
    }
}
