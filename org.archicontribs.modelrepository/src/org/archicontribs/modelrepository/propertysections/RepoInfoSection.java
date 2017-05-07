/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.grafico.GraficoUtils;
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
            return (object instanceof File) && GraficoUtils.isGitRepository((File)object);
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
        if(element instanceof File) {
            File f = (File)element;
            
            fTextFile.setText(f.getAbsolutePath());
            
            try {
                fTextURL.setText(GraficoUtils.getRepositoryURL(f));
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
