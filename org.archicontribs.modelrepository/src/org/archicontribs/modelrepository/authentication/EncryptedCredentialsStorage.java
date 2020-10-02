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
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.dialogs.NewPrimaryPasswordDialog;
import org.archicontribs.modelrepository.dialogs.PrimaryPasswordDialog;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.utils.StringUtils;


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
    private static final String PRIMARY_KEY_FILE = "primary_key";

    /**
     * File name of secure user name/password for each git repo
     */
    private static final String SECURE_REPO_CREDENTIALS_FILE = "secure_credentials";
    
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    
    /**
     * Convenience method to create new a EncryptedCredentialsStorage for a repository
     */
    public static EncryptedCredentialsStorage forRepository(IArchiRepository repository) {
        return new EncryptedCredentialsStorage(new File(repository.getLocalGitFolder(), SECURE_REPO_CREDENTIALS_FILE));
    }
    
    private File fStorageFile;
    private Properties fProperties;
    
    public EncryptedCredentialsStorage(File storageFile) {
        fStorageFile = storageFile;
    }

    public void store(UsernamePassword npw) throws GeneralSecurityException, IOException {
        store(npw.getUsername(), npw.getPassword());
    }

    public void store(String userName, String password) throws GeneralSecurityException, IOException {
        storeUserName(userName);
        storePassword(password);
    }
    
    public void storeUserName(String userName) throws IOException {
        Properties properties = getProperties();
        
        // If userName not set remove it
        if(!StringUtils.isSet(userName)) {
            properties.remove(USERNAME);
        }
        else {
            properties.setProperty(USERNAME, userName);
        }
        
        saveProperties();
    }
    
    public boolean storePassword(String password) throws GeneralSecurityException, IOException {
        // If password not set remove it
        if(!StringUtils.isSet(password)) {
            getProperties().remove(PASSWORD);
            saveProperties();
            return true;
        }
        
        // Get the stored primary key
        SecretKey key = getStoredPrimaryKey();
        if(key == null) {
            return false;
        }
        
        // Get password as bytes
        byte[] passwordBytes = password.getBytes("UTF-8"); // Must use UTF-8
        
        // Encrypt the password
        Cipher cipher = makeCipherWithKey(key, Cipher.ENCRYPT_MODE);
        byte[] encrypted = cipher.doFinal(passwordBytes);
        
        // Store in properties file
        getProperties().setProperty(PASSWORD, Base64.getEncoder().encodeToString(encrypted)); // Use Base64 because this is a string
        saveProperties();
        
        return true;
    }

    public UsernamePassword getUsernamePassword() throws GeneralSecurityException, IOException {
        return new UsernamePassword(getUserName(), getPassword());
    }
    
    public String getUserName() throws IOException {
        return getProperties().getProperty(USERNAME, "");
    }
    
    public String getPassword() throws IOException, GeneralSecurityException {
        if(hasPassword()) {
            // Get the stored primary key
            SecretKey key = getStoredPrimaryKey();
            if(key == null) {
                return "";
            }
            
            // Decode password from Base64 string in properties
            String pw = getProperties().getProperty(PASSWORD, "");
            byte[] passwordBytes = Base64.getDecoder().decode(pw);
            
            // Decrypt password
            Cipher cipher = makeCipherWithKey(key, Cipher.DECRYPT_MODE);
            passwordBytes = cipher.doFinal(passwordBytes);
            
            return new String(passwordBytes, "UTF-8"); // Use UTF-8 for the string because we used that to encrypt
        }
        
        return "";
    }
    
    /**
     * Lighweight method of determining if there is a password entry without actually decrypting it
     */
    public boolean hasPassword() throws IOException {
        return StringUtils.isSet(getProperties().getProperty(PASSWORD, ""));
    }

    public boolean hasCredentialsFile() {
        return getCredentialsFile().exists();
    }
    
    private File getCredentialsFile() {
        return fStorageFile;
    }
    
    public boolean deleteCredentialsFile() {
        fProperties = null;
        return getCredentialsFile().delete();
    }
    
    private Properties getProperties() throws IOException {
        if(fProperties == null) {
            fProperties = new Properties();
            
            if(hasCredentialsFile()) {
                try(FileInputStream is = new FileInputStream(getCredentialsFile())) {
                    fProperties.load(is);
                }
            }
        }
        
        return fProperties;
    }
    
    private void saveProperties() throws IOException {
        if(fProperties == null) {
            throw new IOException("Credentials Properties is null");
        }
        
        File credentialsFile = getCredentialsFile();
        
        // If there is a password or a user name
        if(hasPassword() || StringUtils.isSet(getUserName())) {
            getCredentialsFile().getParentFile().mkdirs();
            
            try(FileOutputStream out = new FileOutputStream(credentialsFile)) {
                fProperties.store(out, null);
            }
        }
        // If not delete the file
        else {
            credentialsFile.delete();
        }
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
    
    
    // ==============================================================================================
    // Primary Key Storage
    // ==============================================================================================
    
    private static SecretKey storedPrimaryKey;
    
    public static String askUserForPrimaryPassword() {
        PrimaryPasswordDialog dialog = new PrimaryPasswordDialog(Display.getCurrent().getActiveShell());

        if(dialog.open() == Window.OK) {
            return dialog.getValue();
        }
        
        return null;
    }
    
    public static String askUserToCreatePrimaryPassword() {
        NewPrimaryPasswordDialog dialog = new NewPrimaryPasswordDialog(Display.getCurrent().getActiveShell());
        
        if(dialog.open() == Window.OK) {
            return dialog.getPassword();
        }
        
        return null;
    }
    
    /**
     * Ensure the primary key is set and ask user for it if not set
     */
    public static boolean checkPrimaryKeySet() throws GeneralSecurityException, IOException {
        return getStoredPrimaryKey() != null;
    }
    
    /**
     * Check wether the primary key is set
     */
    public static boolean isPrimaryKeySet() {
        return storedPrimaryKey != null;
    }
    
    private static SecretKey getStoredPrimaryKey() throws GeneralSecurityException, IOException {
        if(storedPrimaryKey == null) {
            File primaryKeyFile = getPrimaryKeyFile();
            
            // If the key file exists just ask user for password
            if(primaryKeyFile.exists()) {
                String password = askUserForPrimaryPassword();
                if(password != null) {
                    storedPrimaryKey = getStoredPrimaryKey(password);
                }
            }
            // Else create a new file key with password
            else {
                String password = askUserToCreatePrimaryPassword();
                if(password != null) {
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
            byte[] bytes = null;
            
            // Read in all bytes
            try(FileInputStream fis = new FileInputStream(file)) {
                bytes = fis.readAllBytes();
            }
            
            if(bytes != null) {
                // Get the salt from the first 8 bytes
                byte[] salt = Arrays.copyOfRange(bytes, 0, 8);
                
                // Get the remaining encrypted key bytes
                byte[] keybytes = Arrays.copyOfRange(bytes, 8, bytes.length);
                
                // Decrypt the key bytes
                Cipher cipher = makeCipherWithPassword(password, Cipher.DECRYPT_MODE, salt);
                keybytes = cipher.doFinal(keybytes);
                
                // Return the key
                return new SecretKeySpec(keybytes, "AES");
            }
        }
        
        return null;
    }
    
    /**
     * Save the primary key encrypted with a password
     */
    private static void savePrimaryKey(SecretKey key, String password) throws GeneralSecurityException, IOException {
        // Generate a new random salt
        byte[] salt = generateSalt();
        
        // Encrypt the key
        Cipher cipher = makeCipherWithPassword(password, Cipher.ENCRYPT_MODE, salt);
        byte[] keybytes = cipher.doFinal(key.getEncoded());
        
        // Save it
        try(FileOutputStream fos = new FileOutputStream(getPrimaryKeyFile())) {
            // Store the password salt
            fos.write(salt);
            
            // Store the encypted key
            fos.write(keybytes);
        }
    }
    
    /**
     * @return The primary key file
     */
    private static File getPrimaryKeyFile() {
        return new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(), PRIMARY_KEY_FILE);
    }
    
    /**
     * Generate a new primary key
     */
    private static SecretKey generatePrimaryKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, SecureRandom.getInstanceStrong());
        return keyGenerator.generateKey();
    }
    
    /**
     * Generate a new random salt
     */
    private static byte[] generateSalt() {
        byte[] salt = new byte[8];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Create a Cipher using a password rather than a key
     * The key is generated from the the password
     * See https://stackoverflow.com/questions/13673556/using-password-based-encryption-on-a-file-in-java
     */
    private static Cipher makeCipherWithPassword(String password, int mode, byte[] salt) throws GeneralSecurityException {
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
