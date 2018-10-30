/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jgit.api.errors.GitAPIException;
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
        // Is enabled if there is more than one branch to switch to
        File localRepoFolder = GraficoUtils.getLocalRepositoryFolderForModel(getActiveArchimateModel());
        if(localRepoFolder != null) {
            IArchiRepository archiRepo = new ArchiRepository(localRepoFolder);
            try {
                BranchStatus status = archiRepo.getBranchStatus();
                return status != null && status.getLocalAndUntrackedRemoteBranches().size() > 1;
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
            }
        }
        
        return false;
    }

}
