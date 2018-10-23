/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.dialogs.SwitchBranchDialog;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Switch and checkout Branch
 */
public class SwitchBranchAction extends AbstractModelAction {
	
    public SwitchBranchAction(IWorkbenchWindow window, IArchimateModel model) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_BRANCHES));
        setText("Switch Branch");
        setToolTipText("Switch Branch");
        
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }

    @Override
    public void run() {
        // TODO Check dirty status
        
        SwitchBranchDialog dialog = new SwitchBranchDialog(fWindow.getShell(), getRepository());
        int retVal = dialog.open();
        
        String branchName = dialog.getBranchName();
        
        if(retVal == IDialogConstants.CANCEL_ID || !StringUtils.isSet(branchName)) {
            return;
        }
        
    }
}
