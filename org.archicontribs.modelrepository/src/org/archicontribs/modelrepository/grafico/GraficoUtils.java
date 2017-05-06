/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
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

    public static final String TEST_REPO_URL = ""; //$NON-NLS-1$
    public static final String TEST_USER_LOGIN = ""; //$NON-NLS-1$
    public static final String TEST_USER_PASSWORD = ""; //$NON-NLS-1$

    /**
     * Clone a model
     * @param localGitFolder
     * @param repoURL
     * @param userName
     * @param userPassword
     * @throws GitAPIException
     * @throws IOException 
     */
    public static void cloneModel(File localGitFolder, String repoURL, String userName, String userPassword) throws GitAPIException, IOException {
        Git git = null;
        
        try {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setDirectory(localGitFolder);
            cloneCommand.setURI(repoURL);
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            
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
                    "Import",
                    "Errors occurred during import",
                    importer.getResolveStatus());

        }
        else {
            model.setFile(getModelFileName(localGitFolder)); // Add a file name used to locate the model
            IEditorModelManager.INSTANCE.openModel(model);
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
    public static void pushToRemote(File localGitFolder, String userName, String userPassword) throws IOException, GitAPIException {
        Git git = null;
        
        try {
            git = Git.open(localGitFolder);
            
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
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
    public static void pullFromRemote(File localGitFolder, String userName, String userPassword) throws IOException, GitAPIException {
        Git git = null;
        
        try {
            git = Git.open(localGitFolder);
            
            PullCommand pullCommand = git.pull();
            pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            pullCommand.call();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }
    
    /**
     * Create a local git folder name based on the repo's URL
     * @param repoURL
     * @return
     */
    public static String createLocalGitFolderName(String repoURL) {
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
        return new File(localGitFolder, "temp.archimate"); //$NON-NLS-1$
    }
}
