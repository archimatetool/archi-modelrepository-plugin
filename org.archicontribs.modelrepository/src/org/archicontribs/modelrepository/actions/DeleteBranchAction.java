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
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Delete Branch Action
 */
public class DeleteBranchAction extends AbstractModelAction {
    
    private BranchInfo fBranchInfo;
    
    public DeleteBranchAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_DELETE));
        setText(Messages.DeleteBranchAction_0);
        setToolTipText(Messages.DeleteBranchAction_0);
    }

    @Override
    public void run() {
        if(!shouldBeEnabled()) {
            return;
        }
        
        // Keep a local reference in case of a notification event changing the current branch selection in the UI
        BranchInfo branchInfo = fBranchInfo;

        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                Messages.DeleteBranchAction_0,
                NLS.bind(Messages.DeleteBranchAction_1, branchInfo.getShortName()));
        if(!response) {
            return;
        }
        
        try {
            deleteBranch(branchInfo, false);
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.DeleteBranchAction_0, ex);
        }
        finally {
            // Notify listeners
            notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
        }
    }
    
    protected void deleteBranch(BranchInfo branchInfo, boolean forceRemote) throws IOException, GitAPIException {
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            // Delete local branch and remote branch refs
            git.branchDelete().setBranchNames(branchInfo.getLocalBranchNameFor(),
                    branchInfo.getRemoteBranchNameFor()).setForce(true).call();
            
            // Local branch
            if(!forceRemote && branchInfo.isLocal() && !branchInfo.hasTrackedRef()) {
                return;
            }
            
            // Get User Credentials first
            UsernamePassword up = getUserNameAndPasswordFromCredentialsFileOrDialog(fWindow.getShell());
            if(up == null) {
                return;
            }
            
            // Delete remote branch
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(up.getUsername(), up.getPassword()));
            RefSpec refSpec = new RefSpec( ":" + branchInfo.getLocalBranchNameFor()); //$NON-NLS-1$
            pushCommand.setRefSpecs(refSpec);
            pushCommand.setRemote(IGraficoConstants.ORIGIN);
            pushCommand.call();
        }
    }
    
    public void setBranch(BranchInfo branchInfo) {
        fBranchInfo = branchInfo;
        setEnabled(shouldBeEnabled());
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        return super.shouldBeEnabled() && fBranchInfo != null && 
                !fBranchInfo.isCurrentBranch() &&
                !IGraficoConstants.MASTER.equals(fBranchInfo.getShortName());
    }
}
