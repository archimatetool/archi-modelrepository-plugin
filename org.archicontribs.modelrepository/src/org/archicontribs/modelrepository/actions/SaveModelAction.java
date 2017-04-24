/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class SaveModelAction extends AbstractModelAction {

	private IWorkbenchWindow fWindow;

    public SaveModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_SAVE_16));
        setText("Save");
        setToolTipText("Save Changes");
    }

    @Override
    public void run() {
    	MessageDialog.openInformation(fWindow.getShell(), this.getText(), this.getToolTipText());
    }
}
