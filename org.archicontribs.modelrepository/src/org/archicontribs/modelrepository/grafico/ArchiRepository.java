/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Representation of a repository
 * 
 * @author Phillip Beauvoir
 */
public class ArchiRepository {
    
    /**
     * The folder location of the local repository
     */
    private File fLocalRepoFolder;
    
    public ArchiRepository(File localRepoFolder) {
        fLocalRepoFolder = localRepoFolder;
    }

    /**
     * @return The local repository folder
     */
    public File getLocalRepositoryFolder() {
        return fLocalRepoFolder;
    }
    
    /**
     * @return The local repository's ".git" folder
     */
    public File getLocalGitFolder() {
        return new File(getLocalRepositoryFolder(), ".git"); //$NON-NLS-1$
    }

    /**
     * @return The repository name - the file name
     */
    public String getName() {
        return fLocalRepoFolder.getName();
    }

    /**
     * @return The .archimate file in the local repo
     */
    public File getTempModelFile() {
        return new File(getLocalRepositoryFolder(), "/.git/" + IGraficoConstants.LOCAL_ARCHI_FILENAME); //$NON-NLS-1$
    }
    
    /**
     * Return the online URL of the Git repo, taken from the local config file.
     * We assume that there is only one remote per repo, and its name is "origin"
     * @return The online URL or null if not found
     * @throws IOException
     */
    public String getOnlineRepositoryURL() throws IOException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            return git.getRepository().getConfig().getString("remote", "origin", "url"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
    
    /**
     * Locate a model in the model manager based on its file location
     * @return The model or null if not found
     */
    public IArchimateModel locateModel() {
        File tempFile = getTempModelFile();
        
        for(IArchimateModel model : IEditorModelManager.INSTANCE.getModels()) {
            if(tempFile.equals(model.getFile())) {
                return model;
            }
        }
        
        return null;
    }

    /**
     * @return true if the local .archimate file has been modified since the last Grafico export
     */
    public boolean hasLocalChanges() {
        File tempFile = getTempModelFile();
        
        File gitModelFolder = new File(getLocalRepositoryFolder(), IGraficoConstants.MODEL_FOLDER);
        
        if(!tempFile.exists() || !gitModelFolder.exists()) {
            return false;
        }
        
        long localFileLastModified = tempFile.lastModified();
        long gitFolderLastModified = gitModelFolder.lastModified();
        
        return localFileLastModified > gitFolderLastModified;
    }

    /**
     * Return true if there are local changes to commit in the working tree
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    public boolean hasChangesToCommit() throws IOException, GitAPIException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            Status status = git.status().call();
            return !status.isClean();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if((obj != null) && (obj instanceof ArchiRepository)) {
            return fLocalRepoFolder != null && fLocalRepoFolder.equals(((ArchiRepository)obj).getLocalRepositoryFolder());
        }
        return false;
    }
}
