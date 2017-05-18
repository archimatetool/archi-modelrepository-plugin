/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.dialogs.CommitDialog;
import org.archicontribs.modelrepository.grafico.GraficoModelExporter;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Commit Model Action
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class CommitModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public CommitModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_COMMIT_16));
        setText("Commit Changes");
        setToolTipText("Commit Changes");
    }

    @Override
    public void run() {
        // TODO Do this without model loaded
        IArchimateModel model = GraficoUtils.locateModel(getLocalRepositoryFolder());
        
        if(model == null) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Commit Changes",
                    "Model is not open. Please open it first.");
            return;
        }
        
        if(IEditorModelManager.INSTANCE.isModelDirty(model)) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Publish",
                    "Please save your model first.");
            return;
        }
        
        // Do the Grafico Export thing first
        try {
            GraficoModelExporter exporter = new GraficoModelExporter();
            exporter.exportModelToLocalGitRepository(model, getLocalRepositoryFolder());
        }
        catch(IOException ex) {
            displayErrorDialog(fWindow.getShell(), "Grafico Export", ex);
        }
        
        // Then check if anything to commit
        try {
            if(!GraficoUtils.hasChangesToCommit(getLocalRepositoryFolder())) {
                MessageDialog.openInformation(fWindow.getShell(),
                        "Commit Changes",
                        "Nothing to commit.");
                return;
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(fWindow.getShell(), "Commit Changes", ex);
        }
        
        CommitDialog commitDialog = new CommitDialog(fWindow.getShell());
        int response = commitDialog.open();
        
        if(response == Window.OK) {
            String userName = commitDialog.getUserName();
            String userEmail = commitDialog.getUserEmail();
            String commitMessage = commitDialog.getCommitMessage();
            PersonIdent personIdent = new PersonIdent(userName, userEmail);
            
            // Store Prefs
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().setValue(IPreferenceConstants.PREFS_COMMIT_USER_NAME, userName);
            ModelRepositoryPlugin.INSTANCE.getPreferenceStore().setValue(IPreferenceConstants.PREFS_COMMIT_USER_EMAIL, userEmail);

            try {
                GraficoUtils.commitChanges(getLocalRepositoryFolder(), personIdent, commitMessage);
            }
            catch(IOException | GitAPIException ex) {
                displayErrorDialog(fWindow.getShell(), "Commit Changes", ex);
            }
        }
    }
}
