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
import org.eclipse.jgit.lib.BranchConfig;
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
    
    public static List<String> getUnionOfBranches(IArchiRepository archiRepo) throws IOException, GitAPIException {
        List<String> list = new ArrayList<String>();
        List<String> localBranches = getLocalBranchNames(archiRepo);
        
        for(String branch : getAllBranchNames(archiRepo)) {
            if(isLocalBranch(branch)) {
                list.add(branch);
            }
            else if(isRemoteBranch(branch) && !localBranches.contains(getLocalBranchNameFor(branch))) {
                list.add(branch);
            }
        }
        
        return list;
    }
    
    public static boolean isPublished(IArchiRepository archiRepo, String branchName) throws IOException {
        return archiRepo.hasRef(remotePrefix + getShortName(branchName));
    }
    
    public static boolean isDeleted(IArchiRepository archiRepo, String branchName) throws IOException {
        if(isRemoteBranch(branchName)) {
            return false;
        }
        
        String shortName = getShortName(branchName);
        boolean hasTrackedRef = false;

        try(Repository repository = Git.open(archiRepo.getLocalRepositoryFolder()).getRepository()) {
            BranchConfig branchConfig = new BranchConfig(repository.getConfig(), shortName);
            hasTrackedRef = branchConfig.getRemoteTrackingBranch() != null;
        }
        
        return hasTrackedRef && !hasRemoteBranchFor(archiRepo, branchName);
    }
    
    public static boolean isLocalBranch(String branchName) {
        return branchName != null && branchName.startsWith(localPrefix);
    }
    
    public static boolean isRemoteBranch(String branchName) {
        return branchName != null && branchName.startsWith(remotePrefix);
    }
    
    public static boolean hasRemoteBranchFor(IArchiRepository archiRepo, String localBranchName) throws IOException {
        String remoteBranchName = getRemoteBranchNameFor(localBranchName);
        return archiRepo.hasRef(remoteBranchName);
    }
    
    public static boolean hasLocalBranchFor(IArchiRepository archiRepo, String remoteBranchName) throws IOException {
        String localBranchName = getLocalBranchNameFor(remoteBranchName);
        return archiRepo.hasRef(localBranchName);
    }

    public static List<String> getAllBranchNames(IArchiRepository archiRepo) throws IOException, GitAPIException {
        List<String> list = new ArrayList<String>();
        
        for(Ref ref : getAllBranchRefs(archiRepo)) {
            list.add(ref.getName());
        }
        
        return list;
    }
    
    public static List<String> getLocalBranchNames(IArchiRepository archiRepo) throws IOException, GitAPIException {
        List<String> list = new ArrayList<String>();
        
        for(Ref ref : getLocalBranchRefs(archiRepo)) {
            list.add(ref.getName());
        }
        
        return list;
    }
    
    public static List<Ref> getLocalBranchRefs(IArchiRepository archiRepo) throws IOException, GitAPIException {
        try(Git git = Git.open(archiRepo.getLocalRepositoryFolder())) {
            return git.branchList().call(); // Local branches
        }
    }
    
    public static List<Ref> getAllBranchRefs(IArchiRepository archiRepo) throws IOException, GitAPIException {
        try(Git git = Git.open(archiRepo.getLocalRepositoryFolder())) {
            return git.branchList().setListMode(ListMode.ALL).call();
        }
    }

    public static List<Ref> getRemoteBranchRefs(IArchiRepository archiRepo) throws IOException, GitAPIException {
        try(Git git = Git.open(archiRepo.getLocalRepositoryFolder())) {
            return git.branchList().setListMode(ListMode.REMOTE).call();
        }
    }
    
    public static String getCurrentLocalBranch(IArchiRepository archiRepo) throws IOException {
        try(Repository repository = Git.open(archiRepo.getLocalRepositoryFolder()).getRepository()) {
            return repository.getFullBranch();
        }
    }
    
    public static String getCurrentRemoteBranch(IArchiRepository archiRepo) throws IOException {
        return getRemoteBranchNameFor(getCurrentLocalBranch(archiRepo));
    }
    
    public static String getRemoteBranchNameFor(String branchName) {
        return remotePrefix + getShortName(branchName);
    }
    
    public static String getLocalBranchNameFor(String branchName) {
        return localPrefix + getShortName(branchName);
    }

    public static boolean isCurrentBranch(IArchiRepository archiRepo, String branchName) throws IOException {
        return branchName.equals(getCurrentLocalBranch(archiRepo));
    }
    
    public static String getShortName(String branchName) {
        int index = branchName.lastIndexOf("/"); //$NON-NLS-1$
        if(index != -1 && branchName.length() > index) {
            return branchName.substring(index + 1);
        }
        
        return branchName;
    }
    
}
