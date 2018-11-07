/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.views.history.HistoryView;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.ui.services.ViewManager;

/**
 * Show in History action
 *
 * @author Phillip Beauvoir
 */
public class ShowInHistoryAction extends AbstractModelAction {

    public ShowInHistoryAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_HISTORY_VIEW));
        setText(Messages.ShowInHistoryAction_0);
        setToolTipText(Messages.ShowInHistoryAction_0);
    }

    @Override
    public void run() {
        ViewManager.showViewPart(HistoryView.ID, false);
    }
}
