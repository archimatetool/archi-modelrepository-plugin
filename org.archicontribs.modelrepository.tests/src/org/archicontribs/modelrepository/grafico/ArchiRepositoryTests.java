/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.archicontribs.modelrepository.GitHelper;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;

import junit.framework.JUnit4TestAdapter;


@SuppressWarnings("nls")
public class ArchiRepositoryTests {
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ArchiRepositoryTests.class);
    }
    
    @Before
    public void runOnceBeforeEachTest() {
    }
    
    @After
    public void runOnceAfterEachTest() throws IOException {
        FileUtils.deleteFolder(GitHelper.getTempTestsFolder());
    }
    
    @Test
    public void getName_IsCorrect() {
        File localRepoFolder = new File("/temp/folder");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        assertEquals("folder", repo.getName());
    }

    @Test
    public void getLocalGitFolder_IsCorrect() {
        File localRepoFolder = new File("/temp/folder");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        assertEquals(new File(localRepoFolder, ".git"), repo.getLocalGitFolder());
    }

    @Test
    public void getTempModelFile_IsCorrect() {
        File localRepoFolder = new File("/temp/folder");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        assertEquals(new File(localRepoFolder, ".git/" + IGraficoConstants.LOCAL_ARCHI_FILENAME), repo.getTempModelFile());
    }
    
    @Test
    public void getRepositoryURL_ShouldReturnURL() throws Exception {
        File localRepoFolder = new File(GitHelper.getTempTestsFolder(), "testRepo");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        String URL = "https://www.somewherethereish.net/myRepo.git";
        
        try(Git git = repo.createNewLocalGitRepository(URL)) {
            assertNotNull(git);
            assertEquals(URL, repo.getOnlineRepositoryURL());
        }
    }
    
    @Test
    public void locateModel_LocateNewModel() {
        File localRepoFolder = new File("/temp/folder");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        model.setFile(repo.getTempModelFile());
        
        // Not open
        assertNull(repo.locateModel());
        
        IEditorModelManager.INSTANCE.openModel(model);
        assertEquals(model, repo.locateModel());
    }

    @Test
    public void createNewLocalGitRepository_CreatesNewRepo() throws Exception {
        File localRepoFolder = new File(GitHelper.getTempTestsFolder(), "testRepo");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        String URL = "https://www.somewherethereish.net/myRepo.git";
        
        try(Git git = repo.createNewLocalGitRepository(URL)) {
            assertNotNull(git);
            assertEquals("origin", git.getRepository().getRemoteName("refs/remotes/origin/"));
            assertEquals(localRepoFolder, git.getRepository().getWorkTree());
            assertFalse(git.getRepository().isBare());
            assertEquals(URL, git.remoteList().call().get(0).getURIs().get(0).toASCIIString());
        }
    }
    
    @Test (expected=IOException.class)
    public void createNewLocalGitRepository_ThrowsExceptionIfNotEmptyDir() throws Exception {
        File tmpFile = File.createTempFile("architest", null, GitHelper.getTempTestsFolder());
        IArchiRepository repo = new ArchiRepository(tmpFile.getParentFile());
        
        // Should throw exception
        repo.createNewLocalGitRepository("");
    }
    
    @Test
    public void getFileContents_IsCorrect() throws Exception {
        File localRepoFolder = new File(GitHelper.getTempTestsFolder(), "testRepo");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        String contents = "Hello World!\nTesting.";
        
        try(Repository repos = GitHelper.createNewRepository(localRepoFolder)) {
            File file = new File(localRepoFolder, "test.txt");
            
            try(FileWriter fw = new FileWriter(file)) {
                fw.write(contents);
                fw.flush();
            }
            
            assertTrue(file.exists());
            
            // Add file to index
            AddCommand addCommand = new AddCommand(repos);
            addCommand.addFilepattern("."); //$NON-NLS-1$
            addCommand.setUpdate(false);
            addCommand.call();
            
            // Commit file
            CommitCommand commitCommand = Git.wrap(repos).commit();
            commitCommand.setAuthor("Test", "Test");
            commitCommand.setMessage("Message");
            commitCommand.call();

            assertEquals(contents, new String(repo.getFileContents("test.txt", IGraficoConstants.HEAD)));
        }
    }
    

}
