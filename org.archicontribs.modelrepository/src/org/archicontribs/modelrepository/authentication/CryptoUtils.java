/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;

/**
 * CryptoUtils
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class CryptoUtils {
    
    /**
     * Convert char array to byte array without using intermediate String
     * chars are converted to UTF-8
     */
    public static byte[] convertCharsToBytes(char[] chars) {
        ByteBuffer buf = Charset.forName("UTF-8").encode(CharBuffer.wrap(chars.clone()));
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        Arrays.fill(buf.array(), (byte) 0); // clear ByteBuffer
        return bytes;
    }

    /**
     * Convert byte array to char array without using intermediate String
     * chars are converted to UTF-8
     */
    public static char[] convertBytesToChars(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes.clone());
        CharBuffer charBuffer = Charset.forName("UTF-8").decode(buf);
        char[] charArray = new char[charBuffer.remaining()];
        charBuffer.get(charArray);  
        Arrays.fill(buf.array(), (byte) 0); // clear ByteBuffer
        return charArray;
    }

    /**
     * Encode a char array to a Base64 encoded char array
     * the chars are converted to UTF-8
     */
    public static char[] encodeCharsToBase64(char[] chars) {
        byte[] bytes = convertCharsToBytes(chars); // convert to bytes using UTF-8
        String encoded = Base64.getEncoder().encodeToString(bytes); // Encode bytes to Base64 String
        return encoded.toCharArray(); // Return string chars
    }

}
