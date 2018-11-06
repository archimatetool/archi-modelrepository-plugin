/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.views.branches.BranchesView;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.ui.services.ViewManager;

/**
 * Show Branches action
 * 
 * @author Phillip Beauvoir
 */
public class ShowInBranchesViewAction extends AbstractModelAction {
    
    public ShowInBranchesViewAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_BRANCHES));
        setText(Messages.ShowInBranchesViewAction_0);
        setToolTipText(Messages.ShowInBranchesViewAction_0);
    }
    
    @Override
    public void run() {
        ViewManager.showViewPart(BranchesView.ID, false);
    }
}
