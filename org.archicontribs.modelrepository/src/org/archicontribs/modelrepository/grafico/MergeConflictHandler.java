/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.archicontribs.modelrepository.dialogs.ConflictsDialog;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CheckoutCommand;
import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.widgets.Shell;

/**
 * Handle Merge Conflicts on a PullResult
 * 
 * @author Phillip Beauvoir
 */
public class MergeConflictHandler {
    
    private File fLocalGitFolder;
    private MergeResult fMergeResult;
    private Shell fShell;
    private PullResult fPullRequest;

    public MergeConflictHandler(PullResult pullResult, File localGitFolder, Shell shell) {
        fPullRequest = pullResult;
        fLocalGitFolder = localGitFolder;
        fShell = shell;
    }
    
    public void checkForMergeConflicts() throws IOException, GitAPIException {
        fMergeResult = fPullRequest.getMergeResult();
        
        // This could be null if Rebase is set rather than merge on Pull
        if(fMergeResult == null) {
            throw new IOException("MergeResult was null"); //$NON-NLS-1$
        }
        
        ConflictsDialog dialog = new ConflictsDialog(fShell, this);
        dialog.open();
    }
    
    public String getConflictsAsString() {
        Map<String, int[][]> allConflicts = fMergeResult.getConflicts();
        
        String message = ""; //$NON-NLS-1$
        
        for(String path : allConflicts.keySet()) {
            int[][] c = allConflicts.get(path);
            message = "File: " + path + "\n\n"; //$NON-NLS-1$ //$NON-NLS-2$
            for(int i = 0; i < c.length; ++i) {
                message += "Conflict #" + i + "\n"; //$NON-NLS-1$ //$NON-NLS-2$
                for(int j = 0; j < (c[i].length) - 1; ++j) {
                    if(c[i][j] >= 0) {
                        message += "  Chunk for " + //$NON-NLS-1$
                        fMergeResult.getMergedCommits()[j] +
                        " starts on line #" + //$NON-NLS-1$
                        c[i][j] + "\n"; //$NON-NLS-1$
                    }
                }
            }
        }
        
        return message;
    }

    public void mergeAndCommitToTheirs() throws IOException, GitAPIException {
        mergeAndCommit(Stage.THEIRS);
    }
    
    public void mergeAndCommitToOurs() throws IOException, GitAPIException {
        mergeAndCommit(Stage.OURS);
    }
    
    void mergeAndCommit(Stage stage) throws IOException, GitAPIException {
        Git git = Git.open(fLocalGitFolder);
        
        try {
            CheckoutCommand checkoutCommand = git.checkout();
            checkoutCommand.setStage(stage);
            Map<String, int[][]> allConflicts = fMergeResult.getConflicts();
            checkoutCommand.addPaths(new ArrayList<String>( allConflicts.keySet()));
            checkoutCommand.call();
            
            AddCommand addCommand = git.add();
            addCommand.addFilepattern("."); //$NON-NLS-1$
            addCommand.setUpdate(false);
            addCommand.call();
            
            CommitCommand commitCommand = git.commit();
            commitCommand.call();
        }
        finally {
            git.close();
        }
    }
}
