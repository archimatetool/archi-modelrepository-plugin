/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    private Map<String, BranchInfo> infos = new HashMap<String, BranchInfo>();
    
    private BranchInfo currentLocalBranch;
    private BranchInfo currentRemoteBranch;
    
    BranchStatus(IArchiRepository archiRepo) throws IOException, GitAPIException {
        try(Git git = Git.open(archiRepo.getLocalRepositoryFolder())) {
            Repository repository = git.getRepository();

            // Get all known branches
            for(Ref ref : git.branchList().setListMode(ListMode.ALL).call()) {
                BranchInfo info = new BranchInfo(repository, ref);
                infos.put(info.getFullName(), info);
            }
            
            // Get current local branch
            String head = repository.getFullBranch();
            if(head != null) {
                currentLocalBranch = infos.get(head);
            }
            
            // Get current remote branch
            if(currentLocalBranch != null) {
                String remoteName = currentLocalBranch.getRemoteBranchNameFor();
                if(remoteName != null) {
                    currentRemoteBranch = infos.get(remoteName);
                }
            }
        }
    }
    
    /**
     * @return A union of local branches and remote branches that we are not tracking
     */
    public List<BranchInfo> getLocalAndUntrackedRemoteBranches() {
        List<BranchInfo> list = new ArrayList<BranchInfo>();
        
        for(BranchInfo branch : infos.values()) {
            // All local branches
            if(branch.isLocal()) {
                list.add(branch);
            }
            // All remote branches that don't have a local ref
            else if(branch.isRemote() && !branch.hasLocalRef()) {
                list.add(branch);
            }
        }
        
        return list;
    }
    
    /**
     * @return All local branches
     */
    public List<BranchInfo> getLocalBranches() {
        List<BranchInfo> list = new ArrayList<BranchInfo>();
        
        for(BranchInfo branch : infos.values()) {
            if(branch.isLocal()) {
                list.add(branch);
            }
        }
        
        return list;
    }
    
    /**
     * @return All remote branches
     */
    public List<BranchInfo> getRemoteBranches() {
        List<BranchInfo> list = new ArrayList<BranchInfo>();
        
        for(BranchInfo branch : infos.values()) {
            if(branch.isRemote()) {
                list.add(branch);
            }
        }
        
        return list;
    }
    
    public BranchInfo getCurrentLocalBranch() {
        return currentLocalBranch;
    }
    
    public BranchInfo getCurrentRemoteBranch() {
        return currentRemoteBranch;
    }
}
