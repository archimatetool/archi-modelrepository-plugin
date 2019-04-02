/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceImpl;

import com.archimatetool.editor.model.compatibility.IncompatibleModelException;
import com.archimatetool.editor.model.compatibility.ModelCompatibility;
import com.archimatetool.model.IIdentifier;

/**
 * Load an EObject from a file or input stream
 */
public class GraficoResourceLoader {

    public static IIdentifier loadEObject(File file) throws IOException {
        XMLResource resource = new XMLResourceImpl(URI.createFileURI(file.getAbsolutePath()));
        return load(resource, null);
    }
    
    public static IIdentifier loadEObject(InputStream inputStream) throws IOException {
        XMLResource resource = new XMLResourceImpl();
        return load(resource, inputStream);
    }
    
    private static IIdentifier load(XMLResource resource, InputStream inputStream) throws IOException {
        resource.getDefaultLoadOptions().put(XMLResource.OPTION_ENCODING, "UTF-8"); //$NON-NLS-1$
        
        ModelCompatibility modelCompatibility = new ModelCompatibility(resource);
        
        // Load the Resource so we can trap any exceptions
        try {
            if(inputStream != null) {
                resource.load(inputStream, null);
                inputStream.close();
            }
            else {
                resource.load(null);
            }
        }
        catch(IOException ex) {
            // Check to see if it's an exception that is OK or not
            try {
                modelCompatibility.checkErrors();
            }
            catch(IncompatibleModelException ex1) {
                ModelRepositoryPlugin.INSTANCE.log(IStatus.ERROR, "Error loading model", ex); //$NON-NLS-1$
                throw ex;
            }
        }
        
        EObject eObject = resource.getContents().get(0);
        
        if(!(eObject instanceof IIdentifier)) {
            throw new IOException("EObject has no ID"); //$NON-NLS-1$
        }
        
        // We have to remove the Eobject from its Resource so it can added to a new Resource and saved in the proper *.archimate format
        resource.getContents().remove(eObject);
        
        return (IIdentifier)eObject;
    }
}
