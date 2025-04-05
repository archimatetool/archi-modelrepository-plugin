/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.archicontribs.modelrepository.authentication.ProxyAuthenticator;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.archimatetool.editor.utils.StringUtils;



/**
 * Activator
 * 
 * @author Phillip Beauvoir
 */
public class ModelRepositoryPlugin extends AbstractUIPlugin {

    public static final String PLUGIN_ID = "org.archicontribs.modelrepository"; //$NON-NLS-1$
    
    // The shared instance
    private static ModelRepositoryPlugin instance;
    
    /**
     * @return the shared instance
     */
    public static ModelRepositoryPlugin getInstance() {
        return instance;
    }

    public ModelRepositoryPlugin() {
        instance = this;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        
        // Set this first
        ProxyAuthenticator.init();
    }
    
    /**
     * @return The File Location of this plugin
     */
    public File getPluginFolder() {
        try {
            URL url = FileLocator.resolve(getBundle().getEntry("/")); //$NON-NLS-1$
            return new File(url.getPath());
        }
        catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
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
    
    public void log(int severity, String message, Throwable ex) {
        getLog().log(
                new Status(severity, getInstance().getBundle().getSymbolicName(), IStatus.OK, message, ex));
    }
}
