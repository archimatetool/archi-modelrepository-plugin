/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.graphics.Image;


public class LabelProvider implements ILabelProvider {

    @Override
    public void addListener(ILabelProviderListener listener) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
    }

    @Override
    public Image getImage(Object element) {
        return IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_MODEL);
    }

    @Override
    public String getText(Object element) {
        if(!(element instanceof IStructuredSelection)) {
            return " "; //$NON-NLS-1$
        }
        
        element = ((IStructuredSelection)element).getFirstElement();
        
        if(element instanceof IArchiRepository) {
            return ((IArchiRepository)element).getName();
        }
        
        return null;
    }

}
