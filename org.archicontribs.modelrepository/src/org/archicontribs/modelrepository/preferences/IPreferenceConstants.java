/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.preferences;




/**
 * Constant definitions for plug-in preferences
 * 
 * @author Phillip Beauvoir
 */
public interface IPreferenceConstants {
    
    String PREFS_COMMIT_USER_NAME = "userName"; //$NON-NLS-1$
    String PREFS_COMMIT_USER_EMAIL = "userEmail"; //$NON-NLS-1$
    String PREFS_REPOSITORY_FOLDER = "repoFolder"; //$NON-NLS-1$
    String PREFS_SSH_IDENTITY_FILE = "sshIdentityFile"; //$NON-NLS-1$
    String PREFS_SSH_IDENTITY_ENABLED = "sshIdentityEnabled"; //$NON-NLS-1$
    String PREFS_SSH_IDENTITY_REQUIRES_PASSWORD = "sshIdentityRequiresPassword"; //$NON-NLS-1$
    String PREFS_STORE_REPO_CREDENTIALS = "storeCredentials"; //$NON-NLS-1$
    
    String PREFS_PROXY_USE = "proxyUse"; //$NON-NLS-1$
    String PREFS_PROXY_HOST = "proxyHost"; //$NON-NLS-1$
    String PREFS_PROXY_PORT = "proxyPort"; //$NON-NLS-1$
    String PREFS_PROXY_REQUIRES_AUTHENTICATION = "proxyAuthenticate"; //$NON-NLS-1$
    
    String PREFS_EXPORT_MAX_THREADS = "exportMaxThreads";  //$NON-NLS-1$
    
    String PREFS_FETCH_IN_BACKGROUND = "fetchInBackground";  //$NON-NLS-1$
 }
