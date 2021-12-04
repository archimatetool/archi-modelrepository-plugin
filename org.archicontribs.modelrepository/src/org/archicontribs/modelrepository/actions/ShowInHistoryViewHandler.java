/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;


/**
 * Show In History View Handler
 * 
 * @author Phillip Beauvoir
 */
public class ShowInHistoryViewHandler extends AbstractModelHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ShowInHistoryAction action = new ShowInHistoryAction(HandlerUtil.getActiveWorkbenchWindowChecked(event));
        action.run();
        return null;
    }

}
