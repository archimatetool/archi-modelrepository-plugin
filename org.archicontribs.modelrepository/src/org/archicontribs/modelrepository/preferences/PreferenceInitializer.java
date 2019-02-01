/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.preferences;

import java.io.File;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import com.archimatetool.editor.ArchiPlugin;



/**
 * Class used to initialize default preference values
 * 
 * @author Phillip Beauvoir
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer
implements IPreferenceConstants {

    @Override
    public void initializeDefaultPreferences() {
		IPreferenceStore store = ModelRepositoryPlugin.INSTANCE.getPreferenceStore();
        
        store.setDefault(PREFS_COMMIT_USER_NAME, System.getProperty("user.name")); //$NON-NLS-1$
		store.setDefault(PREFS_COMMIT_USER_EMAIL, ""); //$NON-NLS-1$
		store.setDefault(PREFS_SSH_IDENTITY_ENABLED, false);
		store.setDefault(PREFS_SSH_IDENTITY_FILE, System.getProperty("user.home") + "/.ssh/id_rsa"); //$NON-NLS-1$
		store.setDefault(PREFS_SSH_IDENTITY_REQUIRES_PASSWORD, false);
		store.setDefault(PREFS_REPOSITORY_FOLDER, new File(ArchiPlugin.INSTANCE.getUserDataFolder(), "model-repository").getAbsolutePath()); //$NON-NLS-1$
		store.setDefault(PREFS_STORE_REPO_CREDENTIALS, false);
		
		store.setDefault(PREFS_PROXY_USE, false);
		store.setDefault(PREFS_PROXY_REQUIRES_AUTHENTICATION, false);
		store.setDefault(PREFS_PROXY_PORT, 8088);
		store.setDefault(PREFS_PROXY_HOST, "localhost"); //$NON-NLS-1$
		
		store.setDefault(PREFS_EXPORT_MAX_THREADS, 10);
		
		store.setDefault(PREFS_FETCH_IN_BACKGROUND, true);
    }
}
