/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Delete Local Branches that are tacking a a Remote Branch but which does not exist
 */
public class DeleteLocalTrackedBranchesAction extends AbstractModelAction {
    
    public DeleteLocalTrackedBranchesAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_DELETE));
        setText("Delete Untracked Branches");
        setToolTipText("Delete Untracked Branches");
    }

    @Override
    public void run() {
        if(!shouldBeEnabled()) {
            return;
        }
        
        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                "Delete Untracked Branches",
                "Are you sure you want to delete these branches?");
        if(!response) {
            return;
        }
        
        try {
            deleteBranches();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog("Delete Untracked Branches", ex);
        }
        finally {
            // Notify listeners
            notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
        }
    }
    
    private void deleteBranches() throws IOException, GitAPIException {
        List<BranchInfo> deadBranches = getDeadBranches();
        
        if(!deadBranches.isEmpty()) {
            for(BranchInfo branchInfo : deadBranches) {
                try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
                    git.branchDelete().setBranchNames(branchInfo.getLocalBranchNameFor()).call();
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
            return !getDeadBranches().isEmpty();
        }
        catch(IOException | GitAPIException ex) {
            ex.printStackTrace();
        }
        
        return false;
    }
    
    private List<BranchInfo> getDeadBranches() throws IOException, GitAPIException {
        List<BranchInfo> list = new ArrayList<BranchInfo>();
        
        BranchStatus status = getRepository().getBranchStatus();
        if(status != null) {
            
            for(BranchInfo branchInfo : status.getLocalBranches()) {
                if(branchInfo.isRemoteDeleted() && !branchInfo.isCurrentBranch()) {
                    list.add(branchInfo);
                }
            }
        }
        
        return list;
    }
}
