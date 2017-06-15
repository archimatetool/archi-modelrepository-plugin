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

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Simple Credentials Storage for user name and password
 * 
 * @author Phillip Beauvoir
 */
public class SimpleCredentialsStorage {
    
    private File fStorageFile;
    
    public SimpleCredentialsStorage(File storageFile) {
        fStorageFile = storageFile;
    }

    public void store(String userName, String password) throws IOException, NoSuchAlgorithmException {
        Writer out = new OutputStreamWriter(new FileOutputStream(getCredentialsFile()));
        out.append(encrypt(userName) + "\n"); //$NON-NLS-1$
        out.append(encrypt(password));
        out.close();
    }
    
    public String getUsername() throws IOException {
        if(!hasCredentialsFile()) {
            return null;
        }
        
        try(BufferedReader in = new BufferedReader(new FileReader(getCredentialsFile()))) {
            String str = in.readLine();
            return decrypt(str);
        }
    }
    
    public String getPassword() throws IOException {
        if(!hasCredentialsFile()) {
            return null;
        }
        
        try(BufferedReader in = new BufferedReader(new FileReader(getCredentialsFile()))) {
            in.readLine();
            String str = in.readLine();
            return decrypt(str);
        }
    }
    
    public boolean hasCredentialsFile() {
        return getCredentialsFile().exists();
    }
    
    public boolean deleteCredentialsFile() {
        return getCredentialsFile().delete();
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
        return fStorageFile;
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
