/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.dialogs.AddBranchDialog;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.utils.StringUtils;

/**
 * Add a Branch
 */
public class AddBranchAction extends AbstractModelAction {
    
    private BranchInfo fBranchInfo;
	
    public AddBranchAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_NEW_BRANCH));
        setText(Messages.AddBranchAction_0);
        setToolTipText(Messages.AddBranchAction_0);
    }

    @Override
    public void run() {
        AddBranchDialog dialog = new AddBranchDialog(fWindow.getShell());
        int retVal = dialog.open();
        
        String branchName = dialog.getBranchName();
        
        if(retVal == IDialogConstants.CANCEL_ID || !StringUtils.isSet(branchName)) {
            return;
        }
        
        String fullName = Constants.R_HEADS + branchName;
    	
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            // If the branch exists show error
            if(git.getRepository().findRef(fullName) != null) {
                MessageDialog.openError(fWindow.getShell(),
                        Messages.AddBranchAction_1,
                        NLS.bind(Messages.AddBranchAction_2, branchName));
            }
            else {
                // Create the branch
                git.branchCreate().setName(branchName).call();

                // Checkout if option set
                if(retVal == AddBranchDialog.ADD_BRANCH_CHECKOUT) {
                    git.checkout().setName(fullName).call();
                }
                
                // Notify listeners
                notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.AddBranchAction_1, ex);
        }
    }
    
    public void setBranch(BranchInfo branchInfo) {
        fBranchInfo = branchInfo;
        setEnabled(shouldBeEnabled());
    }
    
    @Override
    protected boolean shouldBeEnabled() {
        if(fBranchInfo != null) {
            return super.shouldBeEnabled() && fBranchInfo.isCurrentBranch();
        }
        
        return super.shouldBeEnabled();
    }

}
