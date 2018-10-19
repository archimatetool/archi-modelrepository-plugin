/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.history;

import java.io.IOException;
import java.util.List;

import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/**
 * Branches Viewer
 * 
 * @author Phillip Beauvoir
 */
public class BranchesViewer extends ComboViewer {

    public BranchesViewer(Composite parent) {
        super(parent, SWT.READ_ONLY);
        
        setContentProvider(new IStructuredContentProvider() {
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            public void dispose() {
            }

            public Object[] getElements(Object inputElement) {
                if(!(inputElement instanceof IArchiRepository)) {
                    return new Object[0];
                }
                
                IArchiRepository repo = (IArchiRepository)inputElement;
                
                // Local Repo was deleted
                if(!repo.getLocalRepositoryFolder().exists()) {
                    return new Object[0];
                }
                
                try {
                    List<String> names = BranchStatus.getLocalBranchNames(repo);
                    return names.toArray();
                }
                catch(IOException | GitAPIException ex) {
                    ex.printStackTrace();
                }
                
                return new Object[0];
            }
        });
        
        
        setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                String branch = (String)element;
                String current = "";  //$NON-NLS-1$
                
                try {
                    if(getInput() != null) {
                        boolean isCurrentBranch = BranchStatus.isCurrentBranch((IArchiRepository)getInput(), branch);
                        if(isCurrentBranch) {
                            current = " " + Messages.BranchesViewer_0; //$NON-NLS-1$
                        }
                    }
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
                
                return BranchStatus.getShortName(branch) + current;
            }
        });
    }

    void doSetInput(IArchiRepository archiRepo) {
        setInput(archiRepo);
        
        // Set selection to current branch
        try {
            String branch = BranchStatus.getCurrentLocalBranch(archiRepo);
            if(branch != null) {
                setSelection(new StructuredSelection(branch));
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        getControl().getParent().layout();
    }
}
