/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;

import java.io.File;

import org.archicontribs.modelrepository.actions.CreateRepoFromModelAction;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.editor.actions.AbstractModelSelectionHandler;
import com.archimatetool.model.IArchimateModel;


/**
 * Create a Repo from existing model handler
 * 
 * @author Phillip Beauvoir
 */
public class CreateRepoFromModelHandler extends AbstractModelSelectionHandler {
    
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IArchimateModel model = getActiveArchimateModel();
        
        if(model != null) {
            CreateRepoFromModelAction action = new CreateRepoFromModelAction(HandlerUtil.getActiveWorkbenchWindowChecked(event), model);
            action.run();
        }
        
        return null;
    }
    
    @Override
    public void updateState() {
        setBaseEnabled(getActiveArchimateModel() != null && !isInRepository());
    }
    
    private boolean isInRepository() {
        IArchimateModel model = getActiveArchimateModel();
        
        if(model == null) {
            return false;
        }
        
        File file = model.getFile();
        
        return file != null && file.getParentFile().getName().equals(".git") && file.getName().equals("temp.archimate"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
