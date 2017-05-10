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
 * Clone Input Dialog
 * 
 * @author Jean-Baptiste Sarrodie
 */
public class CloneInputDialog extends TitleAreaDialog {

	private Text txtURL;
    private Text txtUsername;
    private Text txtPassword;

    private String URL;
    private String username;
    private String password;

    public CloneInputDialog(Shell parentShell) {
        super(parentShell);
        setTitle(Messages.CloneInputDialog_0);
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.CloneInputDialog_0);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.CloneInputDialog_1, IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        createURL(container);
        createUsername(container);
        createPassword(container);

        return area;
    }

    private void createURL(Composite container) {
        Label lbt = new Label(container, SWT.NONE);
        lbt.setText(Messages.CloneInputDialog_2);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;

        txtURL = new Text(container, SWT.BORDER);
        txtURL.setLayoutData(data);
    }
    
    private void createUsername(Composite container) {
        Label lbt = new Label(container, SWT.NONE);
        lbt.setText(Messages.CloneInputDialog_3);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;

        txtUsername = new Text(container, SWT.BORDER);
        txtUsername.setLayoutData(data);
    }

    private void createPassword(Composite container) {
        Label lbt = new Label(container, SWT.NONE);
        lbt.setText(Messages.CloneInputDialog_4);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;
        txtPassword = new Text(container, SWT.BORDER | SWT.PASSWORD);
        txtPassword.setLayoutData(data);
    }

    @Override
    protected boolean isResizable() {
        return true;
    }

    // save content of the Text fields because they get disposed
    // as soon as the Dialog closes
    private void saveInput() {
        username = txtUsername.getText();
        password = txtPassword.getText();
        URL = txtURL.getText();
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