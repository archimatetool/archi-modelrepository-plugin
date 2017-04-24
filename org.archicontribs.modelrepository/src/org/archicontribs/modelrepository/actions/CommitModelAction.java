package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class CommitModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public CommitModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_COMMIT_16));
        setText("Create Version");
        setToolTipText("Create a Version");
    }

    @Override
    public void run() {
    	MessageDialog.openInformation(fWindow.getShell(), this.getText(), this.getToolTipText());
    }
}
