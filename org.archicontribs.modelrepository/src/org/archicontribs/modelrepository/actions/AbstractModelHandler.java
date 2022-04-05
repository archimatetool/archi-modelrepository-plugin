/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.model.IArchimateModel;


/**
 * Abstract Model handler manageing enabled state
 * 
 * @author Phillip Beauvoir
 */
public abstract class AbstractModelHandler extends AbstractHandler {
    
    protected IArchimateModel getActiveArchimateModel() {
        IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart();
        return part != null ? part.getAdapter(IArchimateModel.class) : null;
    }

    protected IArchiRepository getActiveArchiRepository() {
        if(GraficoUtils.isModelInLocalRepository(getActiveArchimateModel())) {
            return new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(getActiveArchimateModel()));
        }
        
        IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart();
        return part != null ? part.getAdapter(IArchiRepository.class) : null;
    }

    @Override
    public boolean isEnabled() {
        return GraficoUtils.isModelInLocalRepository(getActiveArchimateModel());
    }
}
