/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
            for(BranchInfo branchInfo : getStaleBranches()) {
                branchInfo.refresh(); // Refresh branch info in case of a change in the merge status since the last delete
                if(isStaleBranch(branchInfo)) {
                    git.branchDelete().setBranchNames(branchInfo.getLocalBranchNameFor(), // Delete local branch
                            branchInfo.getRemoteBranchNameFor())                          // Delete remote ref
                            .setForce(true).call();                                       // Force delete in case of not merged 
                }
            }
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
        BranchStatus status = getRepository().getBranchStatus();

        return status.getAllBranches().stream()
                .filter(branchInfo -> isStaleBranch(branchInfo))
                .collect(Collectors.toList());
    }
    
    /**
     * 1. Is a Local branch
     * 2. Is not the current branch
     * 3. Is not the master branch
     * 4. Is being tracked to remote and has no remote ref
     * 5. Is merged
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
