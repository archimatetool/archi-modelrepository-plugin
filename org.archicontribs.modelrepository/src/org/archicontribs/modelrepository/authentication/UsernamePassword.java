/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.authentication;


/**
 * Username and Password pair
 * 
 * @author Phillip Beauvoir
 */
public class UsernamePassword {

    private String username;
    private char[] password;
    
    public UsernamePassword(String username, char[] password) {
        this.username = username;
        this.password = password != null ? password.clone() : null;
    }
    
    public UsernamePassword(String username, String password) {
        this(username, password != null ? password.toCharArray() : null);
    }

    public char[] getPassword() {
        return password;
    }
    
    public String getUsername() {
        return username;
    }
}
