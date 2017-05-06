/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.preferences;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;



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
    }
}
