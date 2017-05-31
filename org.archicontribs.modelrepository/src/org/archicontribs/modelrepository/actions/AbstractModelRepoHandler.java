/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;

import com.archimatetool.editor.actions.AbstractModelSelectionHandler;
import com.archimatetool.model.IArchimateModel;


/**
 * Abstract model repo handler
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractModelRepoHandler extends AbstractModelSelectionHandler {
    
    /**
     * @return True if the model in focus is in a repository
     */
    protected boolean isModelInRepository() {
        IArchimateModel model = getActiveArchimateModel();
        
        if(model == null) {
            return false;
        }
        
        File file = model.getFile();
        
        return file != null && file.getParentFile().getName().equals(".git") && file.getName().equals("temp.archimate"); //$NON-NLS-1$ //$NON-NLS-2$
    }
}
