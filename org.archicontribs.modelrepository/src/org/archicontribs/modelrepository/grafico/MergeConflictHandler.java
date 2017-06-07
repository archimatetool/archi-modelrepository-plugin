/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.dialogs.ConflictsDialog;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.widgets.Shell;

/**
 * Handle Merge Conflicts on a MergeResult
 * 
 * @author Phillip Beauvoir
 */
public class MergeConflictHandler {
    
    private File fLocalGitFolder;
    private MergeResult fMergeResult;
    private Shell fShell;
    
    private List<String> fOurs;
    private List<String> fTheirs;

    public MergeConflictHandler(MergeResult mergeResult, File localGitFolder, Shell shell) {
        fMergeResult = mergeResult;
        fLocalGitFolder = localGitFolder;
        fShell = shell;
    }
    
    public boolean checkForMergeConflicts() throws IOException {
        // This could be null if Rebase is the default behaviour on the repo rather than merge when a Pull is done
        if(fMergeResult == null) {
            throw new IOException("MergeResult was null"); //$NON-NLS-1$
        }
        
        ConflictsDialog dialog = new ConflictsDialog(fShell, this);
        return dialog.open() == Window.OK ? true : false;
    }
    
    public MergeResult getMergeResult() {
        return fMergeResult;
    }
    
    public String getConflictsAsString() {
        Map<String, int[][]> allConflicts = fMergeResult.getConflicts();
        
        String message = ""; //$NON-NLS-1$
        
        for(String path : allConflicts.keySet()) {
            message += "File: " + path + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
            
            int[][] c = allConflicts.get(path);
            for(int i = 0; i < c.length; ++i) {
                message += "  Conflict #" + (i + 1) + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
                for(int j = 0; j < (c[i].length) - 1; ++j) {
                    if(c[i][j] >= 0) {
                        message += "   Chunk for " + //$NON-NLS-1$
                        fMergeResult.getMergedCommits()[j] +
                        " starts on line #" + //$NON-NLS-1$
                        c[i][j] + "\n"; //$NON-NLS-1$
                    }
                }
            }
            
            message += "\n\n"; //$NON-NLS-1$
        }
        
        return message;
    }
    
    public void mergeAndCommit(String commitMessage) throws IOException, GitAPIException {
        try(Git git = Git.open(fLocalGitFolder)) {
            if(fOurs != null && !fOurs.isEmpty()) {
                checkout(git, Stage.OURS, fOurs);
            }
            if(fTheirs != null  && !fTheirs.isEmpty()) {
                checkout(git, Stage.THEIRS, fTheirs);
            }
            
            // Add to index all files
            AddCommand addCommand = git.add();
            addCommand.addFilepattern("."); //$NON-NLS-1$
            addCommand.setUpdate(false);
            addCommand.call();
            
            // Commit
            CommitCommand commitCommand = git.commit();
            String userName = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_NAME);
            String userEmail = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_EMAIL);
            commitCommand.setAuthor(userName, userEmail);
            commitCommand.setMessage(commitMessage);
            commitCommand.call();
        }
    }
    
    // Check out conflicting files either from us or them
    private void checkout(Git git, Stage stage, List<String> paths) throws GitAPIException {
        CheckoutCommand checkoutCommand = git.checkout();
        checkoutCommand.setStage(stage);
        checkoutCommand.addPaths(paths);
        checkoutCommand.call();
    }
    
    public File getLocalGitFolder() {
        return fLocalGitFolder;
    }
    
    public void resetToRemoteState() throws IOException, GitAPIException {
        resetToState("origin/master"); //$NON-NLS-1$
    }
    
    public void resetToLocalState() throws IOException, GitAPIException {
        resetToState("refs/heads/master"); //$NON-NLS-1$
    }
    
    private void resetToState(String ref) throws IOException, GitAPIException {
        // Reset HARD  which will lose all changes
        try(Git git = Git.open(fLocalGitFolder)) {
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef(ref);
            resetCommand.setMode(ResetType.HARD);
            resetCommand.call();
        }
    }

    public void setOursAndTheirs(List<String> ours, List<String> theirs) {
        fOurs = ours;
        fTheirs = theirs;
    }
}
