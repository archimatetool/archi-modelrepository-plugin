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
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.propertysections.AbstractArchiPropertySection;
import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.utils.StringUtils;


/**
 * Property Section for Authorisation
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
    
    private Button editButton, clearButton;
    
    public AuthSection() {
    }

    @Override
    protected void createControls(Composite parent) {
        Group group = getWidgetFactory().createGroup(parent, Messages.AuthSection_0);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        group.setLayoutData(gd);
        group.setLayout(new GridLayout(2, false));
        
        editButton = getWidgetFactory().createButton(group, Messages.AuthSection_1, SWT.PUSH);
        editButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                new EditAuthDialog(parent.getShell()).open();
            }
        });
        
        clearButton = getWidgetFactory().createButton(group, Messages.AuthSection_2, SWT.PUSH);
        clearButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                boolean answer = MessageDialog.openQuestion(parent.getShell(), Messages.AuthSection_3,
                        Messages.AuthSection_4);
                if(answer) {
                    getCredentials().deleteCredentialsFile();
                    clearButton.setEnabled(false);
                }
            }
        });
    }
    
    @Override
    protected void handleSelection(IStructuredSelection selection) {
        if(selection.getFirstElement() instanceof IArchiRepository) {
            fRepository = (IArchiRepository)selection.getFirstElement();
            clearButton.setEnabled(getCredentials().hasCredentialsFile());
        }
        else {
            System.err.println(getClass() + " failed to get element for " + selection.getFirstElement()); //$NON-NLS-1$
        }
    }
    
    private SimpleCredentialsStorage getCredentials() {
        return new SimpleCredentialsStorage(new File(fRepository.getLocalGitFolder(),
                IGraficoConstants.REPO_CREDENTIALS_FILE)); 
    }
    
    private class EditAuthDialog extends TitleAreaDialog {
        private Text txtUsername;
        private Text txtPassword;
        
        public EditAuthDialog(Shell parentShell) {
            super(parentShell);
            setTitle(Messages.AuthSection_5);
        }

        @Override
        protected void configureShell(Shell shell) {
            super.configureShell(shell);
            shell.setText(Messages.AuthSection_5);
        }

        @Override
        protected Control createDialogArea(Composite parent) {
            setMessage(Messages.AuthSection_7, IMessageProvider.INFORMATION);
            setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

            Composite area = (Composite) super.createDialogArea(parent);
            Composite container = new Composite(area, SWT.NONE);
            container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
            GridLayout layout = new GridLayout(2, false);
            container.setLayout(layout);

            txtUsername = createTextField(container, Messages.AuthSection_8, SWT.NONE);
            txtPassword = createTextField(container, Messages.AuthSection_9, SWT.PASSWORD);

            try {
                SimpleCredentialsStorage creds = getCredentials();
                txtUsername.setText(StringUtils.safeString(creds.getUsername()));
                txtPassword.setText(StringUtils.safeString(creds.getPassword()));
            }
            catch(IOException ex) {
                ex.printStackTrace();
                MessageDialog.openError(getShell(), Messages.AuthSection_5, ex.getMessage());
            }

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
        protected boolean isResizable() {
            return true;
        }

        private void saveInput() {
            try {
                getCredentials().store(txtUsername.getText().trim(), txtPassword.getText().trim());
            }
            catch(NoSuchAlgorithmException | IOException ex) {
                ex.printStackTrace();
                MessageDialog.openError(getShell(),
                        Messages.AuthSection_11,
                        Messages.AuthSection_12 +
                                " " + //$NON-NLS-1$
                                ex.getMessage());
            }
        }

        @Override
        protected void okPressed() {
            saveInput();
            super.okPressed();
        }
    }
}
