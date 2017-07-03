/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ProgressMonitor;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

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
    public static void cloneModel(File localRepoFolder, String repoURL, String userName, String userPassword, ProgressMonitor monitor) throws GitAPIException, IOException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setDirectory(localRepoFolder);
        cloneCommand.setURI(repoURL);
        cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
        cloneCommand.setProgressMonitor(monitor);
            
        try(Git git = cloneCommand.call()) {
            // Use the same line endings
            StoredConfig config = git.getRepository().getConfig();
            config.setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, "true"); //$NON-NLS-1$
            config.save();
        }
    }

    /**
     * Push to Remote
     * @param localGitFolder
     * @param userName
     * @param userPassword
     * @return 
     * @throws IOException
     * @throws GitAPIException
     */
    public static Iterable<PushResult> pushToRemote(File localRepoFolder, String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        try(Git git = Git.open(localRepoFolder)) {
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            pushCommand.setProgressMonitor(monitor);
            return pushCommand.call();
        }
    }
    
    /**
     * Pull from Remote
     * @param localRepoFolder
     * @param userName
     * @param userPassword
     * @return 
     * @throws IOException
     * @throws GitAPIException
     */
    public static PullResult pullFromRemote(File localRepoFolder, String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        try(Git git = Git.open(localRepoFolder)) {
            PullCommand pullCommand = git.pull();
            pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            pullCommand.setRebase(false); // Merge, not rebase
            pullCommand.setProgressMonitor(monitor);
            return pullCommand.call();
        }
    }
    
    /**
     * Fetch from Remote
     * @param localRepoFolder
     * @param userName
     * @param userPassword
     * @throws IOException
     * @throws GitAPIException
     */
    public static FetchResult fetchFromRemote(File localRepoFolder, String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        try(Git git = Git.open(localRepoFolder)) {
            FetchCommand fetchCommand = git.fetch();
            fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            fetchCommand.setProgressMonitor(monitor);
            return fetchCommand.call();
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
    
    /**
     * Create a new, local Git repository with name set to "origin"
     * @param localRepoFolder
     * @param URL online URL
     * @return The Git object
     * @throws GitAPIException
     * @throws IOException
     * @throws URISyntaxException
     */
    public static Git createNewLocalGitRepository(File localRepoFolder, String URL) throws GitAPIException, IOException, URISyntaxException {
        if(localRepoFolder.exists() && localRepoFolder.list().length > 0) {
            throw new IOException("Directory: " + localRepoFolder.getAbsolutePath() + " is not empty."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        InitCommand initCommand = Git.init();
        initCommand.setDirectory(localRepoFolder);
        Git git = initCommand.call();
        
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin"); //$NON-NLS-1$
        remoteAddCommand.setUri(new URIish(URL));
        remoteAddCommand.call();
        
        // Use the same line endings
        StoredConfig config = git.getRepository().getConfig();
        config.setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, "true"); //$NON-NLS-1$
        config.save();
        
        return git;
    }

    /**
     * Retune the contents of a file in the repo given its ref
     * Ref could be "HEAD" or "origin/master" for example
     * @param localRepoFolder
     * @param path
     * @param ref
     * @return
     * @throws IOException
     */
    public static String getFileContents(File localRepoFolder, String path, String ref) throws IOException {
        String str = ""; //$NON-NLS-1$
        
        try(Repository repository = Git.open(localRepoFolder).getRepository()) {
            ObjectId lastCommitId = repository.resolve(ref);

            try(RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                RevTree tree = commit.getTree();

                // now try to find a specific file
                try(TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(path));

                    if(!treeWalk.next()) {
                        return Messages.GraficoUtils_0;
                    }

                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);

                    str = new String(loader.getBytes());
                }

                revWalk.dispose();
            }
        }
        
        return str;
    }

    /**
     * Get the file contents of a file in the working tree
     * @param localRepoFolder
     * @param path
     * @return
     * @throws IOException
     */
    public static String getWorkingTreeFileContents(File localRepoFolder, String path) throws IOException {
        String str = ""; //$NON-NLS-1$
        
        try(Git git = Git.open(localRepoFolder)) {
            try(BufferedReader in = new BufferedReader(new FileReader(new File(localRepoFolder, path)))) {
                String line;
                while((line = in.readLine()) != null) {
                    str += line + "\n"; //$NON-NLS-1$
                }
            }
        }
        
        return str;
    }
}
