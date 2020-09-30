/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.IArchiImages;

/**
 * New Primary Password Dialog
 * 
 * @author Phil Beauvoir
 */
public class NewPrimaryPasswordDialog extends TitleAreaDialog {

    private Text txtPassword1;
    private Text txtPassword2;

    private String password;

    public NewPrimaryPasswordDialog(Shell parentShell) {
        super(parentShell);
        setTitle(Messages.NewPrimaryPasswordDialog_0);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.NewPrimaryPasswordDialog_0);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.NewPrimaryPasswordDialog_1, IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        ModifyListener listener = event -> {
            int passwordLength = txtPassword1.getText().length();
            boolean matches = txtPassword1.getText().equals(txtPassword2.getText());
            
            if(!matches) {
                setErrorMessage(Messages.NewPrimaryPasswordDialog_2, false);
            }
            else if(passwordLength == 0) {
                setErrorMessage(null, false);
            }
            else {
                setErrorMessage(null, true);
            }
        };
        
        txtPassword1 = createTextField(container, Messages.NewPrimaryPasswordDialog_3, SWT.PASSWORD);
        txtPassword1.addModifyListener(listener);
        
        txtPassword2 = createTextField(container, Messages.NewPrimaryPasswordDialog_4, SWT.PASSWORD);
        txtPassword2.addModifyListener(listener);
        
        return area;
    }
    
    private void setErrorMessage(String message, boolean okEnabled) {
        super.setErrorMessage(message);
        getButton(IDialogConstants.OK_ID).setEnabled(okEnabled);
    }
    
    private Text createTextField(Composite container, String message, int style) {
        Label label = new Label(container, SWT.NONE);
        label.setText(message);
        
        Text txt = new Text(container, SWT.BORDER | style);
        txt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        return txt;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    // save content of the Text fields because they get disposed
    // as soon as the Dialog closes
    private void saveInput() {
        password = txtPassword1.getText();
    }

    @Override
    protected void okPressed() {
        saveInput();
        super.okPressed();
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
    
    public String getPassword() {
        return password;
    }
}