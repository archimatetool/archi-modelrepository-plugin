/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;


/**
 * Encrypted Credentials Storage for user name and password
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class EncryptedCredentialsStorage {
    
    
    /**
     * File name of secure primary key for encrypted files
     */
    private static final String PRIMARY_KEY_FILE = "primary_key"; //$NON-NLS-1$

    /**
     * File name of secure user name/password for each git repo
     */
    private static final String SECURE_REPO_CREDENTIALS_FILE = "secure_credentials"; //$NON-NLS-1$
    
    /**
     * Convenience method to create new a EncryptedCredentialsStorage for a repository
     */
    public static EncryptedCredentialsStorage forRepository(IArchiRepository repository) {
        return new EncryptedCredentialsStorage(new File(repository.getLocalGitFolder(), SECURE_REPO_CREDENTIALS_FILE));
    }
    
    private File fStorageFile;
    
    public EncryptedCredentialsStorage(File storageFile) {
        fStorageFile = storageFile;
    }

    public boolean store(UsernamePassword npw) throws GeneralSecurityException, IOException {
        return store(npw.getUsername(), npw.getPassword());
    }

    public boolean store(String userName, String password) throws GeneralSecurityException, IOException {
        File file = getCredentialsFile();
        file.getParentFile().mkdirs();
        
        // Get the stored primary key
        SecretKey key = getStoredPrimaryKey();
        if(key == null) {
            return false;
        }
        
        // Get username and password as bytes
        byte[] bytes = new String(userName + password).getBytes("UTF-8");
        
        // Prepend a single byte for the length of the user name
        bytes = ByteBuffer.allocate(bytes.length + 1)
                         .put((byte)userName.length())
                         .put(bytes)
                         .array();
        
        // Encrypt
        Cipher cipher = makeCipherWithKey(key, Cipher.ENCRYPT_MODE);
        
        try(CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(file), cipher)) {
            cos.write(bytes);
        }
        
        return true;
    }
    
    public UsernamePassword getUsernamePassword() throws GeneralSecurityException, IOException {
        String userName = "";
        String password = "";
        
        if(hasCredentialsFile()) {
            byte[] bytes = null;
            
            // Get the store primary key
            SecretKey key = getStoredPrimaryKey();
            if(key == null) {
                return new UsernamePassword(userName, password);
            }
            
            // Read in encrypted bytes
            Cipher cipher = makeCipherWithKey(key, Cipher.DECRYPT_MODE);
            
            try(CipherInputStream cis = new CipherInputStream(new FileInputStream(getCredentialsFile()), cipher)) {
                bytes = cis.readAllBytes();
            }
            
            if(bytes != null) {
                // First byte is username length
                int userNameLength = bytes[0];
                
                // Convert bytes to strings at offsets
                userName = new String(Arrays.copyOfRange(bytes, 1, userNameLength + 1));
                password = new String(Arrays.copyOfRange(bytes, userNameLength + 1, bytes.length));
            }
        }
        
        return new UsernamePassword(userName, password);
    }

    public boolean hasCredentialsFile() {
        // Check for zero length file
        if(getCredentialsFile().exists() && getCredentialsFile().length() == 0) {
            deleteCredentialsFile();
        }
        
        return getCredentialsFile().exists();
    }
    
    public boolean deleteCredentialsFile() {
        return getCredentialsFile().delete();
    }
    
    /**
     * Make a Cipher from a given Key
     */
    private Cipher makeCipherWithKey(Key key, int mode) throws GeneralSecurityException {
        // Set up the cipher
        Cipher cipher = Cipher.getInstance("AES");

        // Set the cipher mode to decryption or encryption
        cipher.init(mode, key);

        return cipher;
    }
    
    private File getCredentialsFile() {
        return fStorageFile;
    }
    
    // ==============================================================================================
    // Primary Key Storage
    // ==============================================================================================
    
    private static SecretKey storedPrimaryKey;
    
    public static String askUserForPrimaryPassword() {
        InputDialog dialog = new InputDialog(Display.getCurrent().getActiveShell(),
                "Primary Password",
                "Enter Primary Password", null, null) {
            
            @Override
            protected int getInputTextStyle() {
                return super.getInputTextStyle() | SWT.PASSWORD;
            }
        };
        
        if(dialog.open() == Window.OK) {
            return dialog.getValue();
        }
        
        return null;
    }
    
    private static SecretKey getStoredPrimaryKey() throws GeneralSecurityException, IOException {
        if(storedPrimaryKey == null) {
            // Get password from user
            String password = askUserForPrimaryPassword();
            
            if(password != null) {
                storedPrimaryKey = getStoredPrimaryKey(password);
                
                if(storedPrimaryKey == null) {
                    storedPrimaryKey = generatePrimaryKey();
                    savePrimaryKey(storedPrimaryKey, password);
                }
            }
        }
        
        return storedPrimaryKey;
    }
    
    /**
     * Get the stored primary key or null if not present
     */
    private static SecretKey getStoredPrimaryKey(String password) throws GeneralSecurityException, IOException {
        File file = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(), PRIMARY_KEY_FILE);
        
        if(file.exists()) {
            Cipher cipher = makeCipherWithPassword(password, Cipher.DECRYPT_MODE);
            
            byte[] bytes = null;
            
            try(CipherInputStream cis = new CipherInputStream(new FileInputStream(file), cipher)) {
                bytes = cis.readAllBytes();
            }
            
            if(bytes != null) {
                return new SecretKeySpec(bytes, "AES");
            }
        }
        
        return null;
    }
    
    /**
     * Save the primary key encrypted with a password
     */
    private static void savePrimaryKey(SecretKey key, String password) throws GeneralSecurityException, IOException {
        File file = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(), PRIMARY_KEY_FILE);
        
        Cipher cipher = makeCipherWithPassword(password, Cipher.ENCRYPT_MODE);
        
        try(CipherOutputStream cos = new CipherOutputStream(new FileOutputStream(file), cipher)) {
            cos.write(key.getEncoded());
        }
    }
    
    /**
     * Generate a new primary key
     */
    private static SecretKey generatePrimaryKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, SecureRandom.getInstanceStrong());
        return keyGenerator.generateKey();
    }
    
    // Arbitrarily selected 8-byte salt sequence
    private static final byte[] salt = {
        (byte) 0x21, (byte) 0x52, (byte) 0x95, (byte) 0xc7,
        (byte) 0x7b, (byte) 0xd6, (byte) 0x41, (byte) 0x18 
    };

    /**
     * Create a Cipher using a password rather than a key
     * The key is created from the password
     * See https://stackoverflow.com/questions/13673556/using-password-based-encryption-on-a-file-in-java
     */
    private static Cipher makeCipherWithPassword(String password, int mode) throws GeneralSecurityException {
        // Use a KeyFactory to derive the corresponding key from the password
        PBEKeySpec keySpec = new PBEKeySpec(password.toCharArray());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
        SecretKey key = keyFactory.generateSecret(keySpec);

        // Create parameters from the salt and an arbitrary number of iterations:
        PBEParameterSpec pbeParamSpec = new PBEParameterSpec(salt, 28);

        // Set up the cipher
        Cipher cipher = Cipher.getInstance("PBEWithMD5AndDES");

        // Set the cipher mode to decryption or encryption
        cipher.init(mode, key, pbeParamSpec);

        return cipher;
    }
}
