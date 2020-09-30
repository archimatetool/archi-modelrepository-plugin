/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.authentication.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.preferences.ModelRepositoryPreferencePage;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.archimatetool.editor.propertysections.AbstractArchiPropertySection;


/**
 * Property Section for Authentication
 * 
 * @author Phillip Beauvoir
 */
public class AuthSection extends AbstractArchiPropertySection {
    
    public static class Filter implements IFilter {
        @Override
        public boolean select(Object object) {
            return object instanceof IArchiRepository;
        }
    }
    
    private IArchiRepository fRepository;
    
    private Button clearButton;
    private Button prefsButton;
    
    private UpdatingTextControl textUserName;
    private UpdatingTextControl textPassword;
    
    public AuthSection() {
    }

    @Override
    protected void createControls(Composite parent) {
        Group group1 = getWidgetFactory().createGroup(parent, Messages.AuthSection_0);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        group1.setLayoutData(gd);
        group1.setLayout(new GridLayout(2, false));
        
        // User name
        createLabel(group1, Messages.AuthSection_6, STANDARD_LABEL_WIDTH, SWT.CENTER);
        textUserName = new UpdatingTextControl(createSingleTextControl(group1, SWT.NONE)) {
            @Override
            protected void textChanged(String newText) {
                storeUserName(newText);
            }
        };
        
        // Password
        createLabel(group1, Messages.AuthSection_7, STANDARD_LABEL_WIDTH, SWT.CENTER);
        textPassword = new UpdatingTextControl(createSingleTextControl(group1, SWT.PASSWORD)) {
            @Override
            protected void textChanged(String newText) {
                setNotifications(false); // Setting the password might invoke the primary password dialog and cause a focus out event
                storePassword(newText);
                setNotifications(true);
            }
        };

        // Clear Credentials
        clearButton = getWidgetFactory().createButton(group1, Messages.AuthSection_2, SWT.PUSH);
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean answer = MessageDialog.openQuestion(parent.getShell(), Messages.AuthSection_3,
                        Messages.AuthSection_4);
                if(answer) {
                    getCredentials().deleteCredentialsFile();
                    updateControls();
                }
            }
        });
        
        // SSH Preferences
        prefsButton = getWidgetFactory().createButton(group1, Messages.AuthSection_8, SWT.PUSH);
        prefsButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(getPart().getSite().getShell(),
                        ModelRepositoryPreferencePage.ID, null, null);
                if(dialog != null) {
                    dialog.open();
                }
            }
        });
    }
    
    @Override
    protected void handleSelection(IStructuredSelection selection) {
        if(selection.getFirstElement() instanceof IArchiRepository) {
            fRepository = (IArchiRepository)selection.getFirstElement();
            updateControls();
        }
        else {
            System.err.println(getClass() + " failed to get element for " + selection.getFirstElement()); //$NON-NLS-1$
        }
    }
    
    private void updateControls() {
        boolean isHTTP = true;
        
        try {
            isHTTP = GraficoUtils.isHTTP(fRepository.getOnlineRepositoryURL());
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        EncryptedCredentialsStorage credentials = getCredentials();
        
        if(isHTTP && credentials.hasCredentialsFile()) {
            try {
                textUserName.setText(credentials.getUserName());
            }
            catch(IOException ex) {
                showError(ex);
            }
            
            textPassword.setText("********"); //$NON-NLS-1$
        }
        else {
            textUserName.setText(""); //$NON-NLS-1$
            textPassword.setText(""); //$NON-NLS-1$
        }
        
        textUserName.setEnabled(isHTTP);
        textPassword.setEnabled(isHTTP);
        
        clearButton.setEnabled(credentials.hasCredentialsFile());
        prefsButton.setEnabled(!isHTTP);
    }
    
    private EncryptedCredentialsStorage getCredentials() {
        return EncryptedCredentialsStorage.forRepository(fRepository); 
    }
    
    private void storeUserName(String userName) {
        try {
            getCredentials().storeUserName(userName);
        }
        catch(IOException ex) {
            showError(ex);
        }
    }
    
    private void storePassword(String password) {
        try {
            getCredentials().storePassword(password);
        }
        catch(IOException | GeneralSecurityException ex) {
            showError(ex);
        }
    }
    
    private void showError(Exception ex) {
        ex.printStackTrace();
        MessageDialog.openError(Display.getCurrent().getActiveShell(),
                Messages.AuthSection_11,
                Messages.AuthSection_12 +
                        " " + //$NON-NLS-1$
                        ex.getMessage());
    }
}
