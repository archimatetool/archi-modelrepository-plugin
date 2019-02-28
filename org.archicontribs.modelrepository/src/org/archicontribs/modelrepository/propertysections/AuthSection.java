/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PreferencesUtil;

import com.archimatetool.editor.propertysections.AbstractArchiPropertySection;
import com.archimatetool.editor.utils.StringUtils;


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
    
    private TextListener textUserName, textPassword;
    
    private class TextListener {
        Text text;
        String oldValue;

        Listener listener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                switch(event.type) {
                    case SWT.FocusOut:
                    case SWT.DefaultSelection:
                        String newValue = text.getText();
                        
                        // Different value so save
                        if(!oldValue.equals(newValue)) {
                            oldValue = newValue;
                            // Save
                            saveCredentials();
                            clearButton.setEnabled(true);
                        }
                        break;
                    
                    default:
                        break;
                }
             }
        };
        
        TextListener(Composite parent, int style) {
            text = createSingleTextControl(parent, style);
            text.addListener(SWT.DefaultSelection, listener);
            text.addListener(SWT.FocusOut, listener);
        }

        void setText(String oldValue) {
            this.oldValue = oldValue;
            text.setText(oldValue);
        }
        
        String getText() {
            return text.getText().trim();
        }

        void setEnabled(boolean enabled) {
            text.setEnabled(enabled);
        }
    }
    
    public AuthSection() {
    }

    @Override
    protected void createControls(Composite parent) {
        Group group1 = getWidgetFactory().createGroup(parent, Messages.AuthSection_0);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        group1.setLayoutData(gd);
        group1.setLayout(new GridLayout(2, false));
        
        createLabel(group1, Messages.AuthSection_6, STANDARD_LABEL_WIDTH, SWT.CENTER);
        textUserName = new TextListener(group1, SWT.NONE);
        
        createLabel(group1, Messages.AuthSection_7, STANDARD_LABEL_WIDTH, SWT.CENTER);
        textPassword = new TextListener(group1, SWT.PASSWORD);

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
        SimpleCredentialsStorage scs = getCredentials();
        
        if(scs.hasCredentialsFile()) {
            try {
                UsernamePassword npw = scs.getUsernamePassword();
                textUserName.setText(StringUtils.safeString(npw.getUsername()));
                textPassword.setText(StringUtils.safeString(npw.getPassword()));
            }
            catch(IOException ex) {
                ex.printStackTrace();
                MessageDialog.openError(getPart().getSite().getShell(), Messages.AuthSection_0, ex.getMessage());
            }
        }
        else {
            textUserName.setText(""); //$NON-NLS-1$
            textPassword.setText(""); //$NON-NLS-1$
        }
        
        boolean isHTTP = true;
        try {
            isHTTP = GraficoUtils.isHTTP(fRepository.getOnlineRepositoryURL());
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        textUserName.setEnabled(isHTTP);
        textPassword.setEnabled(isHTTP);
        
        clearButton.setEnabled(scs.hasCredentialsFile());
        prefsButton.setEnabled(!isHTTP);
    }
    
    private SimpleCredentialsStorage getCredentials() {
        return new SimpleCredentialsStorage(new File(fRepository.getLocalGitFolder(),
                IGraficoConstants.REPO_CREDENTIALS_FILE)); 
    }
    
    private void saveCredentials() {
        try {
            getCredentials().store(textUserName.getText(), textPassword.getText());
        }
        catch(NoSuchAlgorithmException | IOException ex) {
            ex.printStackTrace();
            MessageDialog.openError(getPart().getSite().getShell(),
                    Messages.AuthSection_11,
                    Messages.AuthSection_12 +
                            " " + //$NON-NLS-1$
                            ex.getMessage());
        }
    }
    
    @Override
    public void dispose() {
        if(textPassword != null) {
            textPassword.oldValue = null;
        }
    }
}
