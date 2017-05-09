/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.widgets.Shell;

public abstract class AbstractModelAction extends Action {
	
	private File fGitRepoFolder;
	 
	public void setGitRepository(File folder) {
        fGitRepoFolder = folder;
        setEnabled(GraficoUtils.isGitRepository(folder));
	}
	
	public File getGitRepository() {
	    return fGitRepoFolder;
	}
	
	// TEMPORARY!!!!!!!
	protected void checkConflicts(PullResult pullResult, Shell shell) throws IOException, GitAPIException {
        MergeResult mergeResult = pullResult.getMergeResult();
        Map<String, int[][]> allConflicts = mergeResult.getConflicts();
        
        String message = "";
        
        for(String path : allConflicts.keySet()) {
            int[][] c = allConflicts.get(path);
            message = "Conflicts in file " + path + "\n\n";
            for(int i = 0; i < c.length; ++i) {
                message += "  Conflict #" + i + "\n";
                for(int j = 0; j < (c[i].length) - 1; ++j) {
                    if(c[i][j] >= 0) {
                        message += "    Chunk for " + mergeResult.getMergedCommits()[j] + " starts on line #" + c[i][j] + "\n";
                    }
                }
            }
        }
        
        MessageDialog.openError(shell, "Conflicts", message);
        
        // For now, reset HARD
        Git git = Git.open(getGitRepository());
        ResetCommand resetCommand = git.reset();
        resetCommand.setMode(ResetType.HARD);
        resetCommand.call();
        git.close();
	}
}
