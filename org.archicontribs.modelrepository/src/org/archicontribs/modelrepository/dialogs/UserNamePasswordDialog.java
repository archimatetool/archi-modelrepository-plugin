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
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
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
 * User Name and Password Dialog
 * 
 * @author Phil Beauvoir
 */
public class UserNamePasswordDialog extends TitleAreaDialog {

    private Text txtUsername;
    private Text txtPassword;
    
    private Button storeCredentialsButton;

    private String username;
    private char[] password;
    
    private EncryptedCredentialsStorage credentialsStorage;
    private ISecurePreferences repoNode;

    public UserNamePasswordDialog(Shell parentShell, ISecurePreferences repoNode) {
        super(parentShell);
        setTitle(Messages.UserNamePasswordDialog_0);
        this.repoNode = repoNode;
    }

    public UserNamePasswordDialog(Shell parentShell, EncryptedCredentialsStorage credentialsStorage) {
        super(parentShell);
        setTitle(Messages.UserNamePasswordDialog_0);
        this.credentialsStorage = credentialsStorage;
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.UserNamePasswordDialog_0);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.UserNamePasswordDialog_1, IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        txtUsername = createTextField(container, Messages.UserNamePasswordDialog_2, SWT.NONE);
        txtPassword = createTextField(container, Messages.UserNamePasswordDialog_3, SWT.PASSWORD);
        createPreferenceButton(container);

        return area;
    }
    
    private Text createTextField(Composite container, String message, int style) {
        Label label = new Label(container, SWT.NONE);
        label.setText(message);
        
        Text txt = new Text(container, SWT.BORDER | style);
        txt.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        return txt;
    }

    private void createPreferenceButton(Composite container) {
        storeCredentialsButton = new Button(container, SWT.CHECK);
        storeCredentialsButton.setText(Messages.UserNamePasswordDialog_4);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        storeCredentialsButton.setLayoutData(gd);
        storeCredentialsButton.setSelection(ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_STORE_REPO_CREDENTIALS));
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }

    private void saveInputCredentialsStorage() {
        username = txtUsername.getText();
        password = txtPassword.getTextChars();
        
        boolean doStoreInCredentialsFile = storeCredentialsButton.getSelection();
        
        // Store Credentials
        if(doStoreInCredentialsFile) {
            try {
                credentialsStorage.store(username, password);
            }
            catch(IOException | GeneralSecurityException ex) {
                ex.printStackTrace();
                MessageDialog.openError(getShell(),
                        Messages.UserNamePasswordDialog_5,
                        Messages.UserNamePasswordDialog_6 +
                            " " + //$NON-NLS-1$
                            ex.getMessage());
            }
        }
        // Delete credentials file
        else {
            credentialsStorage.deleteCredentialsFile();
        }
    }

    private void saveSecurePreferences() {
        username = txtUsername.getText();
        password = txtPassword.getTextChars();
        
        boolean doStoreInCredentialsFile = storeCredentialsButton.getSelection();
        
        try {
            // Store Secure Preferences
            if(doStoreInCredentialsFile) {
                repoNode.put("username", username, true); //$NON-NLS-1$
                repoNode.put("password", txtPassword.getText(), true); //$NON-NLS-1$
            }
            // Delete Secure Preferences
            else {
                repoNode.remove("username"); //$NON-NLS-1$
                repoNode.remove("password"); //$NON-NLS-1$
            }
            
            repoNode.flush();
        }
        catch(StorageException | IOException ex) {
            ex.printStackTrace();
            MessageDialog.openError(getShell(),
                    Messages.UserNamePasswordDialog_5,
                    Messages.UserNamePasswordDialog_6 +
                        " " + //$NON-NLS-1$
                        ex.getMessage());
        }
    }

    @Override
    protected void okPressed() {
        if(credentialsStorage != null) {
            saveInputCredentialsStorage();
        }
        else if(repoNode != null) {
            saveSecurePreferences();
        }
        
        super.okPressed();
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }
}