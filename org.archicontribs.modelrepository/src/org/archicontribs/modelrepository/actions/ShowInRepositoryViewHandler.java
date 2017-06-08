/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.views.repositories.ModelRepositoryView;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import com.archimatetool.editor.actions.AbstractModelSelectionHandler;
import com.archimatetool.editor.ui.services.ViewManager;


/**
 * Show In Repository View Handler
 * 
 * @author Phillip Beauvoir
 */
public class ShowInRepositoryViewHandler extends AbstractModelSelectionHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ModelRepositoryView part = (ModelRepositoryView)ViewManager.showViewPart(ModelRepositoryView.ID, false);
        if(part != null && getActiveArchimateModel() != null) {
            part.selectObject(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(getActiveArchimateModel())));
        }
        
        return null;
    }

    @Override
    public void updateState() {
        setBaseEnabled(GraficoUtils.isModelInLocalRepository(getActiveArchimateModel()));
    }

}
