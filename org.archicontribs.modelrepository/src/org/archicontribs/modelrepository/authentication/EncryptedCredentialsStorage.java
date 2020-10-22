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
import java.nio.file.Files;
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

    public void store(String userName, char[] password) throws GeneralSecurityException, IOException {
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
    
    public boolean storePassword(char[] password) throws GeneralSecurityException, IOException {
        // If password not set remove it
        if(password == null || password.length == 0) {
            getProperties().remove(PASSWORD);
            saveProperties();
            return true;
        }
        
        // Get the primary key
        SecretKey key = getPrimaryKey();
        if(key == null) {
            return false;
        }
        
        // Get password as bytes - Must use UTF-8 !
        byte[] passwordBytes = new String(password).getBytes("UTF-8");
        
        // Encrypt the password
        Cipher cipher = makeCipherWithKey(key, Cipher.ENCRYPT_MODE);
        byte[] encrypted = cipher.doFinal(passwordBytes);
        
        // Store in properties file as a Base64 encoded string
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
    
    public char[] getPassword() throws IOException, GeneralSecurityException {
        if(hasPassword()) {
            // Get the primary key in order to decrypt it
            SecretKey key = getPrimaryKey();
            if(key == null) {
                return "".toCharArray();
            }
            
            // Decode password from Base64 string in properties first
            String pw = getProperties().getProperty(PASSWORD, "");
            byte[] passwordBytes = null;

            try {
                passwordBytes = Base64.getDecoder().decode(pw);
            }
            catch(IllegalArgumentException ex) {
                throw new GeneralSecurityException(ex);
            }
            
            // Decrypt the password
            Cipher cipher = makeCipherWithKey(key, Cipher.DECRYPT_MODE);
            passwordBytes = cipher.doFinal(passwordBytes);
            
            // Use UTF-8 because we used that to encrypt it
            return new String(passwordBytes, "UTF-8").toCharArray();
        }
        
        return "".toCharArray();
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
    
    /**
     * The primary key used to encrypt passwords in repository credentials files
     */
    private static SecretKey primaryKey;
    
    /**
     * Ensure the primary key is set and ask user for it if not set
     */
    public static boolean checkPrimaryKeySet() throws GeneralSecurityException, IOException {
        return getPrimaryKey() != null;
    }
    
    /**
     * Check wether the primary key is set
     */
    public static boolean isPrimaryKeySet() {
        return primaryKey != null;
    }
    
    /**
     * @return The primary key file
     */
    public static File getPrimaryKeyFile() {
        return new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(), PRIMARY_KEY_FILE);
    }
    
    /**
     * Create and save a new primary key
     */
    public static void createNewPrimaryKey(char[] password) throws GeneralSecurityException, IOException {
        primaryKey = generatePrimaryKey();
        savePrimaryKey(primaryKey, password);
    }
    
    /**
     * Set a new password for the existing primary key
     */
    public static void setNewPasswordForPrimaryKey(char[] oldPassword, char[] newPassword) throws GeneralSecurityException, IOException {
        // If it exists load and save
        SecretKey key = loadPrimaryKey(oldPassword);
        if(key != null) {
            savePrimaryKey(key, newPassword);
        }
    }
    
    /**
     * Get the primary key.
     * If it is not loaded, load it from file else ask user to create a new one.
     */
    private static SecretKey getPrimaryKey() throws GeneralSecurityException, IOException {
        if(primaryKey == null) {
            File primaryKeyFile = getPrimaryKeyFile();
            
            // If the key file exists just ask user for password and load it
            if(primaryKeyFile.exists()) {
                char[] password = askUserForPrimaryPassword();
                if(password != null) {
                    primaryKey = loadPrimaryKey(password);
                }
            }
            // Else create a new file key with password
            else {
                askUserToCreatePrimaryPassword();
            }
        }
        
        return primaryKey;
    }
    
    /**
     * Load the primary key 
     * Return null if not present
     */
    private static SecretKey loadPrimaryKey(char[] password) throws GeneralSecurityException, IOException {
        File file = getPrimaryKeyFile();
        
        if(file.exists()) {
            byte[] bytes = null;
            
            // Read in all bytes
            bytes = Files.readAllBytes(file.toPath());
            
            if(bytes != null) {
                // Get the salt from the first 8 bytes
                byte[] salt = Arrays.copyOfRange(bytes, 0, 8);
                
                // Get the remaining encrypted key bytes
                byte[] keybytes = Arrays.copyOfRange(bytes, 8, bytes.length);
                
                // Decrypt the key bytes with the password and salt
                Cipher cipher = makeCipherWithPassword(password, Cipher.DECRYPT_MODE, salt);
                keybytes = cipher.doFinal(keybytes);
                
                // Return the key
                return new SecretKeySpec(keybytes, "AES");
            }
        }
        
        return null;
    }
    
    /**
     * Save the primary key to file encrypted with a password
     */
    private static void savePrimaryKey(SecretKey key, char[] password) throws GeneralSecurityException, IOException {
        // Generate a new random salt
        byte[] salt = generateSalt();
        
        // Encrypt the key
        Cipher cipher = makeCipherWithPassword(password, Cipher.ENCRYPT_MODE, salt);
        byte[] keybytes = cipher.doFinal(key.getEncoded());
        
        // Save it
        try(FileOutputStream fos = new FileOutputStream(getPrimaryKeyFile())) {
            // Store the password salt first
            fos.write(salt);
            
            // Then store the encypted key
            fos.write(keybytes);
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
    private static Cipher makeCipherWithPassword(char[] password, int mode, byte[] salt) throws GeneralSecurityException {
        // We have to convert the password characters to Base64 characters because PBEKey class will not accept non-Ascii characters in a password
        byte[] passwordBytes = new String(password).getBytes();
        String encoded = Base64.getEncoder().encodeToString(passwordBytes);
        
        // Use a KeyFactory to derive the corresponding key from the password
        PBEKeySpec keySpec = new PBEKeySpec(encoded.toCharArray());
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
    
    private static char[] askUserForPrimaryPassword() {
        // Check that current thread is the UI thread in case this is called from a non-UI thread
        if(Display.getCurrent() != null) {
            PrimaryPasswordDialog dialog = new PrimaryPasswordDialog(Display.getCurrent().getActiveShell());
            if(dialog.open() == Window.OK) {
                return dialog.getValue().toCharArray();
            }
        }
        
        return null;
    }
    
    private static void askUserToCreatePrimaryPassword() {
        // Check that current thread is the UI thread in case this is called from a non-UI thread
        if(Display.getCurrent() != null) {
            NewPrimaryPasswordDialog dialog = new NewPrimaryPasswordDialog(Display.getCurrent().getActiveShell());
            dialog.open();
        }
    }
}
