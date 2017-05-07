/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.eclipse.ui.IWorkbenchCommandConstants;

import com.archimatetool.editor.ui.services.ViewManager;

/**
 * Show Properties
 */
public class PropertiesAction extends AbstractModelAction {
	
    public PropertiesAction() {
        setText("P&roperties");
        // Ensures key binding is displayed
        setActionDefinitionId(IWorkbenchCommandConstants.FILE_PROPERTIES);
    }

    @Override
    public void run() {
        ViewManager.showViewPart(ViewManager.PROPERTIES_VIEW, false);
    }
}
