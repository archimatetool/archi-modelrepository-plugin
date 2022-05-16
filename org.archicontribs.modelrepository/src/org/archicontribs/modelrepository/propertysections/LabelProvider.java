/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.swt.graphics.Image;

import com.archimatetool.editor.ui.IArchiLabelProvider;


public class LabelProvider implements IArchiLabelProvider {

    @Override
    public Image getImage(Object element) {
        if(element instanceof IArchiRepository) {
            return IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_MODEL);
        }
        return null;
    }

    @Override
    public String getLabel(Object element) {
        if(element instanceof IArchiRepository) {
            return ((IArchiRepository)element).getName();
        }
        
        return null;
    }
}
