/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.archimatetool.editor.ArchiPlugin;



/**
 * Activitor
 * 
 * @author Phillip Beauvoir
 */
public class ModelRepositoryPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.archicontribs.modelrepository"; //$NON-NLS-1$
    
    /**
     * The shared instance
     */
    public static ModelRepositoryPlugin INSTANCE;

    public ModelRepositoryPlugin() {
        INSTANCE = this;
    }

    /**
     * @return The File Location of this plugin
     */
    public File getPluginFolder() {
        URL url = getBundle().getEntry("/"); //$NON-NLS-1$
        try {
            url = FileLocator.resolve(url);
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        return new File(url.getPath());
    }
    
    /**
     * @return The folder where we store repositories
     */
    public File getUserModelRepositoryFolder() {
        return new File(ArchiPlugin.INSTANCE.getUserDataFolder(), "model-repository"); //$NON-NLS-1$
    }
    
}
