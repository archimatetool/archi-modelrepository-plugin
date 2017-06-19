/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Contains information to Resolve Issues
 * 
 * @author Phillip Beauvoir
 */
public class GraficoResolutionHandler {

    private static class ProblemPair {
        IIdentifier object;
        IIdentifier parent;

        ProblemPair(IIdentifier object, IIdentifier parent) {
            this.object = object;
            this.parent = parent;
        }
    }
    
    private IArchimateModel fModel;
    
    private List<ProblemPair> fProblems;
    
    public GraficoResolutionHandler(IArchimateModel model) {
        fModel = model;
        fProblems = new ArrayList<ProblemPair>();
    }

    /**
     * Add a proxy problem pair of objects
     * @param object
     * @param parent
     */
    public void addResolveProblem(IIdentifier object, IIdentifier parent) {
        fProblems.add(new ProblemPair(object, parent));
    }
    
    public void deleteProblemObjects() {
        for(ProblemPair problemPair : fProblems) {
            //String objectID = EcoreUtil.getURI(problemPair.object).fragment();
            String parentID = problemPair.parent.getId();
            
            EObject eObject = ArchimateModelUtils.getObjectByID(fModel, parentID);
            if(eObject != null) {
                EcoreUtil.remove(eObject);
            }
        }
    }
    
    public boolean hasProblems() {
        return !fProblems.isEmpty();
    }
    
    /**
     * @return A list of error messages
     */
    public List<String> getErrorMessages() {
        List<String> messages = new ArrayList<String>();
        
        for(ProblemPair problemPair : fProblems) {
            String objectID = EcoreUtil.getURI(problemPair.object).fragment();
            String parentID = problemPair.parent.getId();
            
            String message = NLS.bind(Messages.ResolutionHandler_0,
                    new Object[] { objectID, problemPair.parent.getClass().getSimpleName(), parentID });
            
            messages.add(message);
        }
        
        return messages;
    }
}
