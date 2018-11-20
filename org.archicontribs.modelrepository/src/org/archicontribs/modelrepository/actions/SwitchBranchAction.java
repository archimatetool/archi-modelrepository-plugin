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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Switch and checkout Branch
 */
public class SwitchBranchAction extends AbstractModelAction {
    
    private BranchInfo fBranchInfo;
    
    public SwitchBranchAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_BRANCHES));
        setText(Messages.SwitchBranchAction_0);
        setToolTipText(Messages.SwitchBranchAction_0);
    }

    @Override
    public void run() {
        if(!shouldBeEnabled()) {
            return;
        }

        // Keep a local reference in case of a notification event changing the current branch selection in the UI
        BranchInfo branchInfo = fBranchInfo;
        
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        boolean notifyHistoryChanged = false;
        
        try {
            // Will we require a switch to a different commit point?
            boolean isCommitSameAsCurrentBranch = isCommitSameAsCurrentBranch(branchInfo);
            
            // Do the Grafico Export first
            getRepository().exportModelToGraficoFiles();
            
            // If there are changes to commit...
            if(getRepository().hasChangesToCommit()) {
                boolean doCommit = true;
                
                // If target branch ref is same as the current commit we don't actully need to commit changes
                // But we should ask the user first...
                if(isCommitSameAsCurrentBranch) {
                    // Ask user
                    doCommit = MessageDialog.openQuestion(fWindow.getShell(), Messages.SwitchBranchAction_0,
                            Messages.SwitchBranchAction_1);
                }
                
                // Commit dialog
                if(doCommit && !offerToCommitChanges()) {
                    return;
                }
                
                notifyHistoryChanged = true;
            }
            
            // Switch branch
            switchBranch(branchInfo, !isCommitSameAsCurrentBranch);
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.SwitchBranchAction_0, ex);
        }
        
        // Notify listeners last because a new UI selection will trigger an updated BranchInfo here
        if(notifyHistoryChanged) {
            notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
        }
        notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
    }
    
    protected void switchBranch(BranchInfo branchInfo, boolean doReloadGrafico) throws IOException, GitAPIException {
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            // If the branch is local just checkout
            if(branchInfo.isLocal()) {
                git.checkout().setName(branchInfo.getFullName()).call();
            }
            // If the branch is remote and not tracked we need create the local branch and switch to that
            else if(branchInfo.isRemote() && !branchInfo.hasTrackedRef()) {
                String branchName = branchInfo.getShortName();
                
                // Create local branch at point of remote branch ref
                Ref ref = git.branchCreate()
                        .setName(branchName)
                        .setStartPoint(branchInfo.getFullName())
                        .call();
                
                // checkout
                git.checkout().setName(ref.getName()).call();
            }
            
            // Reload the model from the Grafico XML files
            if(doReloadGrafico) {
                new GraficoModelLoader(getRepository()).loadModel();
                
                // Save the checksum
                getRepository().saveChecksum();
            }
        }
    }
    
    protected boolean isCommitSameAsCurrentBranch(BranchInfo branchInfo) throws IOException, GitAPIException {
        BranchInfo currentBranch = getRepository().getBranchStatus().getCurrentLocalBranch();
        
        try(Repository repository = Git.open(getRepository().getLocalRepositoryFolder()).getRepository()) {
            Ref currentRef = repository.exactRef(currentBranch.getFullName());
            Ref branchRef = repository.exactRef(branchInfo.getFullName());
            
            return currentRef != null &&
                    branchRef != null &&
                    currentRef.getObjectId().equals(branchRef.getObjectId());
        }
    }
    
    public void setBranch(BranchInfo branchInfo) {
        fBranchInfo = branchInfo;
        setEnabled(shouldBeEnabled());
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        return fBranchInfo != null && !fBranchInfo.isCurrentBranch() && super.shouldBeEnabled();
    }
}
