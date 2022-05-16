/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import java.io.IOException;

import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.propertysections.AbstractArchiPropertySection;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;


/**
 * Property Section for Repo information
 * 
 * @author Phillip Beauvoir
 */
public class RepoInfoSection extends AbstractArchiPropertySection {
    
    public static class Filter implements IFilter {
        @Override
        public boolean select(Object object) {
            return object instanceof IArchiRepository ||
                    (object instanceof IArchimateModel && GraficoUtils.isModelInLocalRepository((IArchimateModel)object));
        }
    }
    
    private Text fTextFile, fTextURL, fTextCurrentBranch;
    
    // Store these because of the Mac focus bug
    String fFile, fURL, fBranch;
    
    private IArchiRepository fArchiRepo;

    public RepoInfoSection() {
    }

    @Override
    protected void createControls(Composite parent) {
        createLabel(parent, Messages.RepoInfoSection_0, STANDARD_LABEL_WIDTH, SWT.CENTER);
        fTextFile = createSingleTextControl(parent, SWT.READ_ONLY);

        createLabel(parent, Messages.RepoInfoSection_1, STANDARD_LABEL_WIDTH, SWT.CENTER);
        fTextURL = createSingleTextControl(parent, SWT.READ_ONLY);
        
        createLabel(parent, Messages.RepoInfoSection_2, STANDARD_LABEL_WIDTH, SWT.CENTER);
        fTextCurrentBranch = createSingleTextControl(parent, SWT.READ_ONLY);
    }

    @Override
    protected void handleSelection(IStructuredSelection selection) {
        if(selection.getFirstElement() instanceof IArchiRepository) {
            fArchiRepo = (IArchiRepository)selection.getFirstElement();
        }
        else if(selection.getFirstElement() instanceof IArchimateModel) {
            fArchiRepo = new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel((IArchimateModel)selection.getFirstElement()));
        }
        else {
            fArchiRepo = null;
        }
        
        if(fArchiRepo != null) {
            try {
                fFile = fArchiRepo.getLocalRepositoryFolder().getAbsolutePath();
                fTextFile.setText(fFile);
                
                fURL = fArchiRepo.getOnlineRepositoryURL();
                fTextURL.setText(StringUtils.safeString(fURL));
                
                fBranch = ""; //$NON-NLS-1$
                
                BranchStatus status = fArchiRepo.getBranchStatus();
                if(status != null) {
                    BranchInfo branchInfo = status.getCurrentLocalBranch();
                    if(branchInfo != null) {
                        fBranch = branchInfo.getShortName();
                    }
                }
                
                fTextCurrentBranch.setText(StringUtils.safeString(fBranch));
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
            }
        }
    }
}
