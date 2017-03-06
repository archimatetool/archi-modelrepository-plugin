package org.archicontribs.modelrepository;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.archimatetool.editor.ArchiPlugin;



/**
 * Activitor
 * 
 * @author Phillip Beauvoir
 */
public class ModelRepositoryPlugin extends AbstractUIPlugin implements IStartup {

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
     * @return The folder where we store user scripts
     */
    public File getUserScriptsFolder() {
        return new File(ArchiPlugin.INSTANCE.getUserDataFolder(), "model-repository"); //$NON-NLS-1$
    }
    
    /**
     * @return The folder where we store example scripts
     */
//    public File getExamplesFolder() {
//        return new File(getPluginFolder(), "examples"); //$NON-NLS-1$
//    }

    public void earlyStartup() {
        // Do nothing
    }

}
