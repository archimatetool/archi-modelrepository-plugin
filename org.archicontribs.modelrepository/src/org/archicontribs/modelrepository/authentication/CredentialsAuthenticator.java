/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.TransportHttp;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import org.eclipse.jgit.util.FS;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;


/**
 * Authenticator for SSH and HTTP
 * 
 * @author Phillip Beauvoir
 */
public final class CredentialsAuthenticator {
	private static final Bundle BUNDLE = FrameworkUtil.getBundle(CredentialsAuthenticator.class);
	private static final ILog LOGGER = Platform.getLog(BUNDLE);
    
    public interface SSHIdentityProvider {
        File getIdentityFile();
        char[] getIdentityPassword() throws IOException, GeneralSecurityException;
    }
    
    static {
        /**
         * Set the SshSessionFactory instance to our specialised SshSessionFactory 
         */
        SshSessionFactory.setInstance(new CustomSshSessionFactory());
    }
    
    /**
     * SSH Identity Provider. Default is with details from Prefs
     */
    private static SSHIdentityProvider sshIdentityProvider = new SSHIdentityProvider() {
        @Override
        public File getIdentityFile() {
            return new File(ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_SSH_IDENTITY_FILE));
        }
        
        @Override
        public char[] getIdentityPassword() throws IOException, GeneralSecurityException {
            char[] password = null;
            
            if(Platform.getPreferencesService() != null // Check Preference Service is running in case background fetch is running and we quit the app
                    && ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_SSH_IDENTITY_REQUIRES_PASSWORD)) {
                
                EncryptedCredentialsStorage cs = new EncryptedCredentialsStorage(
                        new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(), IGraficoConstants.SSH_CREDENTIALS_FILE));

                if(cs.hasCredentialsFile()) {
                    password = cs.getPassword();
                }
                else {
                    throw new IOException(Messages.CredentialsAuthenticator_1);
                }
            }
            
            return password;
        }
    };
    
    public static void setSSHIdentityProvider(SSHIdentityProvider sshIdentityProvider) {
        CredentialsAuthenticator.sshIdentityProvider = sshIdentityProvider;
    }
    
    public static SSHIdentityProvider getSSHIdentityProvider() {
        return sshIdentityProvider;
    }
    
    /**
     * Factory method to get the TransportConfigCallback for authentication for repoURL
     * npw can be null and is ignored if repoURL is SSH
     */
    public static TransportConfigCallback getTransportConfigCallback(String repoURL, UsernamePassword npw) {
        return new TransportConfigCallback() {
            @Override
            public void configure(Transport transport) {
                transport.setRemoveDeletedRefs(true); // Delete remote branches that we don't have
                
                // SSH
                if(GraficoUtils.isSSH(repoURL)) {
                    transport.setCredentialsProvider(new SSHCredentialsProvider());
                }
             // HTTP
                else if(npw != null) {
                    transport.setCredentialsProvider(new UsernamePasswordCredentialsProvider(npw.getUsername(), npw.getPassword()));
                 // check for environment variable to add additional http headers to git request
                    Map<String,String> envVariables = System.getenv();
                    String additionalHeader = envVariables.get(ModelRepositoryPlugin.ENV_VAR_ADDITIONALHEADER);
                    if (additionalHeader != null && transport instanceof TransportHttp) {
                    	TransportHttp transportHttp = (TransportHttp) transport;
                        Map<String, String> headerMap = new HashMap<String, String>(1);
                        String headerKeyValue[] = additionalHeader.split(":");
                        if (headerKeyValue.length==2) {
                            headerMap.put(headerKeyValue[0], headerKeyValue[1]);
                            transportHttp.setAdditionalHeaders(headerMap);
                            log(">> added http headers:" + headerMap);
                        }
                    }
                }

            };
        };
 
    }
    
    public static void log(String msg) {
        log(msg, null);
     }

     public static void log(String msg, Exception e) {
        LOGGER.log(new Status((e==null? Status.INFO:Status.ERROR), BUNDLE.getSymbolicName(), msg, e));
     }
}
