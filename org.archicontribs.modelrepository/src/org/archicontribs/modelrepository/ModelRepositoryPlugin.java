/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.archimatetool.editor.utils.StringUtils;



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
        // Get from preferences
        String path = getPreferenceStore().getString(IPreferenceConstants.PREFS_REPOSITORY_FOLDER);
        
        if(StringUtils.isSet(path)) {
            File file = new File(path);
            if(file.canWrite()) {
                return file;
            }
        }
        
        // Default
        path = getPreferenceStore().getDefaultString(IPreferenceConstants.PREFS_REPOSITORY_FOLDER);
        return new File(path);
    }
}
