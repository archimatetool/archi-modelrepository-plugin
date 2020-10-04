/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;


/**
 * Grafico Constants
 * 
 * @author Phillip Beauvoir
 */
public interface IGraficoConstants {
    
    /**
     * Filename for local .archimate file
     */
    String LOCAL_ARCHI_FILENAME = "temp.archimate"; //$NON-NLS-1$
    
    /**
     * Filename to use for serialization of folder elements
     */
    String FOLDER_XML = "folder.xml"; //$NON-NLS-1$
    
    /**
     * Name of folder for images
     */
    String IMAGES_FOLDER = "images"; //$NON-NLS-1$
    
    /**
     * Name of folder for model
     */
    String MODEL_FOLDER = "model"; //$NON-NLS-1$

    /**
     * File name of user name/password for each git repo
     */
    String REPO_CREDENTIALS_FILE = "credentials"; //$NON-NLS-1$
    
    /**
     * File name of SSH identity password
     */
    String SSH_CREDENTIALS_FILE = "secure_ssh_credentials"; //$NON-NLS-1$
    
    /**
     * File name of user name/password for Proxy Server
     */
    String PROXY_CREDENTIALS_FILE = "secure_proxy_credentials"; //$NON-NLS-1$
    
    /**
     * Remote git name, assumed that the repo is called "origin"
     */
    String ORIGIN = "origin"; //$NON-NLS-1$

    /**
     * Master branch
     */
    String MASTER = "master"; //$NON-NLS-1$

    /**
     * Head
     */
    String HEAD = "HEAD"; //$NON-NLS-1$
}
