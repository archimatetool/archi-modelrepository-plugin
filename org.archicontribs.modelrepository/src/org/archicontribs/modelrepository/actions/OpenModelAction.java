/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;

/**
 * Open Model Action
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class OpenModelAction extends AbstractModelAction {
	
    public OpenModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_OPEN));
        setText(Messages.OpenModelAction_0);
        setToolTipText(Messages.OpenModelAction_0);
    }

    @Override
    public void run() {
        BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
            @Override
            public void run() {
                IEditorModelManager.INSTANCE.openModel(getRepository().getTempModelFile());
            }
        });
    }
}
