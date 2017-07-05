/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Handler to Resolve Conflict Issues on Grafico Import
 * 
 * @author Phillip Beauvoir
 */
public class ConflictResolutionHandler {

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
    
    public ConflictResolutionHandler(IArchimateModel model) {
        fModel = model;
        fProblems = new ArrayList<ProblemPair>();
    }

    /**
     * Add a proxy problem pair of objects
     * @param object
     * @param parent
     */
    void addResolveProblem(IIdentifier object, IIdentifier parent) {
        fProblems.add(new ProblemPair(object, parent));
    }
    
    public void resolveProblemObjects() {
        // deleteProblemObjects();
        
        restoreProblemObjects();
    }
    
    public void restoreProblemObjects() {
        for(ProblemPair problemPair : fProblems) {
            URI objectURI = EcoreUtil.getURI(problemPair.object);
            String objectID = objectURI.fragment();
            //String parentID = problemPair.parent.getId();
            
            // Find the problem object xml files from the commit history and restore them
            // Then I think we need to do another Grafico Import...
        }
    }

    public void deleteProblemObjects() throws IOException {
        for(ProblemPair problemPair : fProblems) {
            //String objectID = EcoreUtil.getURI(problemPair.object).fragment();
            String parentID = problemPair.parent.getId();
            
            EObject eObject = ArchimateModelUtils.getObjectByID(fModel, parentID);
            if(eObject != null) {
                EcoreUtil.remove(eObject);
            }
        }
        
        // And re-export to grafico xml files
        GraficoModelExporter exporter = new GraficoModelExporter(fModel, GraficoUtils.getLocalRepositoryFolderForModel(fModel));
        exporter.exportModel();
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

    public MultiStatus getResolveStatus() {
        MultiStatus ms = new MultiStatus(ModelRepositoryPlugin.PLUGIN_ID, IStatus.ERROR, Messages.GraficoResolutionHandler_0, null);
        
        for(String message : getErrorMessages()) {
            ms.add(new Status(IStatus.ERROR, ModelRepositoryPlugin.PLUGIN_ID, message));
        }
        
        return ms;
    }
}
