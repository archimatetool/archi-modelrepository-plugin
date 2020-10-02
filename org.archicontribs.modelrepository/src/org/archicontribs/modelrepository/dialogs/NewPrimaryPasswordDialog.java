/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.authentication.EncryptedCredentialsStorage;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
 * New Primary Password Dialog
 * 
 * @author Phil Beauvoir
 */
public class NewPrimaryPasswordDialog extends TitleAreaDialog {

    private Text currentPasswordText;
    private Text newPasswordText;
    private Text confirmPasswordText;
    
    private Button changePasswordRadio;
    private Button createNewPasswordRadio;
    
    private Label rubric;
    
    private ModifyListener listener = event -> {
        int passwordLength = newPasswordText.getText().length();
        boolean matches = newPasswordText.getText().equals(confirmPasswordText.getText());
        
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
        
        Composite container1 = new Composite(area, SWT.NONE);
        GridDataFactory.create(GridData.FILL_HORIZONTAL).applyTo(container1);
        container1.setLayout(new GridLayout());

        final boolean primaryKeyFileExists = EncryptedCredentialsStorage.getPrimaryKeyFile().exists();
        
        changePasswordRadio = new Button(container1, SWT.RADIO);
        changePasswordRadio.setText(Messages.NewPrimaryPasswordDialog_5);
        changePasswordRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                currentPasswordText.setEnabled(primaryKeyFileExists);
            }
        });
        
        createNewPasswordRadio = new Button(container1, SWT.RADIO);
        createNewPasswordRadio.setText(Messages.NewPrimaryPasswordDialog_6);
        createNewPasswordRadio.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                rubric.setEnabled(createNewPasswordRadio.getSelection());
                currentPasswordText.setEnabled(false);
            }
        });
        
        rubric = new Label(container1, SWT.WRAP);
        rubric.setText(Messages.NewPrimaryPasswordDialog_7);
        GridDataFactory.create(GridData.FILL_HORIZONTAL).hint(100, SWT.DEFAULT).applyTo(rubric);

        Composite container2 = new Composite(container1, SWT.NONE);
        GridDataFactory.create(GridData.FILL_HORIZONTAL).applyTo(container2);
        container2.setLayout(new GridLayout(2, false));
        
        currentPasswordText = createTextField(container2, Messages.NewPrimaryPasswordDialog_8, SWT.PASSWORD);
        
        newPasswordText = createTextField(container2, Messages.NewPrimaryPasswordDialog_3, SWT.PASSWORD);
        newPasswordText.addModifyListener(listener);
        
        confirmPasswordText = createTextField(container2, Messages.NewPrimaryPasswordDialog_4, SWT.PASSWORD);
        confirmPasswordText.addModifyListener(listener);
        
        currentPasswordText.setEnabled(primaryKeyFileExists);
        changePasswordRadio.setEnabled(primaryKeyFileExists);
        createNewPasswordRadio.setSelection(!primaryKeyFileExists);
        rubric.setEnabled(!primaryKeyFileExists);
        
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

    @Override
    protected void okPressed() {
        // Change password in existing primary key
        if(changePasswordRadio.getSelection()) {
            try {
                EncryptedCredentialsStorage.setNewPasswordForPrimaryKey(currentPasswordText.getText(), newPasswordText.getText());
            }
            catch(GeneralSecurityException | IOException ex) {
                ex.printStackTrace();
                MessageDialog.openError(getShell(), Messages.NewPrimaryPasswordDialog_0,
                        Messages.NewPrimaryPasswordDialog_9);
                return;
            }
        }
        // Create new key
        else {
            try {
                EncryptedCredentialsStorage.createNewPrimaryKey(newPasswordText.getText());
            }
            catch(GeneralSecurityException | IOException ex) {
                ex.printStackTrace();
                MessageDialog.openError(getShell(), Messages.NewPrimaryPasswordDialog_0,
                        Messages.NewPrimaryPasswordDialog_10 + ex.getMessage());
                return;
            }
        }
        
        super.okPressed();
    }
    
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        super.createButtonsForButtonBar(parent);
        getButton(IDialogConstants.OK_ID).setEnabled(false);
    }
}