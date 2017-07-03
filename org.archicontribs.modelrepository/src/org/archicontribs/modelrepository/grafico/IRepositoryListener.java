/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;


/**
 * Interface for a repository listener
 * 
 * @author Phillip Beauvoir
 */
public interface IRepositoryListener {
    
    String REPOSITORY_ADDED = "repository_added"; //$NON-NLS-1$
    String REPOSITORY_DELETED = "repository_deleted"; //$NON-NLS-1$
    String HISTORY_CHANGED = "history_changed"; //$NON-NLS-1$

    void repositoryChanged(String eventName, IArchiRepository repository);
    
}
