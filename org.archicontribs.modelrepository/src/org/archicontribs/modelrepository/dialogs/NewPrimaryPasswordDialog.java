/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
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
    
    private static final int minLength = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_LENGTH);
    private static final int minLowerCase = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_LOWERCASE_CHARS);
    private static final int minUpperCase = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_UPPERCASE_CHARS);
    private static final int minDigits = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_DIGITS);
    private static final int minSpecial = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PASSWORD_MIN_SPECIAL_CHARS);

    private ModifyListener listener = event -> {
        String input = newPasswordText.getText();
        int length = input.length();

        if(length == 0) {
            setErrorMessage(null, false);
        }
        else if(length < minLength) {
            setErrorMessage(NLS.bind("Must be a minimum of {0} characters", minLength), false);
        }
        else if(countLowerCase(input) < minLowerCase) {
            setErrorMessage(NLS.bind("Must have a minimum of {0} lower-case characters", minLowerCase), false);
        }
        else if(countUpperCase(input) < minUpperCase) {
            setErrorMessage(NLS.bind("Must have a minimum of {0} upper-case characters", minUpperCase), false);
        }
        else if(countDigits(input) < minDigits) {
            setErrorMessage(NLS.bind("Must have a minimum of {0} digits", minDigits), false);
        }
        else if(countSpecialChars(input) < minSpecial) {
            setErrorMessage(NLS.bind("Must have a minimum of {0} special characters", minSpecial), false);
        }
        else if(!input.equals(confirmPasswordText.getText())) {
            setErrorMessage(Messages.NewPrimaryPasswordDialog_2, false);
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
        
        changePasswordRadio.setSelection(primaryKeyFileExists);
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
                EncryptedCredentialsStorage.setNewPasswordForPrimaryKey(currentPasswordText.getTextChars(), newPasswordText.getText().toCharArray());
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
}