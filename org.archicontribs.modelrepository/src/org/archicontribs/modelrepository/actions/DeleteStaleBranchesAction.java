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
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
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
        setText("Delete Stale Branches");
        setToolTipText(getText());
    }

    @Override
    public void run() {
        if(!shouldBeEnabled()) {
            return;
        }
        
        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                "Delete Stale Branches",
                "Are you sure you want to delete these branches?");
        if(!response) {
            return;
        }
        
        try {
            deleteBranches();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog("Delete Stale Branches", ex);
        }
        finally {
            // Notify listeners
            notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
        }
    }
    
    private void deleteBranches() throws IOException, GitAPIException {
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            // Delete one branch at a time and after it's deleted get the stale branches again
            // This is because deleting a branch can affect the merge status of another branch
            List<BranchInfo> staleBranches;
            do {
                staleBranches = getStaleBranches();
                if(!staleBranches.isEmpty()) {
                    BranchInfo branchInfo = staleBranches.get(0);
                    git.branchDelete().setBranchNames(branchInfo.getLocalBranchNameFor(), // Delete local branch
                            branchInfo.getRemoteBranchNameFor())                          // Delete remote ref
                            .setForce(true).call();                                       // Force delete in case of not merged 
                }
            }
            while(!staleBranches.isEmpty());
        }
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        if(!super.shouldBeEnabled()) {
            return false;
        }
        
        try {
            return !getStaleBranches().isEmpty();
        }
        catch(IOException | GitAPIException ex) {
            ex.printStackTrace();
        }
        
        return false;
    }
    
    private List<BranchInfo> getStaleBranches() throws IOException, GitAPIException {
        List<BranchInfo> list = new ArrayList<BranchInfo>();
        
        BranchStatus status = getRepository().getBranchStatus();
        
        for(BranchInfo branchInfo : status.getAllBranches()) {
            if(isStaleBranch(branchInfo)) {
                list.add(branchInfo);
            }
        }
        
        return list;
    }
    
    /**
     * 1. A Local branch
     * 4. Is not current branch
     * 5. Is not master branch
     * 2. Is being tracked to remote and has no remote ref
     * 6. Is merged
     * 7. Has no unpushed commits
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
