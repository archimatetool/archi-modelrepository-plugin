/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;

import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jgit.api.errors.GitAPIException;
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
import com.archimatetool.editor.ui.components.ExtendedTitleAreaDialog;

/**
 * Conflicts Dialog
 * 
 * @author Phil Beauvoir
 */
public class ConflictsDialog extends ExtendedTitleAreaDialog {
    
    private static String DIALOG_ID = "ConflictsDialog"; //$NON-NLS-1$

    private MergeConflictHandler fHandler;

    public ConflictsDialog(Shell parentShell, MergeConflictHandler handler) {
        super(parentShell, DIALOG_ID);
        setTitle("Conflicts");
        
        fHandler = handler;
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Conflicts");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("There are Conflicts", IMessageProvider.ERROR);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_IMPORT_PREF_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);
        
        displayConflicts(container);

        return area;
    }
    
    private void displayConflicts(Composite container) {
        Text messageText = new Text(container, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
        messageText.setText(fHandler.getConflictsAsString());
        messageText.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        Label label = new Label(container, SWT.NONE);
        label.setText("\n\nPress OK to accept online changes and lose local changes.\nPress Cancel to revert to local version.");
    }

    @Override
    protected void okPressed() {
        try {
            fHandler.resetToRemoteState();
        }
        catch(IOException | GitAPIException ex) {
            ex.printStackTrace();
        }
        super.okPressed();
    }
    
    @Override
    protected void cancelPressed() {
        try {
            fHandler.resetToLocalState();
        }
        catch(IOException | GitAPIException ex) {
            ex.printStackTrace();
        }
        super.cancelPressed();
    }

    @Override
    protected Point getDefaultDialogSize() {
        return new Point(500, 350);
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }

}