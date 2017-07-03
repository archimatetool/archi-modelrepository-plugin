/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;

import com.archimatetool.model.IArchimateModel;

/**
 * Grafico Utils
 * 
 * @author Phillip Beauvoir
 */
public class GraficoUtils {

    /**
     * Get a local git folder name based on the repo's URL
     * @param repoURL
     * @return
     */
    public static String getLocalGitFolderName(String repoURL) {
        repoURL = repoURL.trim();
        
        int index = repoURL.lastIndexOf("/"); //$NON-NLS-1$
        if(index > 0 && index < repoURL.length() - 2) {
            repoURL = repoURL.substring(index + 1).toLowerCase();
        }
        
        index = repoURL.lastIndexOf(".git"); //$NON-NLS-1$
        if(index > 0 && index < repoURL.length() - 1) {
            repoURL = repoURL.substring(0, index);
        }
        
        return repoURL.replaceAll("[^a-zA-Z0-9-]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    /**
     * Check if a folder contains a Git repository
     * @param folder
     * @return
     */
    public static boolean isGitRepository(File folder) {
        if(folder == null || !folder.exists() || !folder.isDirectory()) {
            return false;
        }
        
        File gitFolder = new File(folder, ".git"); //$NON-NLS-1$
        return gitFolder.exists() && gitFolder.isDirectory();
    }
    
    /**
     * @param model
     * @return true if a model is in a local repo folder
     */
    public static boolean isModelInLocalRepository(IArchimateModel model) {
        return getLocalRepositoryFolderForModel(model) != null;
    }
    
    /**
     * Get the enclosing local repo folder for a model
     * It is assumed that the model is located at localRepoFolder/.git/temp.archimate
     * @param model
     * @return The folder
     */
    public static File getLocalRepositoryFolderForModel(IArchimateModel model) {
        if(model == null) {
            return null;
        }
        
        File file = model.getFile();
        if(file == null || !file.getName().equals(IGraficoConstants.LOCAL_ARCHI_FILENAME)) {
            return null;
        }
        
        File parent = file.getParentFile();
        if(parent == null || !parent.getName().equals(".git")) { //$NON-NLS-1$
            return null;
        }
        
        return parent.getParentFile();
    }
    
}
