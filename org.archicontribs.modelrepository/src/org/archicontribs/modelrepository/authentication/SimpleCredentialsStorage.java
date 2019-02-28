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
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;


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

    public void store(UsernamePassword npw) throws IOException, NoSuchAlgorithmException {
        store(npw.getUsername(), npw.getPassword());
    }

    public void store(String userName, String password) throws IOException, NoSuchAlgorithmException {
        File file = getCredentialsFile();
        file.getParentFile().mkdirs();
        
        Writer out = new OutputStreamWriter(new FileOutputStream(file));
        out.append(encrypt(userName) + "\n"); //$NON-NLS-1$
        out.append(encrypt(password));
        out.close();
    }
    
    public UsernamePassword getUsernamePassword() throws IOException {
        return new UsernamePassword(getUsername(), getPassword());
    }

    private String getUsername() throws IOException {
        if(!hasCredentialsFile()) {
            return ""; //$NON-NLS-1$
        }
        
        try(BufferedReader in = new BufferedReader(new FileReader(getCredentialsFile()))) {
            String str = in.readLine();
            return decrypt(str);
        }
    }
    
    private String getPassword() throws IOException {
        if(!hasCredentialsFile()) {
            return ""; //$NON-NLS-1$
        }
        
        try(BufferedReader in = new BufferedReader(new FileReader(getCredentialsFile()))) {
            in.readLine();
            String str = in.readLine();
            return decrypt(str);
        }
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
    
    private String encrypt(String str) throws NoSuchAlgorithmException {
        Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(generateSalt()) + encoder.encodeToString(str.getBytes());
    }
    
    private String decrypt(String encstr) {
        if(encstr != null && encstr.length() > 12) {
            String cipher = encstr.substring(12);
            Decoder decoder = Base64.getDecoder();
            return new String(decoder.decode(cipher));
        }

        return ""; //$NON-NLS-1$
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
