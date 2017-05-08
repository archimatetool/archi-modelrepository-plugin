/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.archicontribs.modelrepository.preferences.ModelRepositoryPreferencePage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.archimatetool.editor.utils.StringUtils;
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
        setText("Commit");
        setToolTipText("Commit");
    }

    @Override
    public void run() {
        if(!GraficoUtils.isModelLoaded(getGitRepository())) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Commit",
                    "Model is not open. Hit Refresh!");
            return;
        }
        
        boolean doCommit = MessageDialog.openConfirm(fWindow.getShell(),
                "Commit",
                "Commit changes?");
        
        if(doCommit) {
            IArchimateModel model = GraficoUtils.locateModel(getGitRepository());
            if(model != null) {
                try {
                    String userName = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_NAME);
                    String userEmail = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_EMAIL);
                    
                    if(!StringUtils.isSet(userName) || !StringUtils.isSet(userEmail)) {
                        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                                    "Commit",
                                    "User name and/or email not set. Set now?");
                        if(response) {
                            // Open Preferences
                            PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(fWindow.getShell(),
                                    ModelRepositoryPreferencePage.ID, null, null);
                            if(dialog != null) {
                                //ModelRepositoryPreferencePage page = (ModelRepositoryPreferencePage)dialog.getSelectedPage();
                                //page.selectSomeTab();
                                dialog.open();
                                return;
                            }
                        }
                        else{
                            return;
                        }
                    }
                    
                    PersonIdent personIdent = new PersonIdent(userName, userEmail);
                    String commitMessage = "Test commit message from model repo!";
                    GraficoUtils.commitModel(model, getGitRepository(), personIdent, commitMessage);
                }
                catch(IOException | GitAPIException ex) {
                    ex.printStackTrace();
                }
            }
        } 
    }
}
