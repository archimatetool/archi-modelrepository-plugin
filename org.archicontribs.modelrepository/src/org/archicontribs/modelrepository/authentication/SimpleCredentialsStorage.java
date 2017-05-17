/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.dialogs.UserNamePasswordDialog;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Simple Credentials Storage for user name and password
 * 
 * @author Phillip Beauvoir
 */
public class SimpleCredentialsStorage {
    
    public static String[] getUserNameAndPasswordFromCredentialsFileOrDialog(File folder, String storageFileName, Shell shell) throws IOException {
        String userName = null;
        String userPassword = null;
        
        SimpleCredentialsStorage sc = new SimpleCredentialsStorage(folder, storageFileName);
        
        if(sc.hasCredentialsFile()) {
            userName = sc.getUserName();
            userPassword = sc.getUserPassword();
        }
        // Ask user
        else {
            UserNamePasswordDialog dialog = new UserNamePasswordDialog(shell);
            if(dialog.open() != Window.OK) {
                return null;
            }
            
            userName = dialog.getUsername();
            userPassword = dialog.getPassword();
            
            // Store credentials if option is set
            if(ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getBoolean(IPreferenceConstants.PREFS_STORE_REPO_CREDENTIALS)) {
                try {
                    sc.store(userName, userPassword);
                }
                catch(NoSuchAlgorithmException | InvalidKeySpecException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return new String[] { userName, userPassword };
    }

    
    private File fFolder;
    private String fStorageFileName;
    
    public SimpleCredentialsStorage(File folder, String storageFileName) {
        fFolder = folder;
        fStorageFileName = storageFileName;
    }

    public void store(String userName, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Writer out = new OutputStreamWriter(new FileOutputStream(getCredentialsFile()));
        out.append(encrypt(userName) + "\n"); //$NON-NLS-1$
        out.append(encrypt(password));
        out.close();
    }
    
    public String getUserName() throws IOException {
        if(!hasCredentialsFile()) {
            return null;
        }
        
        BufferedReader in = new BufferedReader(new FileReader(getCredentialsFile()));
        String str = in.readLine();
        in.close();
        return decrypt(str);
    }
    
    public String getUserPassword() throws IOException {
        if(!hasCredentialsFile()) {
            return null;
        }
        
        BufferedReader in = new BufferedReader(new FileReader(getCredentialsFile()));
        in.readLine();
        String str = in.readLine();
        in.close();
        return decrypt(str);
    }
    
    public boolean hasCredentialsFile() {
        return getCredentialsFile().exists();
    }
    
    private String encrypt(String str) throws NoSuchAlgorithmException {
        BASE64Encoder encoder = new BASE64Encoder();
        return encoder.encode(generateSalt()) + encoder.encode(str.getBytes());
    }
    
    public static String decrypt(String encstr) throws IOException {
        if(encstr.length() > 12) {
            String cipher = encstr.substring(12);
            BASE64Decoder decoder = new BASE64Decoder();
            return new String(decoder.decodeBuffer(cipher));
        }

        return null;
    }
    
    private File getCredentialsFile() {
        return new File(fFolder, fStorageFileName);
    }
    
    private byte[] generateSalt() throws NoSuchAlgorithmException {
        // VERY important to use SecureRandom instead of just Random
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG"); //$NON-NLS-1$

        // Generate a 8 byte (64 bit) salt as recommended by RSA PKCS5
        byte[] salt = new byte[8];
        random.nextBytes(salt);

        return salt;
    }
}
