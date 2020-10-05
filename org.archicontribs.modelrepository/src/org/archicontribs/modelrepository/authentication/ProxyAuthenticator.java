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
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IStatus;

import com.archimatetool.editor.utils.StringUtils;

/**
 * ProxyAuthenticator
 * 
 * @author Phillip Beauvoir
 */
public class ProxyAuthenticator {
    
    // Store the default before we set ours
    static final ProxySelector DEFAULT_PROXY_SELECTOR = ProxySelector.getDefault();
    
    public static void init() {
        // This needs to be set in order to avoid this exception when using a Proxy:
        // "Unable to tunnel through proxy. Proxy returns "HTTP/1.1 407 Proxy Authentication Required""
        // It needs to be set before any JGit operations, because it can't be set again
        System.setProperty("jdk.http.auth.tunneling.disabledSchemes", ""); //$NON-NLS-1$ //$NON-NLS-2$
        
        // Added this one too. I thnk it's for HTTP
        System.setProperty("jdk.http.auth.proxying.disabledSchemes", ""); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Update the Proxy Authenticater
     * Get settings from user prefs
     * @throws IOException
     */
    public static boolean update(String repositoryURL) throws IOException, GeneralSecurityException {
        // HTTP proxy not used with SSH
        if(GraficoUtils.isSSH(repositoryURL)) {
            return true;
        }
        
        boolean useProxy = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_USE);
        
        if(!useProxy) {
            Authenticator.setDefault(null);
            ProxySelector.setDefault(DEFAULT_PROXY_SELECTOR);
            
            // Test the connection - this is better to do it now
            testConnection(repositoryURL, null);
            
            return true;
        }
        
        boolean useAuthentication = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_REQUIRES_AUTHENTICATION);

        if(useAuthentication) {
            // Check primary key set
            if(!EncryptedCredentialsStorage.checkPrimaryKeySet()) {
                return false;
            }
            
            final EncryptedCredentialsStorage cs = new EncryptedCredentialsStorage(new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                    IGraficoConstants.PROXY_CREDENTIALS_FILE));
            
            final UsernamePassword npw = cs.getUsernamePassword();
            
            Authenticator.setDefault(new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    // If proxy, return its credentials
                    // Otherwise the requested URL is the endpoint (and not the proxy host)
                    // In this case the authentication should not be proxy so return null (and JGit CredentialsProvider will be used)
                    if(getRequestorType() == RequestorType.PROXY) {
                        return new PasswordAuthentication(npw.getUsername(), npw.getPassword().toCharArray());
                    }
                    return null;
                }
            });
        }
        else {
            Authenticator.setDefault(null);
        }
        
        final String hostName = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_PROXY_HOST);
        final int port = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PROXY_PORT);
        
        if(!StringUtils.isSet(hostName)) {
            return false;
        }
        
        // Test the connection is reachable
        // Removed 26/3/19 - do we need to do this? Does it always work without a port?
//        InetAddress addr = InetAddress.getByName(hostName);
//        if(!addr.isReachable(2000)) {
//            throw new IOException(Messages.ProxyAuthenticator_0 + " " + hostName); //$NON-NLS-1$
//        }

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

        // Test the connection with the repository URL
        testConnection(repositoryURL, proxy);
        
        return true;
    }
    
    /**
     * Test a connection
     * @param repositoryURL
     * @param proxy
     * @throws IOException 
     */
    private static void testConnection(String repositoryURL, Proxy proxy) throws IOException {
        URL testURL = new URL(repositoryURL);
        
        // localhost https connections throw certificate exceptions
        if("localhost".equals(testURL.getHost()) || "127.0.0.1".equals(testURL.getHost())) { //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        
        URLConnection connection;
        
        if(proxy != null) {
            connection = testURL.openConnection(proxy);
        }
        else {
            connection = testURL.openConnection();
        }
        
        connection.connect();
    }
}
