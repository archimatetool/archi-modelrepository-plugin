/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.CryptoUtils;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.dialogs.NewPrimaryPasswordDialog;
import org.archicontribs.modelrepository.dialogs.PrimaryPasswordDialog;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
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

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    
    private static long PRIMARY_PASSWORD_TIMEOUT = 
            Math.max(ModelRepositoryPlugin.getInstance().getPreferenceStore().getInt(IPreferenceConstants.PREFS_PRIMARY_PASSWORD_TIMEOUT) * 1000 * 60, 0);
    private static long PRIMARY_PASSWORD_TIMEOUT_MARK = 0;
    
    private static long PASSWORD_INACTIVITY_TIMEOUT =
            Math.max(ModelRepositoryPlugin.getInstance().getPreferenceStore().getInt(IPreferenceConstants.PREFS_PASSWORD_INACTIVITY_TIMEOUT) * 1000 * 60, 0);
    private static long PASSWORD_INACTIVITY_TIMEOUT_MARK = 0;

    
    // =========================== CIPHER STUFF ==========================================
    
    // Used for encrypting passwords
    private static final String CIPHER_ALGORITHM = "AES";
    
    // Used for PBE of primary key
    private static final String PBE_ALGORITHM = "PBEWithMD5AndDES";
    
    // Number of iterations for PBEKeySpec and PBEParameterSpec
    private static final int PBE_ITERATIONS = 28;

    // Salt length for PBE of primary key
    private static final int PBE_SALT_LENGTH = 8;
    
 // =======================================================================================
    
    /**
     * Convenience method to create new a EncryptedCredentialsStorage for a repository
     */
    public static EncryptedCredentialsStorage forRepository(IArchiRepository repository) {
        return new EncryptedCredentialsStorage(new File(repository.getLocalGitFolder(), IGraficoConstants.REPO_CREDENTIALS_FILE));
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
        byte[] passwordBytes = CryptoUtils.convertCharsToBytes(password);
        
        // Encrypt the password
        byte[] encrypted = CryptoUtils.transformWithKey(key, CIPHER_ALGORITHM, Cipher.ENCRYPT_MODE, passwordBytes, null);
        
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
                return new char[0];
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
            passwordBytes = CryptoUtils.transformWithKey(key, CIPHER_ALGORITHM, Cipher.DECRYPT_MODE, passwordBytes, null);
            
            // Use UTF-8 because we used that to encrypt it
            return CryptoUtils.convertBytesToChars(passwordBytes);
        }
        
        return new char[0];
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
            credentialsFile.getParentFile().mkdirs(); // Ensure parent folder exists
            
            try(FileOutputStream out = new FileOutputStream(credentialsFile)) {
                fProperties.store(out, null);
            }
        }
        // If not delete the file
        else {
            credentialsFile.delete();
        }
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
        return new File(ModelRepositoryPlugin.getInstance().getUserModelRepositoryFolder(), PRIMARY_KEY_FILE);
    }
    
    /**
     * Create and save a new primary key
     */
    public static void createNewPrimaryKey(char[] password) throws GeneralSecurityException, IOException {
        primaryKey = CryptoUtils.generateRandomSecretKey();
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
        // If the primary password has not been entered since last login set key to null
        if(PRIMARY_PASSWORD_TIMEOUT != 0 && System.currentTimeMillis() - PRIMARY_PASSWORD_TIMEOUT_MARK > PRIMARY_PASSWORD_TIMEOUT) {
            primaryKey = null;
        }
        
        // If inactivity on password since last time set key to null
        if(PASSWORD_INACTIVITY_TIMEOUT != 0 && System.currentTimeMillis() - PASSWORD_INACTIVITY_TIMEOUT_MARK > PASSWORD_INACTIVITY_TIMEOUT) {
            primaryKey = null;
        }

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

            PRIMARY_PASSWORD_TIMEOUT_MARK = System.currentTimeMillis();
        }
        
        PASSWORD_INACTIVITY_TIMEOUT_MARK = System.currentTimeMillis();
        
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
                byte[] salt = Arrays.copyOfRange(bytes, 0, PBE_SALT_LENGTH);
                
                // Get the remaining encrypted key bytes
                byte[] keybytes = Arrays.copyOfRange(bytes, PBE_SALT_LENGTH, bytes.length);
                
                // Decrypt the key bytes with the password and salt
                keybytes = CryptoUtils.transformWithPassword(password,
                                                             PBE_ALGORITHM,
                                                             Cipher.DECRYPT_MODE,
                                                             keybytes,
                                                             salt,
                                                             null, // no iv
                                                             PBE_ITERATIONS);
                
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
        byte[] salt = CryptoUtils.generateRandomBytes(PBE_SALT_LENGTH);
        
        // Encrypt the key
        byte[] keybytes = CryptoUtils.transformWithPassword(password,
                                                            PBE_ALGORITHM,
                                                            Cipher.ENCRYPT_MODE,
                                                            key.getEncoded(),
                                                            salt,
                                                            null, // no iv
                                                            PBE_ITERATIONS);
        
        File primaryKeyFile = getPrimaryKeyFile();
        
        // Ensure parent folder exists
        File parentFolder = primaryKeyFile.getParentFile();
        if(parentFolder != null) {
            parentFolder.mkdirs();
        }
        
        // Save it
        try(FileOutputStream fos = new FileOutputStream(primaryKeyFile)) {
            // Store the password salt first
            fos.write(salt);
            
            // Then store the encypted key
            fos.write(keybytes);
        }
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
