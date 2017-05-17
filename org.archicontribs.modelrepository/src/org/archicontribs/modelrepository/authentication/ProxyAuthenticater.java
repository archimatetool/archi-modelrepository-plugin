/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;

import com.archimatetool.editor.utils.StringUtils;

/**
 * ProxyAuthenticater
 * 
 * @author Phillip Beauvoir
 */
public class ProxyAuthenticater {
    
    // Store the default
    static final ProxySelector DEFAULT_PROXY_SELECTOR = ProxySelector.getDefault();
    
    /**
     * Update the Proxy Authenticater
     * Get settings from user prefs
     * @throws IOException
     */
    public static void update() throws IOException {
        boolean useProxy = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_USE);
        if(!useProxy) {
            Authenticator.setDefault(null);
            ProxySelector.setDefault(DEFAULT_PROXY_SELECTOR);
            return;
        }
        
        boolean useAuthentication = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_PROXY_REQUIRES_AUTHENTICATION);

        if(useAuthentication) {
            SimpleCredentialsStorage sc = new SimpleCredentialsStorage(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                    IGraficoConstants.PROXY_CREDENTIALS_FILE);
            
            final String userName = sc.getUserName();
            final String password = sc.getUserPassword();
            
            if(!StringUtils.isSet(userName) || !StringUtils.isSet(password)) {
                return;
            }
            
            Authenticator.setDefault(new Authenticator() {
                @Override
                public PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password.toCharArray());
                }
            });
        }
        else {
            Authenticator.setDefault(null);
        }
        
        final String hostName = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_PROXY_HOST);
        final int port = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getInt(IPreferenceConstants.PREFS_PROXY_PORT);
        
        if(!StringUtils.isSet(hostName)) {
            return;
        }
        
        final InetSocketAddress address = new InetSocketAddress(InetAddress.getByName(hostName), port);
        
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Arrays.asList(new Proxy(Type.HTTP, address));
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            }
        });      

        // Test the connection
        URL url = new URL("http://localhost"); //$NON-NLS-1$
        URLConnection connection = url.openConnection();
        connection.connect();
    }
}
