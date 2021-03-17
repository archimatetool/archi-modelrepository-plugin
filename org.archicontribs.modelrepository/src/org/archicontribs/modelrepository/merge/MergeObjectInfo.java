/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.merge;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.archicontribs.modelrepository.grafico.GraficoResourceLoader;
import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IIdentifier;
import com.archimatetool.model.util.ArchimateModelUtils;

/**
 * Information about a merge conflict object
 * 
 * @author Phillip Beauvoir
 */
class MergeObjectInfo {

    private IMergeConflictHandler handler;

    private String xmlPath;
    
    // Referenced EObjects - ours and theirs
    private EObject[] objects = new EObject[2];
    
    public static int OURS = 0;
    public static int THEIRS = 1;
    
    // User's choice
    private int userChoice = OURS;

    MergeObjectInfo(String xmlPath, IMergeConflictHandler handler) throws IOException {
        this.handler = handler;
        this.xmlPath = xmlPath;
        
        objects[OURS] = loadEObject(handler.getLocalRef());
        objects[THEIRS] = loadEObject(handler.getTheirRef());
    }
    
    String getXMLPath() {
        return xmlPath;
    }
    
    EObject getEObject(int choice) {
        return objects[choice];
    }
    
    // Default is ours, or theirs if ours is null
    EObject getDefaultEObject() {
        return objects[OURS] != null ? objects[OURS] : objects[THEIRS];
    }
    
    String getStatus() {
        if(objects[OURS] == null) {
            return Messages.MergeObjectInfo_0;
        }
        if(objects[THEIRS] == null) {
            return Messages.MergeObjectInfo_1;
        }
        
        return Messages.MergeObjectInfo_2;
    }
    
    void setUserChoice(int choice) {
        userChoice = choice;
    }
    
    int getUserChoice() {
        return userChoice;
    }

    /**
     * Load the EObject from the ours or theirs XML file so we can get its ID
     * Once we have its ID we can load the real EObject from either "theirs" or "ours" full model.
     * We do this because some EObjects have proxy references to other EObjects that would need resolving
     * Returns null if the file contents does not exist (either we or they deleted the object)
     * ref is either ours or theirs
     */
    private EObject loadEObject(String ref) throws IOException {
        // Load the contents of the ref not the actual file because "theirs" is not an actual file
        byte[] contents = handler.getArchiRepository().getFileContents(xmlPath, ref);
        // Not found so was deleted by us or them
        if(contents == null) {
            return null;
        }
        
        ByteArrayInputStream bis = new ByteArrayInputStream(contents);
        
        IIdentifier eObject = GraficoResourceLoader.loadEObject(bis);
        
        // Get the ID
        String id = eObject.getId();
        
        // Now get the full object from the appropriate model
        IArchimateModel model = null;
        
        if(ref == handler.getLocalRef()) { // Ours
            model = handler.getOurModel();
        }
        else {
            model = handler.getTheirModel();
        }

        return ArchimateModelUtils.getObjectByID(model, id);
    }
}
