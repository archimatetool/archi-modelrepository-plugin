/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;

import org.eclipse.jface.action.IAction;

/**
 * Interface for Actions
 * 
 * @author Phillip Beauvoir
 */
public interface IGraficoModelAction extends IAction {

    /**
     * Set the local repository folder
     * @param folder
     */
    void setLocalRepositoryFolder(File folder);

    /**
     * @return The local repository folder
     */
    File getLocalRepositoryFolder();

    /**
     * @return The local git folder
     */
    File getLocalGitFolder();

}