/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.views.history.HistoryView;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.archimatetool.editor.ui.services.ViewManager;


/**
 * Show History View
 * 
 * @author Phillip Beauvoir
 */
public class ShowHistoryViewHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ViewManager.toggleViewPart(HistoryView.ID, true);
        return null;
    }


}
