/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import java.io.IOException;

import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.propertysections.AbstractArchimatePropertySection;


/**
 * Property Section for Repo information
 * 
 * @author Phillip Beauvoir
 */
public class RepoInfoSection extends AbstractArchimatePropertySection {
    
    public static class Filter implements IFilter {
        public boolean select(Object object) {
            return object instanceof IArchiRepository;
        }
    }
    
    private Text fTextFile;
    private Text fTextURL;

    public RepoInfoSection() {
    }

    @Override
    protected void createControls(Composite parent) {
        createLabel(parent, Messages.RepoInfoSection_0, STANDARD_LABEL_WIDTH, SWT.CENTER);
        fTextFile = createSingleTextControl(parent, SWT.READ_ONLY);

        createLabel(parent, Messages.RepoInfoSection_1, STANDARD_LABEL_WIDTH, SWT.CENTER);
        fTextURL = createSingleTextControl(parent, SWT.READ_ONLY);
        
        // Because of bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=383750
        // But causes ModelRepositoryView to lose focus when selecting
        // addHiddenTextFieldToForm(parent);
    }

    @Override
    protected Adapter getECoreAdapter() {
        return null;
    }

    @Override
    protected EObject getEObject() {
        return null;
    }

    @Override
    protected void setElement(Object element) {
        if(element instanceof IArchiRepository) {
            IArchiRepository repo = (IArchiRepository)element;
            
            fTextFile.setText(repo.getLocalRepositoryFolder().getAbsolutePath());
            
            try {
                fTextURL.setText(repo.getOnlineRepositoryURL());
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
