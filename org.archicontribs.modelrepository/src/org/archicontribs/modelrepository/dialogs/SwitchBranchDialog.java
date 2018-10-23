/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;
import java.util.List;

import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.archimatetool.editor.ui.IArchiImages;

/**
 * Switch Branch Dialog
 * 
 * @author Phil Beauvoir
 */
public class SwitchBranchDialog extends TitleAreaDialog {

    public static final int ADD_BRANCH = 1024;
    public static final int ADD_BRANCH_CHECKOUT = 1025;
    
	private ComboViewer branchesViewer;
	private String branchName;
    private IArchiRepository archiRepo;
	
    public SwitchBranchDialog(Shell parentShell, IArchiRepository archiRepo) {
        super(parentShell);
        setTitle("Switch Branch");
        this.archiRepo = archiRepo;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Switch Branch");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Select the branch to switch to.", IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        Label label = new Label(container, SWT.NONE);
        label.setText("Branch:");
        
        branchesViewer = new ComboViewer(container, SWT.READ_ONLY);
        branchesViewer.getControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        branchesViewer.setContentProvider(new IStructuredContentProvider() {
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            public void dispose() {
            }

            public Object[] getElements(Object inputElement) {
                try {
                    List<String> names = BranchStatus.getLocalBranchNames(archiRepo);
                    String currentBranch = BranchStatus.getCurrentLocalBranch(archiRepo);
                    names.remove(currentBranch);
                    return names.toArray();
                }
                catch(IOException | GitAPIException ex) {
                    ex.printStackTrace();
                }
                
                return new Object[0];
            }
        });

        branchesViewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return BranchStatus.getShortName((String)element);
            }
        });
        
        branchesViewer.setInput(archiRepo);
        
        Object element = branchesViewer.getElementAt(0);
        if(element != null) {
            branchesViewer.setSelection(new StructuredSelection(element));
        }

        return area;
    }
    
    @Override
    protected void okPressed() {
        branchName = (String)branchesViewer.getStructuredSelection().getFirstElement();
        super.okPressed();
    }

    public String getBranchName() {
        return branchName;
    }
}