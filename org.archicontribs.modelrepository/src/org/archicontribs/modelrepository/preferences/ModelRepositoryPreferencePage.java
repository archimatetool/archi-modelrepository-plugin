/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.preferences;

import java.io.File;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
    
	public ModelRepositoryPreferencePage() {
		setPreferenceStore(ModelRepositoryPlugin.INSTANCE.getPreferenceStore());
	}
	
    @Override
    protected Control createContents(Composite parent) {
        // Help
        PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, HELP_ID);

        Composite client = new Composite(parent, SWT.NULL);
        client.setLayout(new GridLayout());
        
        // User details
        Group userDetailsGroup = new Group(client, SWT.NULL);
        userDetailsGroup.setText("User Details");
        userDetailsGroup.setLayout(new GridLayout(2, false));
        userDetailsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        
        Label label = new Label(userDetailsGroup, SWT.NULL);
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
        
        // Repo folder location
        Group settingsGroup = new Group(client, SWT.NULL);
        settingsGroup.setText("Settings");
        settingsGroup.setLayout(new GridLayout(3, false));
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.widthHint = 500;
        settingsGroup.setLayoutData(gd);
        
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
    }
    
    @Override
    public boolean performOk() {
        getPreferenceStore().setValue(PREFS_COMMIT_USER_NAME, fUserNameTextField.getText());
        getPreferenceStore().setValue(PREFS_COMMIT_USER_EMAIL, fUserEmailTextField.getText());
        getPreferenceStore().setValue(PREFS_REPOSITORY_FOLDER, fUserRepoFolderTextField.getText());
        
        return true;
    }
    
    @Override
    protected void performDefaults() {
        fUserNameTextField.setText(getPreferenceStore().getDefaultString(PREFS_COMMIT_USER_NAME));
        fUserEmailTextField.setText(getPreferenceStore().getDefaultString(PREFS_COMMIT_USER_EMAIL));
        fUserRepoFolderTextField.setText(getPreferenceStore().getDefaultString(PREFS_REPOSITORY_FOLDER));
    }
    
    public void init(IWorkbench workbench) {
    }
}