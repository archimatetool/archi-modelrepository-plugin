/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

/**
 * Information about a merge conflict object
 * 
 * @author Phillip Beauvoir
 */
public class MergeObjectInfo {

    private IArchiRepository archiRepo;

    private String xmlPath;
    private EObject[] objects = new EObject[2];
    
    public static int OURS = 0;
    public static int THEIRS = 1;
    
    private int choice = 0;

    public MergeObjectInfo(String xmlPath, IArchiRepository archiRepo) throws IOException {
        this.archiRepo = archiRepo;
        this.xmlPath = xmlPath;
        
        objects[OURS] = loadEObject(IGraficoConstants.HEAD);
        objects[THEIRS] = loadEObject(IGraficoConstants.ORIGIN_MASTER);
    }
    
    public String getXMLPath() {
        return xmlPath;
    }
    
    public EObject getEObject(int choice) {
        return objects[choice];
    }
    
    // Default is ours, or theirs if ours is null
    public EObject getDefault() {
        return objects[OURS] != null ? objects[OURS] : objects[THEIRS];
    }
    
    public String getStatus() {
        if(objects[OURS] == null) {
            return "Deleted by us";
        }
        if(objects[THEIRS] == null) {
            return "Deleted by them";
        }
        
        return "Modified";
    }
    
    public void setChoice(int choice) {
        this.choice = choice;
    }
    
    public int getChoice() {
        return choice;
    }

    // TODO - Do we need to put this in IArchiRepository?
    private EObject loadEObject(String ref) throws IOException {
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMLResourceFactoryImpl()); //$NON-NLS-1$

        XMLResource resource = (XMLResource)resourceSet.createResource(URI.createFileURI(archiRepo.getLocalGitFolder().toString()));
        resource.getDefaultLoadOptions().put(XMLResource.OPTION_ENCODING, "UTF-8"); //$NON-NLS-1$

        String contents = archiRepo.getFileContents(xmlPath, ref);
        if(contents == null) {
            return null;
        }
        
        resource.load(new ByteArrayInputStream(contents.getBytes()), null);
        return resource.getContents().get(0);
    }
}
