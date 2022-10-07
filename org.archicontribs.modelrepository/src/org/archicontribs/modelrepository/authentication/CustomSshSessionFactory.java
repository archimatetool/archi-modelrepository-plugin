/**
 * This program and the accompanying materials are made available under the
 * terms of the License which accompanies this distribution in the file
 * LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;

/**
 * Our extended SshSessionFactory
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class CustomSshSessionFactory extends SshdSessionFactory {

    /**
        Ideally we'd like to do something like:
           Config.setProperty("StrictHostKeyChecking", "no")
           
        You can add the following to a ~/.ssh/config file:
           Host *
           StrictHostKeyChecking no
     */
    private boolean verifyServerKeys = false;

    /**
     * Whether to scan the ~/.ssh directory for all known keys with names "id_rsa, id_dsa, id_ecdsa, id_ed25519"
     */
    private boolean useDefaultIdentities = false;
    
    public CustomSshSessionFactory() {
        // Set ProxyDataFactory to null to allow SSH connections through the proxy if it's enabled
        super(null, null);
    }
    
    /**
     * By default the ~/.ssh directory is scanned for all supported private key files
     * But we can return the identity file as set in Preferences or set of files
     */
    @Override
    protected List<Path> getDefaultIdentities(File sshDir) {
        if(useDefaultIdentities) {
            return super.getDefaultIdentities(sshDir);
        }
        
        List<Path> paths = new ArrayList<Path>();
        
        // Scan SSH directory for all non-public files
        if(Platform.getPreferencesService() != null  // Check Preference Service is running in case background fetch is running and we quit the app
                && ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_SSH_SCAN_DIR)) {
            for(File file : sshDir.listFiles((dir, name) -> !name.endsWith(".pub") && !name.startsWith("known_hosts"))) {
                paths.add(file.toPath());
            }
            return paths;
        }
        
        // Single identity file as specified in prefs
        File file = CredentialsAuthenticator.getSSHIdentityProvider().getIdentityFile();
        if(file != null) {
            paths.add(file.toPath());
        }
        
        return paths;
    }

    /**
     * We can over-ride this to not verify server keys and not write to the known_hosts file
     */
    @Override
    protected ServerKeyDatabase getServerKeyDatabase(File homeDir, File sshDir) {
        if(verifyServerKeys) {
            return super.getServerKeyDatabase(homeDir, sshDir);
        }

        return new ServerKeyDatabase() {
            @Override
            public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress, Configuration config) {
                return new ArrayList<>();
            }

            @Override
            public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey, Configuration config,
                    CredentialsProvider provider) {
                return true;
            }
        };
    }
}
