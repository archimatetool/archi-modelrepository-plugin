/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.IOException;

import org.eclipse.jgit.lib.BranchConfig;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

/**
 * BranchInfo
 * 
 * @author Phillip Beauvoir
 */
public class BranchInfo {
    
    private Ref ref;
    private String shortName;
    private boolean isPublished;
    private boolean isDeleted;
    private boolean isCurrentBranch;
    private boolean hasTrackedRef;
    
    BranchInfo(Repository repository, Ref ref) throws IOException {
        this.ref = ref;
        
        isPublished = getIsPublished(repository);
        hasTrackedRef = getHasTrackedRef(repository);
        isDeleted = getIsDeleted(repository);
        isCurrentBranch = getIsCurrentBranch(repository);
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
        return getFullName().startsWith(BranchStatus.localPrefix);
    }

    public boolean isRemote() {
        return getFullName().startsWith(BranchStatus.remotePrefix);
    }

    public boolean isPublished() {
        return isPublished;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public boolean isCurrentBranch() {
        return isCurrentBranch;
    }
    
    public boolean hasTrackedRef() {
        return hasTrackedRef;
    }
    
    public String getRemoteBranchNameFor() {
        return BranchStatus.remotePrefix + getShortName();
    }
    
    public String getLocalBranchNameFor() {
        return BranchStatus.localPrefix + getShortName();
    }

    private boolean getHasTrackedRef(Repository repository) throws IOException {
        Ref ref = null;
        if(isLocal()) {
            ref = repository.findRef(getRemoteBranchNameFor());
        }
        else {
            ref = repository.findRef(getLocalBranchNameFor());
        }
        return ref != null;
    }
    
    private boolean getIsPublished(Repository repository) throws IOException {
        return repository.findRef(BranchStatus.remotePrefix + getShortName()) != null;
    }

    /*
     * "Deleted" means
     * 1. It has a local branch ref
     * 2. It does not have a remote branch ref
     * 3. It is being tracked
     */
    private boolean getIsDeleted(Repository repository) throws IOException {
        if(isRemote()) {
            return false;
        }
        
        // Is it being tracked?
        BranchConfig branchConfig = new BranchConfig(repository.getConfig(), getShortName());
        boolean isBeingTracked = branchConfig.getRemoteTrackingBranch() != null;
        
        // Does it have a remote ref
        boolean hasNoRemoteBranchFor = repository.findRef(getRemoteBranchNameFor()) == null;
        
        return isBeingTracked && hasNoRemoteBranchFor;
    }

    private boolean getIsCurrentBranch(Repository repository) throws IOException {
        return getFullName().equals(repository.getFullBranch());
    }
    
    private String getShortName(String branchName) {
        int index = branchName.lastIndexOf("/"); //$NON-NLS-1$
        if(index != -1 && branchName.length() > index) {
            return branchName.substring(index + 1);
        }
        
        return branchName;
    }
}