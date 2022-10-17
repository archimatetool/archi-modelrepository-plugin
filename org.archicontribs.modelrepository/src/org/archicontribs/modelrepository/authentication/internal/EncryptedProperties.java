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
import java.security.GeneralSecurityException;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.DestroyFailedException;

import org.archicontribs.modelrepository.authentication.CryptoData;
import org.archicontribs.modelrepository.authentication.CryptoUtils;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.ArchiPlugin;
import com.archimatetool.editor.utils.StringUtils;

/**
 * Encrypted Properties using a SecureKey
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public final class EncryptedProperties {
    
    // Used for encrypting passwords
    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    
    // IV length has to be 12 bytes (96 bits) for GCM
    private static final int GCM_IV_LENGTH = 12;
    
    // Used for PBE of primary key
    private static final String PBE_ALGORITHM = "PBEwithHmacSHA256AndAES_256";
    
    // Number of iterations for PBEKeySpec and PBEParameterSpec
    private static final int PBE_ITERATIONS = 1000;

    // Salt length for PBE of primary key
    private static final int PBE_SALT_LENGTH = 8;
    
    // IV length for PBE of primary key - has to be 16
    private static final int PBE_IV_LENGTH = 16;
    
    private static final String ENCRYPTED_VALUE_PREFIX = "ENC(";
    private static final String ENCRYPTED_VALUE_SUFFIX = ")";
    
    private static final String PRIMARY_KEY = "primaryKey";

    private File fStorageFile;
    private Properties fProperties;
    
    private SecretKey primaryKey;
    
    private static EncryptedProperties defaultProps;
    
    public static EncryptedProperties getDefault() {
        if(defaultProps == null) {
            defaultProps = new EncryptedProperties(new File(ArchiPlugin.INSTANCE.getUserDataFolder(), "secure_storage"));
        }
        return defaultProps;
    }

    public EncryptedProperties(File storageFile) {
        fStorageFile = storageFile;
    }
    
    // ==============================================================================================
    // Testing
    // ==============================================================================================

    public static void main(String[] args) {
        File file = null;
        
        try {
            file = File.createTempFile("props", null);
            EncryptedProperties props = new EncryptedProperties(file);
            
            String key1 = "repo/https://www.github.com/myrepo.git/user";
            props.setProperty(key1, "UserName");
            System.out.println(props.getProperty(key1));
            
            String key2 = "repo/https://www.github.com/myrepo.git/password";
            props.setSecureProperty(key2, "password".toCharArray());

            props.setNewPasswordForPrimaryKey("testPassword".toCharArray(), "testPassword".toCharArray());
            
            char[] val = props.getSecureProperty(key2);
            System.out.println(val);
            
            // Should be null
            props.setProperty(PRIMARY_KEY, "something");
            props.setSecureProperty(PRIMARY_KEY, "something".toCharArray());
            System.out.println(props.getProperty(PRIMARY_KEY));
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        finally {
            if(file != null) {
                file.delete();
            }
        }
    }
    
    
    // ==============================================================================================
    // Properties Storage
    // ==============================================================================================
    
    public void setProperty(String key, String value) throws IOException {
        if(PRIMARY_KEY.equals(key)) {
            return;
        }
        
        // If value not set remove it
        if(!StringUtils.isSet(value)) {
            getProperties().remove(key);
        }
        else {
            getProperties().setProperty(key, value);
        }
        
        saveProperties();
    }
    
    public void setSecureProperty(String key, char[] value) throws IOException, GeneralSecurityException {
        if(PRIMARY_KEY.equals(key)) {
            return;
        }
        
        // If value not set remove it
        if(value == null || value.length == 0) {
            getProperties().remove(key);
        }
        else {
            // Get the primary key
            SecretKey primaryKey = getPrimaryKey();
            if(primaryKey == null) {
                throw new IOException("No primary key");
            }
            
            // Generate a new random iv for the Cipher
            byte[] iv = CryptoUtils.generateRandomBytes(GCM_IV_LENGTH);

            // Get chars as bytes - Must use UTF-8
            byte[] bytes = CryptoUtils.convertCharsToBytes(value);
            
            // Encrypt the bytes
            byte[] encrypted = CryptoUtils.transformWithKey(primaryKey, CIPHER_ALGORITHM, Cipher.ENCRYPT_MODE, bytes, iv);
            
            // Store in properties file as a Base64 encoded string
            CryptoData cd = new CryptoData(null, iv, encrypted);
            getProperties().setProperty(key, ENCRYPTED_VALUE_PREFIX + cd.getBase64String() + ENCRYPTED_VALUE_SUFFIX); // Use Base64 because this is a string
        }
        
        saveProperties();
    }
    
    public String getProperty(String key) throws IOException {
        if(PRIMARY_KEY.equals(key) || !StringUtils.isSet(key)) {
            return null;
        }
        
        String value = getProperties().getProperty(key);
        if(isEncryptedValue(value)) {
            return null;
        }
        
        return value;
    }

    public char[] getSecureProperty(String key) throws GeneralSecurityException, IOException {
        if(PRIMARY_KEY.equals(key) || !StringUtils.isSet(key)) {
            return null;
        }
        
        String value = getProperties().getProperty(key);
        
        // Encrypted Base64 String
        if(isEncryptedValue(value)) {
            // Get the primary key in order to decrypt it
            SecretKey primaryKey = getPrimaryKey();
            if(key == null) {
                throw new IOException("No primary key");
            }
            
            // Remove prefix and suffix from String
            value = getEncryptedValue(value);
            
            // Decode it
            CryptoData cd = new CryptoData(value);
            
            // Get iv
            byte[] iv = cd.getIV();
            
            // Get bytes
            byte[] bytes = cd.getEncryptedData();
            
            if(iv == null || bytes == null) {
                throw new GeneralSecurityException("Could not get property for: " + key);
            }

            // Decrypt the bytes
            bytes = CryptoUtils.transformWithKey(primaryKey, CIPHER_ALGORITHM, Cipher.DECRYPT_MODE, bytes, iv);
            
            // Convert back to UTF-8 chars
            return CryptoUtils.convertBytesToChars(bytes);
        }
        
        return null;
    }
    
    public void removeProperty(String key) throws IOException {
        if(!PRIMARY_KEY.equals(key)) {
            getProperties().remove(key);
            saveProperties();
        }
    }
    
    public boolean hasProperty(String key) throws IOException {
        return !PRIMARY_KEY.equals(key) && getProperties().containsKey(key);
    }
    
    public boolean hasPropertiesFile() {
        return getPropertiesFile().exists();
    }
    
    private File getPropertiesFile() {
        return fStorageFile;
    }
    
    private Properties getProperties() throws IOException {
        if(fProperties == null) {
            fProperties = new Properties();
            
            if(hasPropertiesFile()) {
                try(FileInputStream is = new FileInputStream(getPropertiesFile())) {
                    fProperties.load(is);
                }
            }
        }
        
        return fProperties;
    }
    
    private void saveProperties() throws IOException {
        File file = getPropertiesFile();
        file.getParentFile().mkdirs(); // Ensure parent folder exists
            
        try(FileOutputStream out = new FileOutputStream(file)) {
            getProperties().store(out, null);
        }
    }
    
    private boolean isEncryptedValue(String value) {
        if(!StringUtils.isSet(value)) {
            return false;
        }
        value = value.trim();
        return value.startsWith(ENCRYPTED_VALUE_PREFIX) && value.endsWith(ENCRYPTED_VALUE_SUFFIX);
    }
    
    private String getEncryptedValue(String value) {
        return value.substring(ENCRYPTED_VALUE_PREFIX.length(), value.length() - ENCRYPTED_VALUE_SUFFIX.length());
    }


    // ==============================================================================================
    // Primary Key Storage
    // ==============================================================================================
    
    /**
     * Ensure the primary key is set and ask user for it if not set
     */
    public boolean checkPrimaryKeySet() throws IOException, GeneralSecurityException {
        return getPrimaryKey() != null;
    }
    
    /**
     * Check whether the primary key is set
     */
    public boolean isPrimaryKeySet() {
        return primaryKey != null;
    }
    
    /**
     * Create and save a new primary key
     */
    public void createNewPrimaryKey(char[] password) throws GeneralSecurityException, IOException {
        clearPrimaryKey();
        primaryKey = CryptoUtils.generateRandomSecretKey();
        storePrimaryKey(primaryKey, password);
    }
    
    public void clearPrimaryKey() {
        if(primaryKey != null) {
            try {
                primaryKey.destroy();
            }
            catch(DestroyFailedException ex) {
                ex.printStackTrace();
            }
            
            primaryKey = null;
        }
    }

    private SecretKey getPrimaryKey() throws IOException, GeneralSecurityException {
        // For testing
        if(primaryKey == null) {
            char[] password = "testPassword".toCharArray();
            if(password != null) {
                primaryKey = loadPrimaryKey(password);
                
                if(primaryKey == null) {
                    createNewPrimaryKey(password);
                }
            }
        }
        
        // Ask user
        if(primaryKey == null) {
            char[][] password = new char[1][];
            
            // Ask user for this in UI thread in case this is called from non-UI thread
            PlatformUI.getWorkbench().getDisplay().syncExec(() -> {
                InputDialog dialog = new InputDialog(null, "Enter password", "Password:", "", null) {
                    @Override
                    protected int getInputTextStyle() {
                        return super.getInputTextStyle() | SWT.PASSWORD;
                    }
                };
                
                if(dialog.open() == Window.OK) {
                    password[0] = dialog.getValue().toCharArray();
                }
            });
            
            if(password[0] != null) {
                primaryKey = loadPrimaryKey(password[0]);
                
                if(primaryKey == null) {
                    createNewPrimaryKey(password[0]);
                }
            }
        }
        
        return primaryKey;
    }
    
    /**
     * Set a new password for the existing primary key
     */
    public void setNewPasswordForPrimaryKey(char[] oldPassword, char[] newPassword) throws GeneralSecurityException, IOException {
        // If it exists load it
        SecretKey key = loadPrimaryKey(oldPassword);
        if(key != null) {
            storePrimaryKey(key, newPassword);
        }
    }

    /**
     * Load the primary key 
     * Return null if not present
     */
    private SecretKey loadPrimaryKey(char[] password) throws GeneralSecurityException, IOException {
        // Read in the Base64 encoded String
        String encoded = getProperties().getProperty(PRIMARY_KEY);

        if(encoded != null) {
            CryptoData cd = new CryptoData(encoded);

            byte[] salt = cd.getSalt();
            byte[] iv = cd.getIV();
            byte[] keybytes = cd.getEncryptedData();

            if(salt == null || iv == null || keybytes == null) {
                throw new GeneralSecurityException("Bad Primary Key format");
            }

            // Decrypt the key bytes
            keybytes = CryptoUtils.transformWithPassword(password,
                                                         PBE_ALGORITHM,
                                                         Cipher.DECRYPT_MODE,
                                                         keybytes,
                                                         salt,
                                                         iv,
                                                         PBE_ITERATIONS);

            // Return the key
            return new SecretKeySpec(keybytes, "AES");
        }
        
        return null;
    }
    
    /**
     * Save the primary key to file encrypted with a password
     */
    private void storePrimaryKey(SecretKey key, char[] password) throws GeneralSecurityException, IOException {
        // Generate a new random salt
        byte[] salt = CryptoUtils.generateRandomBytes(PBE_SALT_LENGTH);
        
        // Generate a new random iv - has to be 16 bytes
        byte[] iv = CryptoUtils.generateRandomBytes(PBE_IV_LENGTH);

        // Encrypt the key
        byte[] keybytes = CryptoUtils.transformWithPassword(password,
                                                            PBE_ALGORITHM,
                                                            Cipher.ENCRYPT_MODE,
                                                            key.getEncoded(),
                                                            salt,
                                                            iv,
                                                            PBE_ITERATIONS);
        
        // Save it
        CryptoData cd = new CryptoData(salt, iv, keybytes);
        getProperties().setProperty(PRIMARY_KEY, cd.getBase64String());
        saveProperties();
    }
    
}
