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
public class RevisionCommentViewer {

    private StyledText fText;
    
    public RevisionCommentViewer(Composite parent) {
        fText = new StyledText(parent, SWT.V_SCROLL | SWT.READ_ONLY | SWT.WRAP | SWT.BORDER);
        fText.setLayoutData(new GridData(GridData.FILL_BOTH));
        fText.setMargins(5, 5, 5, 5);
        fText.setFont(JFaceResources.getFontRegistry().get(JFaceResources.TEXT_FONT));
    }
    
    public void setCommit(RevCommit commit) {
        if(commit != null) {
            String shortMessage = commit.getShortMessage();
            String fullMessage = commit.getFullMessage();
            
            fText.setText(fullMessage);
            
            StyleRange style = new StyleRange();
            style.start = 0;
            style.length = shortMessage.length();
            style.fontStyle = SWT.BOLD;
            fText.setStyleRange(style);
        }
    }
}
