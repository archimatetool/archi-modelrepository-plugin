/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
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

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Representation of a local repository
 * This is a wrapper class around a local repo folder
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
        String[] result = new String[1];
        result[0] = fLocalRepoFolder.getName();
        
        // Find the "folder.xml" file and read it from there
        File file = new File(getLocalRepositoryFolder(), IGraficoConstants.MODEL_FOLDER + "/" + IGraficoConstants.FOLDER_XML); //$NON-NLS-1$
        if(file.exists()) {
            try(Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()))) {
                stream.forEach(s -> {
                    if(s.indexOf("name=") != -1) { //$NON-NLS-1$
                        String segments[] = s.split("\""); //$NON-NLS-1$
                        if(segments.length == 2) {
                            result[0] = segments[1];
                        }
                    }
                });
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        
        return result[0];
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
    public boolean hasChangesToCommit() throws IOException, GitAPIException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            Status status = git.status().call();
            return !status.isClean();
        }
    }
    
    @Override
    public RevCommit commitChanges(String commitMessage, boolean amend) throws GitAPIException, IOException {
        String userName = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_NAME);
        String userEmail = ModelRepositoryPlugin.INSTANCE.getPreferenceStore().getString(IPreferenceConstants.PREFS_COMMIT_USER_EMAIL);
        
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            Status status = git.status().call();
            
            // Nothing changed
            if(status.isClean()) {
                return null;
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
            commitCommand.setAuthor(userName, userEmail);
            commitCommand.setMessage(commitMessage);
            commitCommand.setAmend(amend);
            return commitCommand.call();
        }
    }
    
    @Override
    public void cloneModel(String repoURL, String userName, String userPassword, ProgressMonitor monitor) throws GitAPIException, IOException {
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setDirectory(getLocalRepositoryFolder());
        cloneCommand.setURI(repoURL);
        cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
        cloneCommand.setProgressMonitor(monitor);
            
        try(Git git = cloneCommand.call()) {
            // Use the same line endings
            setConfigLineEndings(git);
        }
    }

    @Override
    public Iterable<PushResult> pushToRemote(String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            PushCommand pushCommand = git.push();
            pushCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            pushCommand.setProgressMonitor(monitor);
            return pushCommand.call();
        }
    }
    
    @Override
    public PullResult pullFromRemote(String userName, String userPassword, ProgressMonitor monitor) throws IOException, GitAPIException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            PullCommand pullCommand = git.pull();
            pullCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            pullCommand.setRebase(false); // Merge, not rebase
            pullCommand.setProgressMonitor(monitor);
            return pullCommand.call();
        }
    }
    
    @Override
    public FetchResult fetchFromRemote(String userName, String userPassword, ProgressMonitor monitor, boolean isDryrun) throws IOException, GitAPIException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            // Check and set tracked master branch
            setTrackedMasterBranch(git);
            
            FetchCommand fetchCommand = git.fetch();
            fetchCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            fetchCommand.setProgressMonitor(monitor);
            fetchCommand.setDryRun(isDryrun);
            return fetchCommand.call();
        }
    }

    @Override
    public Git createNewLocalGitRepository(String URL) throws GitAPIException, IOException, URISyntaxException {
        if(getLocalRepositoryFolder().exists() && getLocalRepositoryFolder().list().length > 0) {
            throw new IOException("Directory: " + getLocalRepositoryFolder().getAbsolutePath() + " is not empty."); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        InitCommand initCommand = Git.init();
        initCommand.setDirectory(getLocalRepositoryFolder());
        Git git = initCommand.call();
        
        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin"); //$NON-NLS-1$
        remoteAddCommand.setUri(new URIish(URL));
        remoteAddCommand.call();
        
        // Use the same line endings
        setConfigLineEndings(git);
        
        // Set tracked master branch
        setTrackedMasterBranch(git);
        
        return git;
    }

    @Override
    public String getFileContents(String path, String ref) throws IOException {
        String str = ""; //$NON-NLS-1$
        
        try(Repository repository = Git.open(getLocalRepositoryFolder()).getRepository()) {
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

    @Override
    public String getWorkingTreeFileContents(String path) throws IOException {
        String str = ""; //$NON-NLS-1$
        
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            try(BufferedReader in = new BufferedReader(new FileReader(new File(getLocalRepositoryFolder(), path)))) {
                String line;
                while((line = in.readLine()) != null) {
                    str += line + "\n"; //$NON-NLS-1$
                }
            }
        }
        
        return str;
    }

    @Override
    public void resetToRef(String ref) throws IOException, GitAPIException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            // Reset to master
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef(ref);
            resetCommand.setMode(ResetType.HARD);
            resetCommand.call();
            
            // Clean extra files
            CleanCommand cleanCommand = git.clean();
            cleanCommand.setCleanDirectories(true);
            cleanCommand.call();
        }
    }
    
    @Override
    public boolean hasUnpushedCommits(String branch) throws IOException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(git.getRepository(), branch);
            if(trackingStatus != null) {
                return trackingStatus.getAheadCount() > 0;
            }
            return false;
        }
    }
    

    @Override
    public boolean hasRemoteCommits(String branch) throws IOException {
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            BranchTrackingStatus trackingStatus = BranchTrackingStatus.of(git.getRepository(), branch);
            if(trackingStatus != null) {
                return trackingStatus.getBehindCount() > 0;
            }
            return false;
        }
    }
    
    @Override
    public void exportModelToGraficoFiles() throws IOException {
        // Open the model
        IArchimateModel model = IEditorModelManager.INSTANCE.openModel(getTempModelFile());
        
        if(model == null) {
            throw new IOException(Messages.ArchiRepository_0);
        }
        
        GraficoModelExporter exporter = new GraficoModelExporter(model, getLocalRepositoryFolder());
        exporter.exportModel();
    }
    
    @Override
    public boolean equals(Object obj) {
        if((obj != null) && (obj instanceof ArchiRepository)) {
            return fLocalRepoFolder != null && fLocalRepoFolder.equals(((IArchiRepository)obj).getLocalRepositoryFolder());
        }
        return false;
    }
    
    /**
     * Set Line endings in the config file to autocrlf=true
     * This ensures that files are not seen as different
     * @param git
     * @throws IOException
     */
    private void setConfigLineEndings(Git git) throws IOException {
        StoredConfig config = git.getRepository().getConfig();
        config.setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, "true"); //$NON-NLS-1$
        config.save();
    }
    
    /**
     * Set the tracked master branch to "origin"
     * @param git
     * @throws IOException
     */
    private void setTrackedMasterBranch(Git git) throws IOException {
        StoredConfig config = git.getRepository().getConfig();
        String branchName = "master"; //$NON-NLS-1$
        String remoteName = "origin"; //$NON-NLS-1$
        
        if(!remoteName.equals(config.getString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_REMOTE))) {
            config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName,  ConfigConstants.CONFIG_KEY_REMOTE, remoteName);
            config.setString(ConfigConstants.CONFIG_BRANCH_SECTION, branchName, ConfigConstants.CONFIG_KEY_MERGE, Constants.R_HEADS + branchName);
            config.save();
        }
    }
    
    
    @Override
    public boolean hasLocalChanges() throws IOException {
        String latestChecksum = getLatestChecksum();
        if(latestChecksum == null) {
            return false;
        }

        String currentChecksum = createChecksum();
        return !latestChecksum.equals(currentChecksum);
    }

    @Override
    public boolean saveChecksum() throws IOException {
        // Get the file's checksum as string
        String checksum = createChecksum();
        if(checksum == null) {
            return false;
        }

        File checksumFile = new File(getLocalGitFolder(), "checksum"); //$NON-NLS-1$
        Files.write(Paths.get(checksumFile.getAbsolutePath()), checksum.getBytes(), StandardOpenOption.CREATE);
        
        return true;
    }
    
    private String getLatestChecksum() throws IOException {
        File checksumFile = new File(getLocalGitFolder(), "checksum"); //$NON-NLS-1$
        if(!checksumFile.exists()) {
            return null;
        }
        
        byte[] bytes = Files.readAllBytes(Paths.get(checksumFile.getAbsolutePath()));
        return new String(bytes);
    }
    
    private String createChecksum() throws IOException {
        File tempFile = getTempModelFile();
        
        if(tempFile == null) {
            return null;
        }
        
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5"); //$NON-NLS-1$
        }
        catch(NoSuchAlgorithmException ex) {
            throw new IOException("NoSuchAlgorithm Exception", ex); //$NON-NLS-1$
        } 

        // Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(tempFile);

        // Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        // Read file data and update in message digest
        while((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        }

        fis.close();

        // Get the hash's bytes
        byte[] bytes = digest.digest();
        
        // This bytes[] has bytes in decimal format;
        // Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        
        return sb.toString();
    }
}
