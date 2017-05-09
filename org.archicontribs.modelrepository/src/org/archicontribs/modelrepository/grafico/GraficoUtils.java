/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.swt.widgets.Shell;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Grafico and Git Utils
 * 
 * @author Phillip Beauvoir
 */
public class GraficoUtils {

    /**
     * Clone a model
     * @param localGitFolder
     * @param repoURL
     * @param userName
     * @param userPassword
     * @param monitor
     * @throws GitAPIException
     * @throws IOException
     */
    public static void cloneModel(File localGitFolder, String repoURL, String userName, String userPassword, ProgressMonitor monitor) throws GitAPIException, IOException {
        Git git = null;
        
        try {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setDirectory(localGitFolder);
            cloneCommand.setURI(repoURL);
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            cloneCommand.setProgressMonitor(monitor);
            
            git = cloneCommand.call();
            
            // Use the same line endings
            StoredConfig config = git.getRepository().getConfig();
            config.setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, "true"); //$NON-NLS-1$
            config.save();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }

    /**
     * Load a model from a local Git folder
     * @param localGitFolder
     * @param shell
     * @return The model
     * @throws IOException
     */
    public static IArchimateModel loadModel(File localGitFolder, Shell shell) throws IOException {
        GraficoModelImporter importer = new GraficoModelImporter();
        IArchimateModel model = importer.importLocalGitRepositoryAsModel(localGitFolder);
        
        if(importer.getResolveStatus() != null) {
            ErrorDialog.openError(shell,
                    Messages.GraficoUtils_0,
                    Messages.GraficoUtils_1,
                    importer.getResolveStatus());

        }
        else {
            model.setFile(getModelFileName(localGitFolder)); // Add a file name used to locate the model
            if(!IEditorModelManager.INSTANCE.isModelLoaded(model.getFile())) {
                IEditorModelManager.INSTANCE.openModel(model); // Open it
            }
        }

        return model;
    }

    /**
     * Commit a model with any changes to local repo
     * @param model
     * @param localGitFolder
     * @param personIdent
     * @param commitMessage
     * @throws GitAPIException
     * @throws IOException
     */
    public static void commitModel(IArchimateModel model, File localGitFolder, PersonIdent personIdent,
            String commitMessage) throws GitAPIException, IOException {
        Git git = null;
        
        try {
            GraficoModelExporter exporter = new GraficoModelExporter();
            exporter.exportModelToLocalGitRepository(model, localGitFolder);
            
            git = Git.open(localGitFolder);
            
            Status status = git.status().call();
            
            // Nothing changed
            if(status.isClean()) {
                return;
            }
            
            // Add modified files to index
            AddCommand addCommand = git.add();
            addCommand.addFilepattern("."); //$NON-NLS-1$
            addCommand.setUpdate(false);
            addCommand.call();
            
            // Add missing files to index
            for(String s : status.getMissing()) {
                git.rm().addFilepattern(s).call();
            }
            
            // Commit
            CommitCommand commitCommand = git.commit();
            commitCommand.setAuthor(personIdent);
            commitCommand.setMessage(commitMessage);
            commitCommand.call();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }
    
    /**
     * Push to Remote
     * @param localGitFolder
     * @param userName
     * @param userPassword
     * @throws IOException
     * @throws GitAPIException
     */
    public static void pushToRemote(File localGitFolder, String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        Git git = null;
        
        try {
            git = Git.open(localGitFolder);
            
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            pushCommand.setProgressMonitor(monitor);
            pushCommand.call();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }
    
    /**
     * Pull from Remote
     * @param localGitFolder
     * @param userName
     * @param userPassword
     * @throws IOException
     * @throws GitAPIException
     */
    public static void pullFromRemote(File localGitFolder, String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        Git git = null;
        
        try {
            git = Git.open(localGitFolder);
            
            PullCommand pullCommand = git.pull();
            pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            pullCommand.setProgressMonitor(monitor);
            pullCommand.call();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }
    
    /**
     * Fetch from Remote
     * @param localGitFolder
     * @param userName
     * @param userPassword
     * @throws IOException
     * @throws GitAPIException
     */
    public static void fetchFromRemote(File localGitFolder, String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        Git git = null;
        
        try {
            git = Git.open(localGitFolder);
            
            FetchCommand fetchCommand = git.fetch();
            fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            fetchCommand.setProgressMonitor(monitor);
            fetchCommand.call();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }

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
        
        // Name of the Git folder
        String GIT_FOLDER = ".git"; //$NON-NLS-1$
        
        File gitFolder = new File(folder, GIT_FOLDER);
        
        return gitFolder.exists() && gitFolder.isDirectory();
    }
    
    /**
     * @param localGitFolder
     * @return True if a model based on the local git folder name is loaded in the models tree
     */
    public static boolean isModelLoaded(File localGitFolder) {
        return IEditorModelManager.INSTANCE.isModelLoaded(getModelFileName(localGitFolder));
    }
    
    /**
     * Locate a model in the models tree based on its file location
     * @param localGitFolder
     * @return
     */
    public static IArchimateModel locateModel(File localGitFolder) {
        File tmpFileName = getModelFileName(localGitFolder);
        
        for(IArchimateModel model : IEditorModelManager.INSTANCE.getModels()) {
            if(tmpFileName.equals(model.getFile())) {
                return model;
            }
        }
        
        return null;
    }
    
    /**
     * Create a file name to attach to a model. Used to locate a model in the model tree
     * @param localGitFolder
     * @return
     */
    public static File getModelFileName(File localGitFolder) {
        return new File(localGitFolder, "/.git/temp.archimate"); //$NON-NLS-1$
    }
    
    /**
     * Create a new, local Git repository with name set to "origin"
     * @param localGitFolder
     * @param URL online URL
     * @return The Git object
     * @throws GitAPIException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static Git createNewLocalGitRepository(File localGitFolder, String URL) throws GitAPIException, IOException, URISyntaxException {
        if(localGitFolder.exists() && localGitFolder.list().length > 0) {
            throw new IOException("Directory: " + localGitFolder + " is not empty."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        InitCommand initCommand = Git.init();
        initCommand.setDirectory(localGitFolder);
        Git git = initCommand.call();
        
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin"); //$NON-NLS-1$
        remoteAddCommand.setUri(new URIish(URL));
        remoteAddCommand.call();
        
        return git;
    }

    /**
     * Return the URL of the Git repo, taken from local config file.
     * We assume that there is only one remote per repo, and its name is "origin"
     * @param localGitFolder
     * @return The URL or null if not found
     * @throws IOException
     */
    public static String getRepositoryURL(File localGitFolder) throws IOException {
        Git git = null;
        
        try {
            git = Git.open(localGitFolder);
            return git.getRepository().getConfig().getString("remote", "origin", "url"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }
}
