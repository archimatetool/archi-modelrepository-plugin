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
 * User Name and Password Dialog
 * 
 * @author Phil Beauvoir
 */
public class UserNamePasswordDialog extends TitleAreaDialog {

    private Text txtUsername;
    private Text txtPassword;

    private String username;
    private String password;

    public UserNamePasswordDialog(Shell parentShell) {
        super(parentShell);
    }

    @Override
    public void create() {
        super.create();
        setTitle(Messages.UserNamePasswordDialog_0);
        setMessage(Messages.UserNamePasswordDialog_1, IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_PROPERTIES_VIEW_ICON));
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);

        createUsername(container);
        createPassword(container);

        return area;
    }

    private void createUsername(Composite container) {
        Label lbt = new Label(container, SWT.NONE);
        lbt.setText(Messages.UserNamePasswordDialog_2);

        GridData data = new GridData();
        data.grabExcessHorizontalSpace = true;
        data.horizontalAlignment = GridData.FILL;

        txtUsername = new Text(container, SWT.BORDER);
        txtUsername.setLayoutData(data);
    }

    private void createPassword(Composite container) {
        Label lbt = new Label(container, SWT.NONE);
        lbt.setText(Messages.UserNamePasswordDialog_3);

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
}