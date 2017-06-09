package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;

import com.archimatetool.model.IArchimateModel;

public interface IArchiRepository {

    /**
     * @return The local repository folder
     */
    File getLocalRepositoryFolder();

    /**
     * @return The local repository's ".git" folder
     */
    File getLocalGitFolder();

    /**
     * @return The repository name - the file name
     */
    String getName();

    /**
     * @return The .archimate file in the local repo
     */
    File getTempModelFile();

    /**
     * Return the online URL of the Git repo, taken from the local config file.
     * We assume that there is only one remote per repo, and its name is "origin"
     * @return The online URL or null if not found
     * @throws IOException
     */
    String getOnlineRepositoryURL() throws IOException;

    /**
     * Locate a model in the model manager based on its file location
     * @return The model or null if not found
     */
    IArchimateModel locateModel();

    /**
     * @return true if the local .archimate file has been modified since the last Grafico export
     */
    boolean hasLocalChanges();

    /**
     * Return true if there are local changes to commit in the working tree
     * @return
     * @throws IOException
     * @throws GitAPIException
     */
    boolean hasChangesToCommit() throws IOException, GitAPIException;

}