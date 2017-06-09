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
public class ArchiRepository implements IArchiRepository {
    
    /**
     * The folder location of the local repository
     */
    private File fLocalRepoFolder;
    
    public ArchiRepository(File localRepoFolder) {
        fLocalRepoFolder = localRepoFolder;
    }

    @Override
    public File getLocalRepositoryFolder() {
        return fLocalRepoFolder;
    }
    
    @Override
    public File getLocalGitFolder() {
        return new File(getLocalRepositoryFolder(), ".git"); //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return fLocalRepoFolder.getName();
    }

    @Override
    public File getTempModelFile() {
        return new File(getLocalRepositoryFolder(), "/.git/" + IGraficoConstants.LOCAL_ARCHI_FILENAME); //$NON-NLS-1$
    }
    
    @Override
    public String getOnlineRepositoryURL() throws IOException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            return git.getRepository().getConfig().getString("remote", "origin", "url"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
    }
    
    @Override
    public IArchimateModel locateModel() {
        File tempFile = getTempModelFile();
        
        for(IArchimateModel model : IEditorModelManager.INSTANCE.getModels()) {
            if(tempFile.equals(model.getFile())) {
                return model;
            }
        }
        
        return null;
    }

    @Override
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

    @Override
    public boolean hasChangesToCommit() throws IOException, GitAPIException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            Status status = git.status().call();
            return !status.isClean();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if((obj != null) && (obj instanceof ArchiRepository)) {
            return fLocalRepoFolder != null && fLocalRepoFolder.equals(((IArchiRepository)obj).getLocalRepositoryFolder());
        }
        return false;
    }
}
