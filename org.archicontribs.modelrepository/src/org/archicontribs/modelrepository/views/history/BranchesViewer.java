/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.history;

import java.io.IOException;

import org.archicontribs.modelrepository.grafico.BranchInfo;
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
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public Object[] getElements(Object inputElement) {
                if(!(inputElement instanceof BranchStatus)) {
                    return new Object[0];
                }
                
                BranchStatus branchStatus = (BranchStatus)inputElement;
                return branchStatus.getLocalBranches().toArray();
            }
        });
        
        
        setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                BranchInfo branchInfo = (BranchInfo)element;
                String branchName = branchInfo.getShortName();
                
                if(branchInfo.isCurrentBranch()) {
                    branchName += " " + Messages.BranchesViewer_0; //$NON-NLS-1$
                }
                
                return branchName;
            }
        });
    }

    void doSetInput(IArchiRepository archiRepo) {
        // Get BranchStatus
        BranchStatus branchStatus = null;
        
        try {
            branchStatus = archiRepo.getBranchStatus();
        }
        catch(IOException | GitAPIException ex) {
            ex.printStackTrace();
        }
        
        setInput(branchStatus);
        
        // Set selection to current branch
        if(branchStatus != null) {
            BranchInfo branchInfo = branchStatus.getCurrentLocalBranch();
            if(branchInfo != null) {
                setSelection(new StructuredSelection(branchInfo));
            }
        }
        
        // And relayout
        getControl().getParent().layout();
    }
}
