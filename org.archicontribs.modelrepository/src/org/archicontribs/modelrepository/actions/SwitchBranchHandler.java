/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.actions.AbstractModelSelectionHandler;
import com.archimatetool.model.IArchimateModel;


/**
 * Switch Branch handler
 * 
 * @author Phillip Beauvoir
 */
public class SwitchBranchHandler extends AbstractModelSelectionHandler {
    
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IArchimateModel model = getActiveArchimateModel();
        
        if(model != null) {
            SwitchBranchAction action = new SwitchBranchAction(HandlerUtil.getActiveWorkbenchWindowChecked(event), model);
            action.run();
        }
        
        return null;
    }
    
    @Override
    public boolean isEnabled() {
        File localRepoFolder = GraficoUtils.getLocalRepositoryFolderForModel(getActiveArchimateModel());
        if(localRepoFolder != null) {
            try(Git git = Git.open(localRepoFolder)) {
                List<Ref> branches = git.branchList().call(); // Local branches
                return branches.size() > 1;
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
            }
        }
        
        return false;
    }

}
