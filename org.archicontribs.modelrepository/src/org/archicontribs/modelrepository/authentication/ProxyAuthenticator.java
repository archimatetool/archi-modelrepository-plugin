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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.internal.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.IStatus;

/**
 * Proxy Authenticator
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
                    EncryptedCredentialsStorage cs = new EncryptedCredentialsStorage(new File(ModelRepositoryPlugin.getInstance().getUserModelRepositoryFolder(),
                            IGraficoConstants.PROXY_CREDENTIALS_FILE));
                    UsernamePassword npw = cs.getUsernamePassword();
                    
                    return new PasswordAuthentication(npw.getUsername(), npw.getPassword());
                }
                catch(GeneralSecurityException | IOException ex) {
                    ex.printStackTrace();
                    ModelRepositoryPlugin.getInstance().log(IStatus.ERROR, "Authentication failed to get credentials", ex); //$NON-NLS-1$
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
    }
    
    /**
     * Whether we are using a proxy as set in preferences
     */
    public static boolean isUsingProxy() {
        return ModelRepositoryPlugin.getInstance().getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_USE);
    }
    
    /**
     * Whether we are using authentication for the proxy as set in preferences
     */
    public static boolean isUsingAuthentication() {
        return ModelRepositoryPlugin.getInstance().getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_REQUIRES_AUTHENTICATION);
    }

    /**
     * Return the Proxy Host as set in preferences
     */
    public static String getProxyHost() {
        return ModelRepositoryPlugin.getInstance().getPreferenceStore().getString(IPreferenceConstants.PREFS_PROXY_HOST);
    }

    /**
     * Return the Proxy Port as set in preferences
     */
    public static int getProxyPort() {
        return ModelRepositoryPlugin.getInstance().getPreferenceStore().getInt(IPreferenceConstants.PREFS_PROXY_PORT);
    }

    // Update the proxy details
    public static synchronized void update(String repositoryURL) {
        // Don't use a proxy or HTTP proxy not used with SSH
        if(!isUsingProxy() || GraficoUtils.isSSH(repositoryURL)) {
            clear();
            return;
        }

        // Authentication is used
        if(isUsingAuthentication()) {
            // Set our Authenticator
            
            // However, once this is set Java caches the authentication details
            // So setting it back to null makes no difference
            // See https://bugs.openjdk.org/browse/JDK-4679480
            // See https://www.eclipse.org/forums/index.php?t=msg&th=1085879&goto=1816302&#msg_1816302
            Authenticator.setDefault(AUTHENTICATOR);
        }
        // No authentication used
        else {
            Authenticator.setDefault(null);
        }
        
        // The proxy to use
        final Proxy proxy = createHTTPProxyFromPreferences();
        
        // The default ProxySelector
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Arrays.asList(proxy);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ex) {
                ModelRepositoryPlugin.getInstance().log(IStatus.ERROR, "Connect failed in ProxySelector", ex); //$NON-NLS-1$
                ex.printStackTrace();
            }
        });
    }
    
    /**
     * Clear the Proxy settings
     */
    public static synchronized void clear() {
        Authenticator.setDefault(null); // This does not clear cached authentication details
        ProxySelector.setDefault(DEFAULT_PROXY_SELECTOR);
    }
    
    /**
     * Create a new Proxy from the hostName and port set in Preferences
     */
    private static Proxy createHTTPProxyFromPreferences() {
        InetSocketAddress socketAddress = new InetSocketAddress(getProxyHost(), getProxyPort());
        return new Proxy(Type.HTTP, socketAddress);
    }
    
    /**
     * Test a http connection
     */
    public static boolean testHTTPConnection(String url) throws IOException, URISyntaxException {
        if(GraficoUtils.isSSH(url)) {
            return false;
        }
        
        URL testURL = new URI(url).toURL();
        
        // localhost https connections throw certificate exceptions
        if("localhost".equals(testURL.getHost()) || "127.0.0.1".equals(testURL.getHost())) { //$NON-NLS-1$ //$NON-NLS-2$
            return false;
        }
        
        URLConnection connection;
        
        if(isUsingProxy()) {
            Proxy proxy = createHTTPProxyFromPreferences();
            connection = testURL.openConnection(proxy);
        }
        else {
            connection = testURL.openConnection();
        }
        
        connection.connect();
        
        return true;
    }
}
