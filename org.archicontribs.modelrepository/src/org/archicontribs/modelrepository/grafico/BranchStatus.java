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
import java.util.stream.Collectors;

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
     * @return All branches
     */
    public List<BranchInfo> getAllBranches() {
        return new ArrayList<BranchInfo>(infos.values());
    }

    /**
     * @return A union of local branches and remote branches that we are not tracking
     */
    public List<BranchInfo> getLocalAndUntrackedRemoteBranches() {
        return infos.values().stream()
                .filter(info -> info.isLocal()                         // All local branches or
                        || (info.isRemote() && !info.hasLocalRef() ))  // All remote branches that don't have a local ref
                .collect(Collectors.toList());
    }
    
    /**
     * @return All local branches
     */
    public List<BranchInfo> getLocalBranches() {
        return infos.values().stream()
                .filter(info -> info.isLocal())
                .collect(Collectors.toList());
    }
    
    /**
     * @return All remote branches
     */
    public List<BranchInfo> getRemoteBranches() {
        return infos.values().stream()
                .filter(info -> info.isRemote())
                .collect(Collectors.toList());
    }
    
    public BranchInfo getCurrentLocalBranch() {
        return currentLocalBranch;
    }
    
    public BranchInfo getCurrentRemoteBranch() {
        return currentRemoteBranch;
    }
}
