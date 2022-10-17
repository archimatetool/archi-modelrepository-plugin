package org.archicontribs.modelrepository.authentication.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.archicontribs.modelrepository.authentication.CryptoData;
import org.archicontribs.modelrepository.authentication.CryptoUtils;


@SuppressWarnings("nls")
public class TestCrypto {
    
    
    /**
     * Number of iterations for PBEKeySpec and PBEParameterSpec
     * Once this has been used to encode, the same value must be used to decode
     */
    static final int PBE_ITERATIONS = 1000;


    public static void main(String[] args) {
        TestCrypto tc = new TestCrypto();
        tc.doTests();
    }
    
    void doTests() {
        try {
            testEncryptDecryptWithKey();
            
            for(String algorithm : CryptoUtils.PBK_ALGORITHMS) {
                testEncryptDecryptWithPasswordKey(algorithm);
            }
            
            for(String algorithm : CryptoUtils.PBE_ALGORITHMS) {
                testEncryptDecryptWithPBEKey(algorithm);
            }

            testKeyStore();
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
    
    // ================================== SIMPLE KEY =====================================
    
    // Encrypt the text with a secure random key.
    // The key will need to be saved somewhere securely, perhaps in the same file, and encrypted with a password and salt (PBE)
    // The iv should be generated for each text and  saved with the encypted text
    // The advantage is that if the password to the key changes, any strings encrypted with it don't need a new key.
    
    void testEncryptDecryptWithKey() throws Exception {
        char[] secretText = "SecretTextToEncode".toCharArray();
        
        // Get text as bytes - Must use UTF-8 !
        byte[] secretBytes = CryptoUtils.convertCharsToBytes(secretText);
        
        // This will need to be stored with the encrypted text
        byte[] iv = CryptoUtils.generateRandomBytes(CryptoUtils.GCM_IV_LENGTH);
        
        // Generate a key used to encrypt/decrypt
        SecretKey key = CryptoUtils.generateRandomSecretKey();
        
        // Password used to encrypt the key
        char[] keyPassword = "keyPassword".toCharArray();
        
        // ============ ENCRYPT AND SAVE THE KEY =============
        
        String keyEnryptionString = encryptSecretKeyWithPassword(keyPassword, key);
        // Save this somewhere...
        
        
        // ============ ENCRYPT THE DATA =============
        
        byte[] encryptedBytes = CryptoUtils.transformWithKey(key,
                                                             CryptoUtils.AES_GCM,
                                                             Cipher.ENCRYPT_MODE,
                                                             secretBytes,
                                                             iv);
        
        // Save the encryptedBytes as a Base64 string to file...
        CryptoData cryptoData = new CryptoData(null, iv, encryptedBytes);
        String stringSavedTofile = cryptoData.getBase64String();
        

        // ============ LOAD AND DECRYPT THE KEY =============

        // Load keyEnryptionString from somewhere 
        key = decryptSecretKeyWithPassword(keyPassword, keyEnryptionString);
        
        
        // ============ DECRYPT THE DATA =============
        
        // Read String from file, decode from Base64 and get iv and bytes
        cryptoData = new CryptoData(stringSavedTofile);
        iv = cryptoData.getIV();
        encryptedBytes = cryptoData.getEncryptedData();
        
        // Decrypt
        byte[] decryptedBytes = CryptoUtils.transformWithKey(key,
                                                             CryptoUtils.AES_GCM,
                                                             Cipher.DECRYPT_MODE,
                                                             encryptedBytes,
                                                             iv);

        // Get chars from bytes
        char[] decryptedChars = CryptoUtils.convertBytesToChars(decryptedBytes);
        
        // ============ COMPARE =============
        
        if(!Arrays.equals(decryptedChars, secretText)) {
            System.out.println("Secret text should be the same");
        }
    }
    
    
    /**
     * Encrypt a secret key with a password
     */
    String encryptSecretKeyWithPassword(char[] password, SecretKey key) throws Exception {
        // Generate a new random salt
        byte[] salt = CryptoUtils.generateRandomBytes(8);
        
        // Generate a new random iv - has to be 16 bytes
        byte[] iv = CryptoUtils.generateRandomBytes(16);

        // Encrypt the key bytes
        byte[] keybytes = CryptoUtils.transformWithPassword(password,
                                                            "PBEwithHmacSHA256AndAES_256",
                                                            Cipher.ENCRYPT_MODE, 
                                                            key.getEncoded(),
                                                            salt,
                                                            iv,
                                                            PBE_ITERATIONS);

        // Convert to Base64 String
        CryptoData cryptoData = new CryptoData(salt, iv, keybytes);
        return cryptoData.getBase64String();
    }
    
    /**
     * Decrypt a secret key with a password
     */
    SecretKey decryptSecretKeyWithPassword(char[] password, String keyEnryptionString) throws Exception {
        CryptoData cryptoData = new CryptoData(keyEnryptionString);
        
        // Salt
        byte[] salt = cryptoData.getSalt();
        
        // IV
        byte[] iv = cryptoData.getIV();

        // Encrypted bytes
        byte[] keybytes = cryptoData.getEncryptedData();
        
        // Decrypt key bytes
        keybytes = CryptoUtils.transformWithPassword(password,
                                                     "PBEwithHmacSHA256AndAES_256",
                                                     Cipher.DECRYPT_MODE,
                                                     keybytes,
                                                     salt,
                                                     iv,
                                                     PBE_ITERATIONS);
        
        // Create the key from the bytes
        return new SecretKeySpec(keybytes, "AES");
    }

    
    
    // ================================== PASSWORD KEY, MORE SECURE ALGORITHM =====================================
    
    // Encrypt the text with a key secured by password
    // using a strong GCM algorithm for Cipher and PBK for the key
    // It needs a salt for the password and an iv for the Cipher.
    // The key is regenerated from the password and salt, so no need to store the key.
    // The salt and iv are stored with the encrypted text.
    
    // Alternatively, as there is only one salt and iv
    // store the salt and iv in one encoded string and the encrypted text(s) in another
    
    // However, if the password is changed a new key will be returned and all encrypted strings
    // will need to be retrieved with the old password and re-encrypted with the new key.
    // This works if all the encrypted strings are in the same file.
    
    // I think this is how Eclipse does it.
    
    void testEncryptDecryptWithPasswordKey(String pbkAlgorithm) throws Exception {
        char[] secretText = "SecretTextToEncode".toCharArray();
        
        // Get text as bytes - Must use UTF-8!
        byte[] secretBytes = CryptoUtils.convertCharsToBytes(secretText);
        
        char[] password = "HelloÃẼĨ££ÕŨÑṼ".toCharArray(); // Have some non-ascii characters
        
        
        // ============ ENCRYPT =============
        
        // These should be stored somewhere
        byte[] salt = CryptoUtils.generateRandomBytes(100);
        byte[] iv = CryptoUtils.generateRandomBytes(CryptoUtils.GCM_IV_LENGTH);
        
        byte[] encryptedBytes = CryptoUtils.transformWithPassword2(password,
                                                                   pbkAlgorithm,
                                                                   CryptoUtils.AES_GCM,
                                                                   Cipher.ENCRYPT_MODE,
                                                                   secretBytes,
                                                                   salt,
                                                                   iv,
                                                                   PBE_ITERATIONS);
        
        // Save the encryptedBytes as a Base64 string to file...
        CryptoData cryptoData = new CryptoData(salt, iv, encryptedBytes);
        String stringSavedTofile = cryptoData.getBase64String();
        

        // ============ DECRYPT =============
        
        // Read String from file, decode from Base64 and get salt and iv and bytes
        cryptoData = new CryptoData(stringSavedTofile);
        salt = cryptoData.getSalt();
        iv = cryptoData.getIV();
        encryptedBytes = cryptoData.getEncryptedData();
        
        byte[] decryptedBytes = CryptoUtils.transformWithPassword2(password,
                                                                   pbkAlgorithm,
                                                                   CryptoUtils.AES_GCM,
                                                                   Cipher.DECRYPT_MODE,
                                                                   encryptedBytes,
                                                                   salt,
                                                                   iv,
                                                                   PBE_ITERATIONS);

        // Get chars from bytes
        char[] decryptedChars = CryptoUtils.convertBytesToChars(decryptedBytes);
        
        // ============ COMPARE =============
        
        Assert(Arrays.equals(decryptedChars, secretText), "Secret text should be the same");
    }
    
    
    
    
    
    // ================================== PASSWORD PBE KEY =====================================
    
    // Encrypt the text with a key that can be generated from a password.
    // This uses a weaker PBE algorithm for both the key and the Cipher.
    // The key is generated from a password so does not need to be stored anywhere.
    // However, if the password is changed a new key will be returned and all encrypted strings
    // will need to be retrieved with the old password and re-encrypted with the new key.
    // This would work if all the encrypted strings are in the same file.

    void testEncryptDecryptWithPBEKey(String pbeAlgorithm) throws Exception {
        char[] secretText = "SecretTextToEncode".toCharArray();
        
        // Get text as bytes - Must use UTF-8!
        byte[] secretBytes = CryptoUtils.convertCharsToBytes(secretText);
        
        char[] password = "HelloÃẼĨ££ÕŨÑṼ".toCharArray(); // Have some non-ascii characters
        
        // salt
        byte[] salt = CryptoUtils.generateRandomBytes(8);
        
        // Cipher algorithm requires iv ?
        byte[] iv =  CryptoUtils.usesIV(pbeAlgorithm) ? CryptoUtils.generateRandomBytes(16) : null;
        
        // ============ ENCRYPT =============
        
        // Encrypt bytes
        byte[] encryptedBytes = CryptoUtils.transformWithPassword(password,
                                                                  pbeAlgorithm,
                                                                  Cipher.ENCRYPT_MODE,
                                                                  secretBytes,
                                                                  salt,
                                                                  iv,
                                                                  PBE_ITERATIONS);
        
        // Save encryptedBytes to String along with salt and iv if present
        // String can be written to file
        CryptoData cryptoData = new CryptoData(salt, iv, encryptedBytes);
        String stringSavedTofile = cryptoData.getBase64String();
        
        
        // ============ DECRYPT =============
        
        // Read String from file, decode from Base64 and get salt and iv and bytes
        cryptoData = new CryptoData(stringSavedTofile);
        salt = cryptoData.getSalt();
        iv = cryptoData.getIV();
        encryptedBytes = cryptoData.getEncryptedData();
        
        // Decrypt bytes
        byte[] decryptedBytes = CryptoUtils.transformWithPassword(password,
                                                                  pbeAlgorithm,
                                                                  Cipher.DECRYPT_MODE,
                                                                  encryptedBytes,
                                                                  salt,
                                                                  iv,
                                                                  PBE_ITERATIONS);
        
        // Get chars from bytes
        char[] decryptedChars = CryptoUtils.convertBytesToChars(decryptedBytes);
        
        // ============ COMPARE =============
        
        Assert(Arrays.equals(decryptedChars, secretText), "Secret text should be the same");
    }
    
   
    
    
    
    // ============================ SAVE KEY TO JAVA KEYSTORE =====================================
    
    // Generate a Secret Key and store it in a KeyStore
    
    void testKeyStore() throws Exception {
        // Key to store
        SecretKey key = CryptoUtils.generateRandomSecretKey();
        
        char[] password = "HelloÃẼĨ££ÕŨÑṼ".toCharArray(); // Have some non-ascii characters
        
        // Convert the password bytes to Base64 characters because PBEKey used by KeyStore class will not accept non-Ascii characters in a password
        char[] encodedPassword = CryptoUtils.encodeCharsToBase64(password);

        File fileLocation = File.createTempFile("keyStore", null);
        
        String entryAlias = "myKey";
        
        // Get Keystore instance
        KeyStore ks = KeyStore.getInstance("PKCS12");
        
        // Create a new KeyStore by loading it with null values
        ks.load(null, null);
        
        // Create new SecretKeyEntry with password
        SecretKeyEntry entry = new SecretKeyEntry(key);
        PasswordProtection protParam = new PasswordProtection(encodedPassword);
        ks.setEntry(entryAlias, entry, protParam);
        //ks.setKeyEntry(entryAlias, key, encodedPassword, null); // can be done like this

        // Save KeyStore
        try(FileOutputStream fos = new FileOutputStream(fileLocation)) {
            ks.store(fos, encodedPassword); // password can be null to skip integrity check
        }
        
        // Load KeyStore...
        
        // Get Keystore instance
        ks = KeyStore.getInstance("PKCS12");
        
        // Read it in
        try(FileInputStream fis = new FileInputStream(fileLocation)) {
            ks.load(fis, encodedPassword); // password can be null to skip integrity check
        }
        finally {
            fileLocation.delete(); // Delete temp file
        }
  
        // Get the entry by it's entry alias
        protParam = new PasswordProtection(encodedPassword);
        entry = (SecretKeyEntry)ks.getEntry(entryAlias, protParam);
        
        // Get the key
        SecretKey key2 = entry.getSecretKey();
        
        // Compare
        Assert(key2.equals(key), "Secret key should be the same");
    }

    
    
    
    /**
     * Test Util
     */
    void Assert(boolean cond, String message) {
        if(!cond) {
            throw new RuntimeException(message);
        }
    }
}
