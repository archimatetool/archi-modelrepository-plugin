/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

/**
 * Ask user for Primary Password
 * 
 * @author Phillip Beauvoir
 */
public class PrimaryPasswordDialog extends InputDialog {
    
    private static IInputValidator validator = newText -> {
        return newText.length() < 1 ? "" : null; //$NON-NLS-1$
    };

    public PrimaryPasswordDialog(Shell parentShell) {
        super(parentShell, Messages.PrimaryPasswordDialog_0, Messages.PrimaryPasswordDialog_1, null, validator);
    }

    @Override
    protected int getInputTextStyle() {
        return super.getInputTextStyle() | SWT.PASSWORD;
    }
}
