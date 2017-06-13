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

    private String fUsername;
    private String fPassword;
    
    public UsernamePassword(String username, String password) {
        fUsername = username;
        fPassword = password;
    }
    
    public String getPassword() {
        return fPassword;
    }
    
    public String getUsername() {
        return fUsername;
    }
}
