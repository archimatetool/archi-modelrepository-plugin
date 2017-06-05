/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RevertCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.MultipleParentsNotAllowedException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Revert commits up to a given commit
 */
public class RevertCommitsAction extends RevertCommitAction {
    
    /**
     * @param window
     */
    public RevertCommitsAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REVERT));
        setText(Messages.RevertCommitsAction_0);
        setToolTipText(Messages.RevertCommitsAction_0);
    }

    @Override
    protected RevertCommand doRevertCommand(Git git) throws GitAPIException, IOException {
        RevertCommand revertCommand = git.revert();
        
        try(RevWalk revWalk = new RevWalk(git.getRepository())) {
            // We are interested in the HEAD
            revWalk.markStart(revWalk.parseCommit(git.getRepository().resolve("HEAD"))); //$NON-NLS-1$
            
            for(RevCommit c : revWalk ) {
                if(c.getParentCount() != 1) {
                    throw new MultipleParentsNotAllowedException(NLS.bind(Messages.RevertCommitsAction_1, c.getName()));
                }
                
                if(c.equals(fCommit)) {
                    break;
                }
                revertCommand.include(c);
            }
        }
        
        revertCommand.call();
        
        return revertCommand;
    }
}
