package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class CloneModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public CloneModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE_16));
        setText("Clone");
        setToolTipText("Clone Remote Model");
    }

    @Override
    public void run() {
    	MessageDialog.openInformation(fWindow.getShell(), this.getText(), this.getToolTipText());
    }
}
