/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.api.Git;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;

import junit.framework.JUnit4TestAdapter;


@SuppressWarnings("nls")
public class GraficoUtilsTests {
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(GraficoUtilsTests.class);
    }
    
    @Before
    public void runOnceBeforeEachTest() {
    }
    
    @After
    public void runOnceAfterEachTest() throws IOException {
        FileUtils.deleteFolder(getTempTestsFolder());
    }
    
    
    @Test
    public void isGitRepository_FileShouldNotBe() throws Exception {
        File tmpFile = File.createTempFile("tmp", null);
        assertFalse(GraficoUtils.isGitRepository(tmpFile));
        tmpFile.delete();
    }

    @Test
    public void isGitRepository_EmptyFolderIsNotGitFolder() {
        File tmpFolder = new File(getTempTestsFolder(), "testFolder");
        tmpFolder.mkdirs();
        
        assertFalse(GraficoUtils.isGitRepository(tmpFolder));
    }

    @Test
    public void isGitRepository_HasGitFolder() {
        File tmpFolder = new File(getTempTestsFolder(), "testFolder");
        File gitFolder = new File(tmpFolder, ".git");
        gitFolder.mkdirs();
        
        assertTrue(GraficoUtils.isGitRepository(tmpFolder));
    }
    
    @Test
    public void createLocalGitFolderName_ShouldReturnCorrectName() {
        String repoURL = "https://githosting.org/path/archi-demo-grafico.git";
        assertEquals("archi-demo-grafico", GraficoUtils.createLocalGitFolderName(repoURL));
        
        repoURL = "ssh://githosting.org/path/archi-demo-grafico";
        assertEquals("archi-demo-grafico", GraficoUtils.createLocalGitFolderName(repoURL));
        
        repoURL = "ssh://githosting.org/This_One";
        assertEquals("this_one", GraficoUtils.createLocalGitFolderName(repoURL));        
    }

    @Test
    public void isModelLoaded_IsLoadedInModelsTree() {
        File localGitFolder = new File("/temp/folder");
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        model.setFile(GraficoUtils.getModelFileName(localGitFolder));
        
        IEditorModelManager.INSTANCE.openModel(model);
        assertTrue(GraficoUtils.isModelLoaded(localGitFolder));
    }

    @Test
    public void locateModel_LocateNewModel() {
        File localGitFolder = new File("/temp/folder");
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        model.setFile(GraficoUtils.getModelFileName(localGitFolder));
        
        IEditorModelManager.INSTANCE.openModel(model);
        assertEquals(model, GraficoUtils.locateModel(localGitFolder));
    }

    @Test
    public void getModelFileName_IsCorrect() {
        File localGitFolder = new File("/temp/folder");
        assertEquals(new File(localGitFolder, "temp.archimate"), GraficoUtils.getModelFileName(localGitFolder));
    }
    
    @Test
    public void createNewLocalGitRepository_CreatesNewRepo() throws Exception {
        File localGitFolder = new File(getTempTestsFolder(), "testRepo");
        String URL = "https://www.somewherethereish.net/myRepo.git";
        Git git = null;
        
        try {
            git = GraficoUtils.createNewLocalGitRepository(localGitFolder, URL);
            assertNotNull(git);
            assertEquals("origin", git.getRepository().getRemoteName("refs/remotes/origin/"));
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
    }
    
    @Test(expected=IOException.class)
    public void createNewLocalGitRepository_ThrowsExceptionIfNotEmptyDir() throws Exception {
        File localGitFolder = new File(getTempTestsFolder(), "testRepo");
        String URL = "https://www.somewherethereish.net/myRepo.git";
        Git git = null;
        
        try {
            git = GraficoUtils.createNewLocalGitRepository(localGitFolder, URL);
            assertNotNull(git);
        }
        finally {
            // Don't delete folder
            if(git != null) {
                git.close();
            }
        }
        
        // Should throw exception
        git = GraficoUtils.createNewLocalGitRepository(getTempTestsFolder(), URL);
    }
    
    @Test
    public void getRepositoryURL_ShouldReturnURL() throws Exception {
        File localGitFolder = new File(getTempTestsFolder(), "testRepo");
        String URL = "https://www.somewherethereish.net/myRepo.git";
        Git git = null;
        
        try {
            git = GraficoUtils.createNewLocalGitRepository(localGitFolder, URL);
            assertNotNull(git);
            assertEquals(URL, GraficoUtils.getRepositoryURL(localGitFolder));
        }
        finally {
            // Don't delete folder
            if(git != null) {
                git.close();
            }
        }
    }
    
    
    // Support
    
    private File getTempTestsFolder() {
        File file = new File(System.getProperty("java.io.tmpdir"), "org.archicontribs.modelrepository.tests.tmp");
        file.deleteOnExit();
        file.mkdirs();
        return file;
    }
    

}
