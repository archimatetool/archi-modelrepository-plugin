/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.revwalk.RevWalkUtils;

/**
 * BranchInfo
 * 
 * @author Phillip Beauvoir
 */
public class BranchInfo {
    
    private final static String LOCAL_PREFIX = Constants.R_HEADS;
    private final static String REMOTE_PREFIX = Constants.R_REMOTES + IGraficoConstants.ORIGIN + "/"; //$NON-NLS-1$

    private Ref ref;
    
    private String shortName;
    
    private boolean isRemoteDeleted;
    private boolean isCurrentBranch;
    private boolean hasLocalRef;
    private boolean hasRemoteRef;
    private boolean hasUnpushedCommits;
    private boolean hasRemoteCommits;
    private boolean isMerged;
    
    private RevCommit latestCommit;
    
    private File repoDir; 
    
    BranchInfo(Repository repository, Ref ref) throws IOException, GitAPIException {
        repoDir = repository.getWorkTree();
        init(repository, ref);
    }
    
    /**
     * Initialise this BranchInfo from the Repository and the Ref
     */
    private void init(Repository repository, Ref ref) throws IOException, GitAPIException {
        this.ref = ref.getTarget(); // Important! Get the target in case it's a symbolic Ref
        
        hasLocalRef = getHasLocalRef(repository);
        hasRemoteRef = getHasRemoteRef(repository);

        getCommitStatus(repository);
        
        isRemoteDeleted = getIsRemoteDeleted(repository);
        isCurrentBranch = getIsCurrentBranch(repository);
        
        getRevWalkStatus(repository);
    }
    
    /**
     * Refresh this BranchInfo with updated information
     * @throws IOException
     * @throws GitAPIException
     */
    public void refresh() throws IOException, GitAPIException {
        try(Git git = Git.open(repoDir)) {
            Ref ref = git.getRepository().findRef(getFullName());  // Ref will be a different object with a new Repository instance so renew it
            init(git.getRepository(), ref);
        }
    }
    
    public Ref getRef() {
        return ref;
    }
    
    public String getFullName() {
        return ref.getName();
    }
    
    public String getShortName() {
        if(shortName == null) {
            shortName = getShortName(getFullName());
        }
        return shortName;
    }
    
    public boolean isLocal() {
        return getFullName().startsWith(LOCAL_PREFIX);
    }

    public boolean isRemote() {
        return getFullName().startsWith(REMOTE_PREFIX);
    }

    public boolean hasLocalRef() {
        return hasLocalRef;
    }

    public boolean hasRemoteRef() {
        return hasRemoteRef;
    }

    public boolean isRemoteDeleted() {
        return isRemoteDeleted;
    }

    public boolean isCurrentBranch() {
        return isCurrentBranch;
    }
    
    public String getRemoteBranchNameFor() {
        return REMOTE_PREFIX + getShortName();
    }
    
    public String getLocalBranchNameFor() {
        return LOCAL_PREFIX + getShortName();
    }
    
    public RevCommit getLatestCommit() {
        return latestCommit;
    }

    public boolean hasRemoteCommits() {
        return hasRemoteCommits;
    }
    
    public boolean hasUnpushedCommits() {
        return hasUnpushedCommits;
    }
    
    public boolean isMerged() {
        return isMerged;
    }
    
    public boolean isMasterBranch() {
        return IGraficoConstants.MASTER.equals(getShortName());
    }
    
    private boolean getHasLocalRef(Repository repository) throws IOException {
        return repository.findRef(getLocalBranchNameFor()) != null;
    }

    private boolean getHasRemoteRef(Repository repository) throws IOException {
        return repository.findRef(getRemoteBranchNameFor()) != null;
    }

    /*
     * Figure out whether the remote branch has been deleted
     * 1. We have a local branch ref
     * 2. We are tracking it
     * 3. But it does not have a remote branch ref
     */
    private boolean getIsRemoteDeleted(Repository repository) throws IOException {
        if(isRemote()) {
            return false;
        }
        
        // Is it being tracked?
        BranchConfig branchConfig = new BranchConfig(repository.getConfig(), getShortName());
        boolean isBeingTracked = branchConfig.getRemoteTrackingBranch() != null;
        
        // Does it have a remote ref?
        boolean hasNoRemoteBranchFor = repository.findRef(getRemoteBranchNameFor()) == null;
        
        // Is being tracked but no remote ref
        return isBeingTracked && hasNoRemoteBranchFor;
    }

    private boolean getIsCurrentBranch(Repository repository) throws IOException {
        return getFullName().equals(repository.getFullBranch());
    }
    
    private String getShortName(String branchName) {
        if(branchName.startsWith(LOCAL_PREFIX)) {
            return branchName.substring(LOCAL_PREFIX.length());
        }
        
        if(branchName.startsWith(REMOTE_PREFIX)) {
            return branchName.substring(REMOTE_PREFIX.length());
        }
        
        return branchName;
    }
    
    /**
     * Get status of this branch from a RevWalk
     * This will get the latest commit for this branch
     * and whether this branch is merged into another
     */
    private void getRevWalkStatus(Repository repository) throws GitAPIException, IOException {
        try(RevWalk revWalk = new RevWalk(repository)) {
            // Get the latest commit for this branch
            latestCommit = revWalk.parseCommit(ref.getObjectId());
            
            // If this is the master branch isMerged is true
            if(isMasterBranch()) {
                isMerged = true;
            }
            // Else this is another branch
            else {
                // Get all other branch refs
            	// Setting ListMode to REMOTE lists only remote branches while ALL lists remote and local branches
                List<Ref> otherRefs = Git.wrap(repository).branchList().setListMode(ListMode.ALL).call();
                otherRefs.remove(ref); // remove this one
                
                // In-built method
                List<Ref> refs = RevWalkUtils.findBranchesReachableFrom(latestCommit, revWalk, otherRefs);
                isMerged = !refs.isEmpty(); // If there are other reachable branches then this is merged
                
                /* Another method to do this...
                for(Ref otherRef : otherRefs) {
                    // Get the other branch's latest commit
                    RevCommit otherHead = revWalk.parseCommit(otherRef.getObjectId());

                    // If this head is an ancestor of, or the same as, the other head then this is merged
                    if(revWalk.isMergedInto(latestCommit, otherHead)) {
                        isMerged = true;
                        break;
                    }
                } */
            }
            
            revWalk.dispose();
        }
    }
    
    private void getCommitStatus(Repository repository) throws IOException {
        BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(repository, getShortName());
        if(trackingStatus != null) {
            hasUnpushedCommits = trackingStatus.getAheadCount() > 0;
            hasRemoteCommits = trackingStatus.getBehindCount() > 0;
        }
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof BranchInfo) &&
                repoDir.equals(((BranchInfo)obj).repoDir) &&
                getFullName().equals(((BranchInfo)obj).getFullName());
    }
}