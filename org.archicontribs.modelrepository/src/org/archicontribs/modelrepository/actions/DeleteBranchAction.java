/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

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
            Exception[] exception = new Exception[1];
            IProgressService ps = PlatformUI.getWorkbench().getProgressService();
            ps.busyCursorWhile(new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor pm) {
                    try {
                        pm.beginTask(Messages.DeleteBranchAction_2, -1);
                        deleteBranch(branchInfo, false);
                    }
                    catch(Exception ex) {
                        exception[0] = ex;
                    }
                }
            });
            
            if(exception[0] != null) {
                throw exception[0];
            }
        }
        catch(Exception ex) {
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
            
            // Local branch with no remote ref
            if(!forceRemote && branchInfo.isLocal() && !branchInfo.hasRemoteRef()) {
                return;
            }
            
            UsernamePassword npw = getUsernamePassword();
            
            // Delete remote branch
            PushCommand pushCommand = git.push();
            pushCommand.setTransportConfigCallback(CredentialsAuthenticator.getTransportConfigCallback(getRepository().getOnlineRepositoryURL(), npw));
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
