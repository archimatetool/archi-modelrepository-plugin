/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * Status of Branches
 * 
 * @author Phillip Beauvoir
 */
public class BranchStatus {
    
    public final static String localPrefix = "refs/heads/"; //$NON-NLS-1$
    public final static String remotePrefix = "refs/remotes/origin/"; //$NON-NLS-1$

    public static List<Ref> getLocalBranchRefs(IArchiRepository archiRepo) throws IOException, GitAPIException {
        try(Git git = Git.open(archiRepo.getLocalRepositoryFolder())) {
            return git.branchList().call(); // Local branches
        }
    }
    
    public static List<String> getLocalBranchNames(IArchiRepository archiRepo) throws IOException, GitAPIException {
        List<String> list = new ArrayList<String>();
        
        for(Ref ref : getLocalBranchRefs(archiRepo)) {
            list.add(ref.getName());
        }
        
        return list;
    }
    
    public static List<Ref> getRemoteBranchRefs(IArchiRepository archiRepo) throws IOException, GitAPIException {
        try(Git git = Git.open(archiRepo.getLocalRepositoryFolder())) {
            return git.branchList().setListMode(ListMode.REMOTE).call();
        }
    }
    
    public static String getCurrentBranch(IArchiRepository archiRepo) throws IOException {
        try(Repository repository = Git.open(archiRepo.getLocalRepositoryFolder()).getRepository()) {
            return repository.getFullBranch();
        }
    }
    
    public static boolean isCurrentBranch(IArchiRepository archiRepo, String branchName) throws IOException {
        return branchName.equals(getCurrentBranch(archiRepo));
    }
    
    public static String getShortName(String branchName) {
        int index = branchName.lastIndexOf("/"); //$NON-NLS-1$
        if(index != -1 && branchName.length() > index) {
            return branchName.substring(index + 1);
        }
        
        return branchName;
    }
    
    public static String getRemoteBranchNameFor(String localBranchName) {
        String shortName = getShortName(localBranchName);
        return remotePrefix + shortName;
    }
    
}
