/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.IArchiImages;

/**
 * New Model Repo Dialog
 * 
 * @author Phillip Beauvoir
 */
public class NewModelRepoDialog extends TitleAreaDialog {

	private Text txtURL;
    private Text txtUsername;
    private Text txtPassword;

    private String URL;
    private String username;
    private String password;

    public NewModelRepoDialog(Shell parentShell) {
        super(parentShell);
        setTitle(Messages.NewModelRepoDialog_0);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.NewModelRepoDialog_0);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.NewModelRepoDialog_1, IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        txtURL = createTextField(container, Messages.NewModelRepoDialog_2, SWT.NONE);
        txtUsername = createTextField(container, Messages.NewModelRepoDialog_4, SWT.NONE);
        txtPassword = createTextField(container, Messages.NewModelRepoDialog_5, SWT.PASSWORD);

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

    // save content of the Text fields because they get disposed
    // as soon as the Dialog closes
    private void saveInput() {
        username = txtUsername.getText().trim();
        password = txtPassword.getText().trim();
        URL = txtURL.getText().trim();
    }

    @Override
    protected void okPressed() {
        saveInput();
        super.okPressed();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
    
    public String getURL() {
        return URL;
    }
}