/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.eclipse.jface.action.IAction;

/**
 * Interface for Actions
 * 
 * @author Phillip Beauvoir
 */
public interface IGraficoModelAction extends IAction {

    /**
     * Set the repository
     * @param repository
     */
    void setRepository(ArchiRepository repository);
    
    /**
     * @return The repository
     */
    ArchiRepository getRepository();

    /**
     * Dispose of action
     */
    void dispose();
}