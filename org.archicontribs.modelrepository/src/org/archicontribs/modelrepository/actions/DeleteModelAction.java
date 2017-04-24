package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class DeleteModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public DeleteModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_DELETE_16));
        setText("Delete");
        setToolTipText("Delete Local Copy");
    }

    @Override
    public void run() {
    	MessageDialog.openInformation(fWindow.getShell(), this.getText(), this.getToolTipText());
    }
}
