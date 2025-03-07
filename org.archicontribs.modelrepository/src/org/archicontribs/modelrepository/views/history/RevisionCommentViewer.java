/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.history;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;

/**
 * Revision Comment Viewer
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class RevisionCommentViewer {

    private StyledText styledText;
    
    public RevisionCommentViewer(Composite parent) {
        styledText = new StyledText(parent, SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP | SWT.BORDER);
        styledText.setLayoutData(new GridData(GridData.FILL_BOTH));
        styledText.setMargins(5, 5, 5, 5);
        styledText.setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
    }
    
    public void setCommit(RevCommit commit) {
        if(commit == null) {
            styledText.setText("");
            return;
        }
        
        String message = commit.getFullMessage();
        styledText.setText(message);

        // The first line is bold
        int firstLineLength = message.indexOf('\n');
        firstLineLength = firstLineLength == -1 ? message.length() : firstLineLength;

        StyleRange style = new StyleRange();
        style.start = 0;
        style.length = firstLineLength;
        style.fontStyle = SWT.BOLD;
        styledText.setStyleRange(style);
    }
}
