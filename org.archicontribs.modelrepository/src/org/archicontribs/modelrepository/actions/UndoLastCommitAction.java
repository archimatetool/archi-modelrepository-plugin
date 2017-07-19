/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Undo the last commit
 * 1. If the last commit has been pushed then you're outta luck, boy.
 * 2. Have you saved your goddam stuff!!!???
 * 3. Export to Grafico
 * 4. Wait! There's stuff that needs committing. Are you sure? If you proceed, you'll lose your stuff, dude.
 * 5. Oh, OK then....blat!
 */
public class UndoLastCommitAction extends AbstractModelAction {
    
    public UndoLastCommitAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_UNDO_COMMIT));
        setText(Messages.UndoLastCommitAction_0);
        setToolTipText(Messages.UndoLastCommitAction_0);
    }

    @Override
    public void run() {
        // If the latest commit (i.e. local and remote head are the same) has already been pushed we can't roll back to the previous commit
        try {
            if(isHeadAndRemoteSame()) {
                MessageDialog.openError(fWindow.getShell(),
                        Messages.UndoLastCommitAction_0,
                        Messages.UndoLastCommitAction_3);
                return;
            }
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
        }
        
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        try {
            getRepository().exportModelToGraficoFiles();
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
            return;
        }
        
        int choice = new MessageDialog(fWindow.getShell(),
                Messages.UndoLastCommitAction_0,
                null,
                Messages.UndoLastCommitAction_2,
                MessageDialog.QUESTION,
                new String[] {Messages.UndoLastCommitAction_5,
                        Messages.UndoLastCommitAction_6,
                        Messages.UndoLastCommitAction_7},
                1).open();

        if(choice == 2) {
            return;
        }
        
        // Do it!
        try {
            reset("HEAD^"); //$NON-NLS-1$
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
        }
        
        try {
            // Reload the model from the Grafico XML files if we are rolling back
            if(choice == 0) {
                new GraficoModelLoader(getRepository()).loadModel();
            }
            // Export to grafico if just undoing last commit
            else {
                getRepository().exportModelToGraficoFiles();
            }
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.UndoLastCommitAction_0, ex);
        }
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
    }
    
    /**
     * @return true if the latest local HEAD commit and the remote commit are the same
     * @throws IOException
     */
    protected boolean isHeadAndRemoteSame() throws IOException {
        try(Repository repository = Git.open(getRepository().getLocalRepositoryFolder()).getRepository()) {
            Ref online = repository.findRef("origin/master"); //$NON-NLS-1$
            Ref local = repository.findRef("HEAD"); //$NON-NLS-1$
            
            try(RevWalk revWalk = new RevWalk(repository)) {
                RevCommit onlineCommit = revWalk.parseCommit(online.getObjectId());
                RevCommit localLatestCommit = revWalk.parseCommit(local.getObjectId());
                revWalk.dispose();
                return onlineCommit.equals(localLatestCommit);
            }
        }
    }
    
    /**
     * Reset to ref state HARD
     * @param ref
     * @throws IOException
     * @throws GitAPIException
     */
    protected void reset(String ref) throws IOException, GitAPIException {
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef(ref);
            resetCommand.setMode(ResetType.HARD); // And do it hard, boy!
            resetCommand.call();
        }
    }
}
