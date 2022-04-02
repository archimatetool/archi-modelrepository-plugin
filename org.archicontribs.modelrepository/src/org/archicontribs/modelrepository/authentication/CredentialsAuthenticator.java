/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.osgi.util.NLS;


/**
 * Authenticator for SSH and HTTP
 * 
 * @author Phillip Beauvoir
 */
public final class CredentialsAuthenticator {
    
    public interface SSHIdentityProvider {
        File getIdentityFile() throws IOException;
        char[] getIdentityPassword() throws IOException, GeneralSecurityException;
    }
    
    /**
     * SSH Identity Provider. Default is with details from Prefs
     */
    private static SSHIdentityProvider sshIdentityProvider = new SSHIdentityProvider() {
        @Override
        public File getIdentityFile() throws IOException {
            File file = new File(ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_SSH_IDENTITY_FILE));
            
            if(!file.exists()) {
                throw new IOException(NLS.bind(Messages.CredentialsAuthenticator_0, file));
            }
            
            return file;
        }
        
        @Override
        public char[] getIdentityPassword() throws IOException, GeneralSecurityException {
            char[] password = null;
            
            if(ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_SSH_IDENTITY_REQUIRES_PASSWORD)) {
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
                
                if(transport instanceof SshTransport) {
                    // For some reason, we have to set a new instance of a SshSessionFactory each time
                    ((SshTransport)transport).setSshSessionFactory(new CustomSshSessionFactory());
                }
                
                // HTTP
                if(npw != null && GraficoUtils.isHTTP(repoURL)) {
                    transport.setCredentialsProvider(new UsernamePasswordCredentialsProvider(npw.getUsername(), npw.getPassword()));
                }
            }
        };
    }
}
