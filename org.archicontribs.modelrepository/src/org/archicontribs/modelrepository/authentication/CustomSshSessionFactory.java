/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator.SSHIdentityProvider;
import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig;
import org.eclipse.jgit.util.FS;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Our extended SshSessionFactory
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class CustomSshSessionFactory extends JschConfigSessionFactory {

    @Override
    protected void configure(OpenSshConfig.Host host, Session session) {
        session.setConfig("StrictHostKeyChecking", "no");
    }

    @Override
    protected JSch createDefaultJSch(FS fs) throws JSchException {
        JSch jsch = super.createDefaultJSch(fs);
        
        // TODO - we might not need to do this as it sets default locations for rsa_pub
        jsch.removeAllIdentity();
        
        try {
            SSHIdentityProvider sshIdentityProvider = CredentialsAuthenticator.getSSHIdentityProvider();
            File file = sshIdentityProvider.getIdentityFile();
            char[] pw = sshIdentityProvider.getIdentityPassword();
            
            if(pw != null) {
                jsch.addIdentity(file.getAbsolutePath(), new String(pw));
            }
            else {
                jsch.addIdentity(file.getAbsolutePath());
            }
        }
        catch(IOException | GeneralSecurityException ex) {
            throw new JSchException(ex.getMessage());
        }
        
        return jsch;
    }
}
