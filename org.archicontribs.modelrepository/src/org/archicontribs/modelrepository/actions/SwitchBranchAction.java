/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.dialogs.SwitchBranchDialog;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Switch and checkout Branch
 */
public class SwitchBranchAction extends AbstractModelAction {
	
    public SwitchBranchAction(IWorkbenchWindow window, IArchimateModel model) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_BRANCHES));
        setText(Messages.SwitchBranchAction_0);
        setToolTipText(Messages.SwitchBranchAction_0);
        
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }

    @Override
    public void run() {
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        try {
            getRepository().exportModelToGraficoFiles();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.SwitchBranchAction_0, ex);
        }
        
        // Then offer to Commit
        try {
            if(getRepository().hasChangesToCommit()) {
                if(!offerToCommitChanges()) {
                    return;
                }
                notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.SwitchBranchAction_0, ex);
        }
        
        // Open dialog
        SwitchBranchDialog dialog = new SwitchBranchDialog(fWindow.getShell(), getRepository());
        int retVal = dialog.open();
        
        BranchInfo branchInfo = dialog.getBranchInfo();
        
        if(retVal == IDialogConstants.CANCEL_ID || branchInfo == null) {
            return;
        }
        
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            // Switch branch
            
            // If local checkout
            if(branchInfo.isLocal()) {
                git.checkout().setName(branchInfo.getFullName()).call();
            }
            // If the branch is remote and not tracked we need create the local branch and switch to that
            else if(branchInfo.isRemote() && !branchInfo.hasTrackedRef()) {
                String branchName = branchInfo.getShortName();
                git.branchCreate().setName(branchName).setStartPoint(branchInfo.getFullName()).call();
                git.checkout().setName(branchInfo.getLocalBranchNameFor()).call();
            }
            
            // Notify listeners
            notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);

            // Reload the model from the Grafico XML files
            new GraficoModelLoader(getRepository()).loadModel();
            
            // Save the checksum
            getRepository().saveChecksum();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.SwitchBranchAction_0, ex);
        }
    }
}
