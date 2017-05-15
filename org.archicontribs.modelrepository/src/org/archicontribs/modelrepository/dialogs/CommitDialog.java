/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.UIUtils;
import com.archimatetool.editor.ui.components.ExtendedTitleAreaDialog;

/**
 * Commit (Save) Dialog
 * 
 * @author Phil Beauvoir
 */
public class CommitDialog extends ExtendedTitleAreaDialog {
    
    private static String DIALOG_ID = "CommitDialog"; //$NON-NLS-1$
    
    private Text fTextUserName, fTextUserEmail, fTextCommitMessage;
    
    private String fUserName, fUserEmail, fCommitMessage;
    
    public CommitDialog(Shell parentShell) {
        super(parentShell, DIALOG_ID);
        setTitle("Commit");
    }

    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Commit");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("Please enter user details and a commit message", IMessageProvider.INFORMATION);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_NEW_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(2, false);
        container.setLayout(layout);
        
        // User name & email
        String userName = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_NAME);
        String userEmail = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_EMAIL);
        
        Label label = new Label(container, SWT.NONE);
        label.setText("User name:");
        
        fTextUserName = new Text(container, SWT.BORDER);
        fTextUserName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fTextUserName.setText(userName);
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fTextUserName);
        
        label = new Label(container, SWT.NONE);
        label.setText("User email:");
        
        fTextUserEmail = new Text(container, SWT.BORDER);
        fTextUserEmail.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        fTextUserEmail.setText(userEmail);
        // Single text control so strip CRLFs
        UIUtils.conformSingleTextControl(fTextUserEmail);
        
        new Label(container, SWT.NONE);
        new Label(container, SWT.NONE);
        
        label = new Label(container, SWT.NONE);
        label.setText("Commit message:");
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        gd.horizontalSpan = 2;
        label.setLayoutData(gd);
        
        fTextCommitMessage = new Text(container, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP | SWT.MULTI);
        gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        fTextCommitMessage.setLayoutData(gd);
        
        return area;
    }

    @Override
    protected boolean isResizable() {
        return true;
    }
    
    @Override
    protected Point getDefaultDialogSize() {
        return new Point(600, 450);
    }
    
    public String getUserName() {
        return fUserName;
    }

    public String getUserEmail() {
        return fUserEmail;
    }

    public String getCommitMessage() {
        return fCommitMessage;
    }

    protected void okPressed() {
        fUserEmail = fTextUserEmail.getText();
        fUserName = fTextUserName.getText();
        fCommitMessage = fTextCommitMessage.getText();
        
        super.okPressed();
    }

}