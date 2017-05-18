/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.preferences;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.ui.UIUtils;
import com.archimatetool.editor.utils.StringUtils;


/**
 * Model Repository Preferences Page
 * 
 * @author Phillip Beauvoir
 */
public class ModelRepositoryPreferencePage
extends PreferencePage
implements IWorkbenchPreferencePage, IPreferenceConstants {
    
    public static final String ID = "org.archicontribs.modelrepository.preferences.ModelRepositoryPreferencePage";  //$NON-NLS-1$
    
    private static final String HELP_ID = "org.archicontribs.modelrepository.prefsValidator"; //$NON-NLS-1$
    
    private Text fUserNameTextField;
    private Text fUserEmailTextField;
    
    private Text fUserRepoFolderTextField;
    
    private Button fStoreCredentialsButton;
    
    private Button fUseProxyButton, fRequiresProxyAuthenticationButton;
    private Text fProxyHostTextField;
    private Text fProxyPortTextField;
    private Text fProxyUserNameTextField;
    private Text fProxyUserPasswordTextField;
    
	public ModelRepositoryPreferencePage() {
		setPreferenceStore(ModelRepositoryPlugin.INSTANCE.getPreferenceStore());
	}
	
    @Override
    protected Control createContents(Composite parent) {
        // Help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);

        Composite client = new Composite(parent, SWT.NULL);
        client.setLayout(new GridLayout());
        
        Label label = new Label(client, SWT.NULL);
        label.setText("NOTICE - passwords are encrypted but could still be discovered!");
        
        // User details
        Group userDetailsGroup = new Group(client, SWT.NULL);
        userDetailsGroup.setText("User Details");
        userDetailsGroup.setLayout(new GridLayout(2, false));
        userDetailsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        label = new Label(userDetailsGroup, SWT.NULL);
        label.setText("Name:");
        
        fUserNameTextField = new Text(userDetailsGroup, SWT.BORDER | SWT.SINGLE);
        fUserNameTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fUserNameTextField);

        label = new Label(userDetailsGroup, SWT.NULL);
        label.setText("Email:");
        
        fUserEmailTextField = new Text(userDetailsGroup, SWT.BORDER | SWT.SINGLE);
        fUserEmailTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fUserEmailTextField);
        
        Group settingsGroup = new Group(client, SWT.NULL);
        settingsGroup.setText("Settings");
        settingsGroup.setLayout(new GridLayout(3, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 500;
        settingsGroup.setLayoutData(gd);
        
        // Repo folder location
        label = new Label(settingsGroup, SWT.NULL);
        label.setText("Local repository folder:");
        
        fUserRepoFolderTextField = new Text(settingsGroup, SWT.BORDER | SWT.SINGLE);
        fUserRepoFolderTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fUserRepoFolderTextField);
        
        Button folderButton = new Button(settingsGroup, SWT.PUSH);
        folderButton.setText("Choose...");
        folderButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                String folderPath = chooseFolderPath();
                if(folderPath != null) {
                    fUserRepoFolderTextField.setText(folderPath);
                }
            }
        });
        
        Group otherGroup = new Group(client, SWT.NULL);
        otherGroup.setText("Repository");
        otherGroup.setLayout(new GridLayout(3, false));
        otherGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        // Store Credentials
        fStoreCredentialsButton = new Button(otherGroup, SWT.CHECK);
        fStoreCredentialsButton.setText("Store each repository's user name and password in an encrypted file");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 3;
        fStoreCredentialsButton.setLayoutData(gd);
        label = new Label(otherGroup, SWT.NULL);
        label.setText("If not enabled you will be prompted for user name and password for each action.");
        label.setLayoutData(gd);
        
        // Proxy
        Group proxyGroup = new Group(client, SWT.NULL);
        proxyGroup.setText("Proxy");
        proxyGroup.setLayout(new GridLayout(4, false));
        proxyGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        fUseProxyButton = new Button(proxyGroup, SWT.CHECK);
        fUseProxyButton.setText("Enable Proxy");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 4;
        fUseProxyButton.setLayoutData(gd);
        fUseProxyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateProxyControls();
            }
        });
        
        label = new Label(proxyGroup, SWT.NULL);
        label.setText("Host:");
        fProxyHostTextField = new Text(proxyGroup, SWT.BORDER | SWT.SINGLE);
        fProxyHostTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fProxyHostTextField.setEnabled(false);
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fProxyHostTextField);

        label = new Label(proxyGroup, SWT.NULL);
        label.setText("Port:");
        fProxyPortTextField = new Text(proxyGroup, SWT.BORDER | SWT.SINGLE);
        fProxyPortTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fProxyPortTextField.setEnabled(false);
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fProxyPortTextField);
        
        fProxyPortTextField.addVerifyListener(new VerifyListener() {
            @Override
            public void verifyText(VerifyEvent e) {
                String currentText = ((Text)e.widget).getText();
                String port = currentText.substring(0, e.start) + e.text + currentText.substring(e.end);
                try {
                    int portNum = Integer.valueOf(port);
                    if(portNum < 0 || portNum > 65535) {
                        e.doit = false;
                    }
                }
                catch(NumberFormatException ex) {
                    if(!port.equals("")) { //$NON-NLS-1$
                        e.doit = false;
                    }
                }
            }
        });
        
        fRequiresProxyAuthenticationButton = new Button(proxyGroup, SWT.CHECK);
        fRequiresProxyAuthenticationButton.setText("Requires Authentication");
        gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 4;
        fRequiresProxyAuthenticationButton.setLayoutData(gd);
        fRequiresProxyAuthenticationButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateProxyControls();
            }
        });
        
        label = new Label(proxyGroup, SWT.NULL);
        label.setText("User:");
        fProxyUserNameTextField = new Text(proxyGroup, SWT.BORDER | SWT.SINGLE);
        fProxyUserNameTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fProxyUserNameTextField.setEnabled(false);
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fProxyUserNameTextField);
        
        label = new Label(proxyGroup, SWT.NULL);
        label.setText("Password:");
        fProxyUserPasswordTextField = new Text(proxyGroup, SWT.BORDER | SWT.SINGLE | SWT.PASSWORD);
        fProxyUserPasswordTextField.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fProxyUserPasswordTextField.setEnabled(false);
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fProxyUserPasswordTextField);

        setValues();
        
        return client;
    }

    private String chooseFolderPath() {
        DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell());
        dialog.setText("Local repository folder");
        dialog.setMessage("Choose the top level folder where repositories are stored.");
        File file = new File(fUserRepoFolderTextField.getText());
        if(file.exists()) {
            dialog.setFilterPath(fUserRepoFolderTextField.getText());
        }
        return dialog.open();
    }

    private void setValues() {
        fUserNameTextField.setText(getPreferenceStore().getString(PREFS_COMMIT_USER_NAME));
        fUserEmailTextField.setText(getPreferenceStore().getString(PREFS_COMMIT_USER_EMAIL));
        fUserRepoFolderTextField.setText(getPreferenceStore().getString(PREFS_REPOSITORY_FOLDER));
        fStoreCredentialsButton.setSelection(getPreferenceStore().getBoolean(PREFS_STORE_REPO_CREDENTIALS));
        
        fUseProxyButton.setSelection(getPreferenceStore().getBoolean(PREFS_PROXY_USE));
        fProxyHostTextField.setText(getPreferenceStore().getString(PREFS_PROXY_HOST));
        fProxyPortTextField.setText(getPreferenceStore().getString(PREFS_PROXY_PORT));
        fRequiresProxyAuthenticationButton.setSelection(getPreferenceStore().getBoolean(PREFS_PROXY_REQUIRES_AUTHENTICATION));
        
        try {
            SimpleCredentialsStorage sc = new SimpleCredentialsStorage(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                    IGraficoConstants.PROXY_CREDENTIALS_FILE);
            fProxyUserNameTextField.setText(StringUtils.safeString(sc.getUserName()));
            fProxyUserPasswordTextField.setText(StringUtils.safeString(sc.getUserPassword()));
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        updateProxyControls();
    }
    
    @Override
    public boolean performOk() {
        getPreferenceStore().setValue(PREFS_COMMIT_USER_NAME, fUserNameTextField.getText());
        getPreferenceStore().setValue(PREFS_COMMIT_USER_EMAIL, fUserEmailTextField.getText());
        getPreferenceStore().setValue(PREFS_REPOSITORY_FOLDER, fUserRepoFolderTextField.getText());
        getPreferenceStore().setValue(PREFS_STORE_REPO_CREDENTIALS, fStoreCredentialsButton.getSelection());
        
        getPreferenceStore().setValue(PREFS_PROXY_USE, fUseProxyButton.getSelection());
        getPreferenceStore().setValue(PREFS_PROXY_HOST, fProxyHostTextField.getText());
        getPreferenceStore().setValue(PREFS_PROXY_PORT, fProxyPortTextField.getText());
        getPreferenceStore().setValue(PREFS_PROXY_REQUIRES_AUTHENTICATION, fRequiresProxyAuthenticationButton.getSelection());
        
        try {
            SimpleCredentialsStorage sc = new SimpleCredentialsStorage(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                    IGraficoConstants.PROXY_CREDENTIALS_FILE);
            sc.store(fProxyUserNameTextField.getText(), fProxyUserPasswordTextField.getText());
        }
        catch(NoSuchAlgorithmException | IOException ex) {
            ex.printStackTrace();
        }
        
        return true;
    }
    
    @Override
    protected void performDefaults() {
        fUserNameTextField.setText(getPreferenceStore().getDefaultString(PREFS_COMMIT_USER_NAME));
        fUserEmailTextField.setText(getPreferenceStore().getDefaultString(PREFS_COMMIT_USER_EMAIL));
        fUserRepoFolderTextField.setText(getPreferenceStore().getDefaultString(PREFS_REPOSITORY_FOLDER));
        fStoreCredentialsButton.setSelection(getPreferenceStore().getDefaultBoolean(PREFS_STORE_REPO_CREDENTIALS));
        
        fUseProxyButton.setSelection(getPreferenceStore().getDefaultBoolean(PREFS_PROXY_USE));
        fProxyHostTextField.setText(getPreferenceStore().getDefaultString(PREFS_PROXY_HOST));
        fProxyPortTextField.setText(getPreferenceStore().getDefaultString(PREFS_PROXY_PORT));
        fRequiresProxyAuthenticationButton.setSelection(getPreferenceStore().getDefaultBoolean(PREFS_PROXY_REQUIRES_AUTHENTICATION));
        fProxyUserNameTextField.setText(""); //$NON-NLS-1$
        fProxyUserPasswordTextField.setText(""); //$NON-NLS-1$
        
        updateProxyControls();
    }
    
    private void updateProxyControls() {
        fProxyHostTextField.setEnabled(fUseProxyButton.getSelection());
        fProxyPortTextField.setEnabled(fUseProxyButton.getSelection());

        fRequiresProxyAuthenticationButton.setEnabled(fUseProxyButton.getSelection());
        fProxyUserNameTextField.setEnabled(fUseProxyButton.getSelection() && fRequiresProxyAuthenticationButton.getSelection());
        fProxyUserPasswordTextField.setEnabled(fUseProxyButton.getSelection() && fRequiresProxyAuthenticationButton.getSelection());
    }
    
    public void init(IWorkbench workbench) {
    }
}