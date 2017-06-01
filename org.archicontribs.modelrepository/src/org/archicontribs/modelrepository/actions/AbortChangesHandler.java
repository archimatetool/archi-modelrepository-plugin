/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;

import com.archimatetool.model.IArchimateModel;


/**
 * Abort changes handler
 * 
 * @author Phillip Beauvoir
 */
public class AbortChangesHandler extends AbstractModelRepoHandler {
    
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IArchimateModel model = getActiveArchimateModel();
        
        if(model != null) {
            AbortChangesAction action = new AbortChangesAction(HandlerUtil.getActiveWorkbenchWindowChecked(event), model);
            action.run();
        }
        
        return null;
    }
    
    @Override
    public void updateState() {
        setBaseEnabled(getActiveArchimateModel() != null && isModelInRepository());
    }
    
}
