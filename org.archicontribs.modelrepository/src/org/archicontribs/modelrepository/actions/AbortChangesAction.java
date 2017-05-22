/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Abort Changes Action
 * 
 * @author Phillip Beauvoir
 */
public class AbortChangesAction extends AbstractModelAction {
	
    public AbortChangesAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_ABORT_16));
        setText("Abort Changes");
        setToolTipText("Abort unpublished changes");
    }

    @Override
    public void run() {
        
    }
}
