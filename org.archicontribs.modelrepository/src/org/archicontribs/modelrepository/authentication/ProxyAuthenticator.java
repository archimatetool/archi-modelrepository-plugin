/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IStatus;

/**
 * ProxyAuthenticator
 * 
 * @author Phillip Beauvoir
 */
public class ProxyAuthenticator {
    
    // Store the default ProxySelector before we set ours
    private static final ProxySelector DEFAULT_PROXY_SELECTOR = ProxySelector.getDefault();
    
    // Our Authenticator
    private static Authenticator AUTHENTICATOR = new Authenticator() {
        @Override
        public PasswordAuthentication getPasswordAuthentication() {
            // If proxy request, return its credentials
            // Otherwise the requested URL is the endpoint (and not the proxy host)
            // In this case the authentication should not be proxy so return null (and JGit CredentialsProvider will be used)
            if(getRequestorType() == RequestorType.PROXY) {
                try {
                    // Check primary key is set for access to proxy credentials
                    if(!EncryptedCredentialsStorage.isPrimaryKeySet()) {
                        return null;
                    }

                    // Get the username and password for the proxy from encrypted file
                    EncryptedCredentialsStorage cs = new EncryptedCredentialsStorage(new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                            IGraficoConstants.PROXY_CREDENTIALS_FILE));
                    UsernamePassword npw = cs.getUsernamePassword();
                    
                    return new PasswordAuthentication(npw.getUsername(), npw.getPassword().toCharArray());
                }
                catch(GeneralSecurityException | IOException ex) {
                    ex.printStackTrace();
                    ModelRepositoryPlugin.INSTANCE.log(IStatus.ERROR, "Authentication failed to get credentials", ex); //$NON-NLS-1$
                    return null;
                }
            }
            
            // Not a proxy request
            return null;
        }
    };
    
    // Initialise
    public static void init() {
        // This needs to be set in order to avoid this exception when using a Proxy:
        // "Unable to tunnel through proxy. Proxy returns "HTTP/1.1 407 Proxy Authentication Required""
        // It needs to be set before any JGit operations, because it can't be set again
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", ""); //$NON-NLS-1$ //$NON-NLS-2$
        
        // Added this one too. I think it's for HTTP
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", ""); //$NON-NLS-1$ //$NON-NLS-2$
        
        // Set up now
        setupProxy();
        
        // On preference change setup again
        ModelRepositoryPlugin.INSTANCE.getPreferenceStore().addPropertyChangeListener(event -> {
            if(event.getProperty() == IPreferenceConstants.PREFS_PROXY_USE || 
                                        event.getProperty() == IPreferenceConstants.PREFS_PROXY_REQUIRES_AUTHENTICATION ||
                                        event.getProperty() == IPreferenceConstants.PREFS_PROXY_HOST || 
                                        event.getProperty() == IPreferenceConstants.PREFS_PROXY_PORT) {
                setupProxy();
            }
        });
    }
    
    // Setup the proxy
    private static void setupProxy() {
        boolean useProxy = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_USE);
        
        // Don't use proxy
        if(!useProxy) {
            Authenticator.setDefault(null);
            ProxySelector.setDefault(DEFAULT_PROXY_SELECTOR);
            return;
        }
        
        boolean useAuthentication = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_REQUIRES_AUTHENTICATION);

        // Authentication is used
        if(useAuthentication) {
            // Set our Authenticator
            Authenticator.setDefault(AUTHENTICATOR);
        }
        // No authentication used
        else {
            Authenticator.setDefault(null);
        }
        
        // Get host name and port
        final String hostName = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_PROXY_HOST);
        final int port = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PROXY_PORT);
        
        // Create a new Proxy from these
        final InetSocketAddress socketAddress = new InetSocketAddress(hostName, port);
        final Proxy proxy = new Proxy(Type.HTTP, socketAddress);
        
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Arrays.asList(proxy);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ex) {
                ModelRepositoryPlugin.INSTANCE.log(IStatus.ERROR, "Connect failed in ProxySelector", ex); //$NON-NLS-1$
                ex.printStackTrace();
            }
        });      
    }
}
