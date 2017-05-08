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

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * Simple Credentials Storage
 * 
 * @author Phillip Beauvoir
 */
public class SimpleCredentialsStorage {
    
    private File localGitFolder;
    
    public SimpleCredentialsStorage(File localGitFolder) {
        this.localGitFolder = localGitFolder;
    }

    public void store(String userName, String password) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        Writer out = new OutputStreamWriter(new FileOutputStream(getCredentialsFile()));
        out.append(encrypt(userName) + "\n"); //$NON-NLS-1$
        out.append(encrypt(password));
        out.close();
    }
    
    public String getUserName() throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(getCredentialsFile()));
        String str = in.readLine();
        in.close();
        return decrypt(str);
    }
    
    public String getUserPassword() throws IOException {
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
        File gitFolder = new File(localGitFolder, ".git"); //$NON-NLS-1$
        return new File(gitFolder, "credentials"); //$NON-NLS-1$
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
