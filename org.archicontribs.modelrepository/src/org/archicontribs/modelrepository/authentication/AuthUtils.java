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

/**
 * AuthUtils
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class AuthUtils {
    
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

}
