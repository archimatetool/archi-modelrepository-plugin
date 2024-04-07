/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
public class CryptoDataTests {
    
    char[] secretText = "SecretTextToEncode".toCharArray();;
    byte[] secretBytes = CryptoUtils.convertCharsToBytes(secretText); // These would be encrypted with cipher
    
    byte[] salt = CryptoUtils.generateRandomBytes(8);
    byte[] iv = CryptoUtils.generateRandomBytes(8);
    
    /**
     * No Salt or IV
     */
    @Test
    public void noSaltIV() {
        CryptoData cd = new CryptoData(null, null, secretBytes);
        String encoded = cd.getBase64String();
        
        assertFalse(encoded.contains(",")); // no salt sep
        assertFalse(encoded.contains(";")); // no iv sep
        
        cd = new CryptoData(encoded);
        
        assertNull(cd.getSalt());
        assertNull(cd.getIV());
        assertArrayEquals(secretBytes, cd.getEncryptedData());
    }

    /**
     * Just Salt
     */
    @Test
    public void justSalt() {
        CryptoData cd = new CryptoData(salt, null, secretBytes);
        String encoded = cd.getBase64String();
        
        assertTrue(encoded.contains(","));  // has salt sep
        assertFalse(encoded.contains(";")); // no iv sep
        
        cd = new CryptoData(encoded);
        
        assertArrayEquals(salt, cd.getSalt());
        assertNull(cd.getIV());
        assertArrayEquals(secretBytes, cd.getEncryptedData());
    }
    
    /**
     * Just IV
     */
    @Test
    public void justIV() {
        CryptoData cd = new CryptoData(null, iv, secretBytes);
        String encoded = cd.getBase64String();
        
        assertFalse(encoded.contains(",")); // no salt sep
        assertTrue(encoded.contains(";"));  // has iv sep
        
        cd = new CryptoData(encoded);
        
        assertNull(cd.getSalt());
        assertArrayEquals(iv, cd.getIV());
        assertArrayEquals(secretBytes, cd.getEncryptedData());
    }
    
    /**
     * Salt and IV
     */
    @Test
    public void saltIV() {
        CryptoData cd = new CryptoData(salt, iv, secretBytes);
        String encoded = cd.getBase64String();
        
        assertTrue(encoded.contains(","));  // has salt sep
        assertTrue(encoded.contains(";"));  // has iv sep
        
        cd = new CryptoData(encoded);
        
        assertArrayEquals(salt, cd.getSalt());
        assertArrayEquals(iv, cd.getIV());
        assertArrayEquals(secretBytes, cd.getEncryptedData());
    }

}
