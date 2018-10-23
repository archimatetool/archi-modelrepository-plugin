/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.IArchiImages;

/**
 * Add Branch Dialog
 * 
 * @author Phil Beauvoir
 */
public class AddBranchDialog extends TitleAreaDialog {

    public static final int ADD_BRANCH = 1024;
    public static final int ADD_BRANCH_CHECKOUT = 1025;
    
	private Text txtBranch;
	private String branchName;
	
    public AddBranchDialog(Shell parentShell) {
        super(parentShell);
        setTitle(Messages.AddBranchDialog_0);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.AddBranchDialog_1);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.AddBranchDialog_2, IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        txtBranch = createTextField(container, Messages.AddBranchDialog_3, SWT.NONE);
        
        txtBranch.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent e) {
                String name = txtBranch.getText();
                boolean isValidName = true;
                boolean hasChars = name.length() > 0;
                
                if(hasChars) {
                    isValidName = Repository.isValidRefName(Constants.R_HEADS + txtBranch.getText());
                }
                
                setErrorMessage(isValidName ? null : Messages.AddBranchDialog_4);
                getButton(ADD_BRANCH).setEnabled(isValidName && hasChars);
                getButton(ADD_BRANCH_CHECKOUT).setEnabled(isValidName && hasChars);
            }
        });
        
        return area;
    }
    
    private Text createTextField(Composite container, String message, int style) {
        Label label = new Label(container, SWT.NONE);
        label.setText(message);
        
        Text txt = new Text(container, SWT.BORDER | style);
        txt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        return txt;
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        Button b = createButton(parent, ADD_BRANCH, Messages.AddBranchDialog_5, true);
        b.setEnabled(false);
        
        b = createButton(parent, ADD_BRANCH_CHECKOUT, Messages.AddBranchDialog_6, false);
        b.setEnabled(false);
        
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }
    
    @Override
    protected void buttonPressed(int buttonId) {
        branchName = txtBranch.getText().trim();
        setReturnCode(buttonId);
        close();
    }

    public String getBranchName() {
        return branchName;
    }
}