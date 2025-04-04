/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.internal.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.components.ExtendedTitleAreaDialog;

/**
 * New Primary Password Dialog
 * 
 * @author Phil Beauvoir
 */
public class NewPrimaryPasswordDialog extends ExtendedTitleAreaDialog {

    private Text currentPasswordText;
    private Text newPasswordText;
    private Text confirmPasswordText;
    
    private Button changePasswordRadio;
    private Button createNewPasswordRadio;
    
    private Label rubric;
    
    private IPreferenceStore store = ModelRepositoryPlugin.getInstance().getPreferenceStore();
    
    private int minLowerCase = Math.max(store.getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_LOWERCASE_CHARS), 0);
    private int minUpperCase = Math.max(store.getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_UPPERCASE_CHARS), 0);
    private int minDigits = Math.max(store.getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_DIGITS), 0);
    private int minSpecial = Math.max(store.getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_SPECIAL_CHARS), 0);
    private int minLength = Math.max(store.getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_LENGTH),
                                  minLowerCase + minUpperCase + minDigits + minSpecial); // maximum of minLength or sum of constraints

    private ModifyListener listener = event -> {
        String newPassword = newPasswordText.getText();
        int newPasswordLength = newPassword.length();
        String confirmPassword = confirmPasswordText.getText();
        int confirmPasswordLength = confirmPassword.length();

        if(newPasswordLength == 0 && confirmPasswordLength == 0) {
            setErrorMessage(null, false);
        }
        else if(newPasswordLength < minLength) {
            setErrorMessage(NLS.bind(Messages.NewPrimaryPasswordDialog_11, minLength), false);
        }
        else if(countLowerCase(newPassword) < minLowerCase) {
            setErrorMessage(NLS.bind(Messages.NewPrimaryPasswordDialog_12, minLowerCase), false);
        }
        else if(countUpperCase(newPassword) < minUpperCase) {
            setErrorMessage(NLS.bind(Messages.NewPrimaryPasswordDialog_13, minUpperCase), false);
        }
        else if(countDigits(newPassword) < minDigits) {
            setErrorMessage(NLS.bind(Messages.NewPrimaryPasswordDialog_14, minDigits), false);
        }
        else if(countSpecialChars(newPassword) < minSpecial) {
            setErrorMessage(NLS.bind(Messages.NewPrimaryPasswordDialog_15, minSpecial), false);
        }
        else if(!newPassword.equals(confirmPassword)) {
            if(confirmPasswordLength >= newPasswordLength) {
                setErrorMessage(Messages.NewPrimaryPasswordDialog_2, false);
            }
            else {
                setErrorMessage(null, false);
            }
        }
        else {
            setErrorMessage(null, true);
        }
    };

    public NewPrimaryPasswordDialog(Shell parentShell) {
        super(parentShell, "NewPrimaryPasswordDialog"); //$NON-NLS-1$
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
        setTitle(Messages.NewPrimaryPasswordDialog_0);

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
        GridDataFactory.create(GridData.FILL_HORIZONTAL).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(rubric);

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
        
        changePasswordRadio.setSelection(primaryKeyFileExists);
        createNewPasswordRadio.setSelection(!primaryKeyFileExists);
        
        rubric.setEnabled(!primaryKeyFileExists);
        
        // If there is a minimum password length (either set or the sum of the password constraints) show this...
        if(minLength > 0) {
            String message = NLS.bind(" " + Messages.NewPrimaryPasswordDialog_16, minLength); //$NON-NLS-1$
            
            if(minLowerCase != 0) {
                message += NLS.bind(" " + Messages.NewPrimaryPasswordDialog_17, minLowerCase); //$NON-NLS-1$
            }
            if(minUpperCase != 0) {
                message += NLS.bind(" " + Messages.NewPrimaryPasswordDialog_18, minUpperCase); //$NON-NLS-1$
            }
            if(minDigits != 0) {
                message += NLS.bind(" " + Messages.NewPrimaryPasswordDialog_19, minDigits); //$NON-NLS-1$
            }
            if(minSpecial != 0) {
                message += NLS.bind(" " + Messages.NewPrimaryPasswordDialog_20, minSpecial); //$NON-NLS-1$
            }
            
            message = message.replaceAll(",$", "."); // replace last comma with a dot //$NON-NLS-1$ //$NON-NLS-2$
            message = Messages.NewPrimaryPasswordDialog_21 + message;
            
            Label label = new Label(container2, SWT.WRAP);
            label.setText(message);
            GridDataFactory.create(GridData.FILL_HORIZONTAL).hint(SWT.DEFAULT, SWT.DEFAULT).span(2, 1).applyTo(label);
        }
        
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

    private long countLowerCase(String inputString) {
        return inputString.chars().filter(Character::isLowerCase).count();
    }

    private long countUpperCase(String inputString) {
        return inputString.chars().filter(Character::isUpperCase).count();
    }
    
    private long countDigits(String inputString) {
        return inputString.chars().filter(Character::isDigit).count();
    }

    private long countSpecialChars(String inputString) {
        return inputString.chars().filter((s) -> !Character.isAlphabetic(s) && !Character.isDigit(s)).count();
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
                EncryptedCredentialsStorage.setNewPasswordForPrimaryKey(currentPasswordText.getTextChars(), newPasswordText.getTextChars());
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
                EncryptedCredentialsStorage.createNewPrimaryKey(newPasswordText.getTextChars());
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
    
    @Override
    protected Point getDefaultDialogSize() {
        return new Point(560, 410);
    }
}