/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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
public class ArchiRepositoryTests {
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(ArchiRepositoryTests.class);
    }
    
    @Before
    public void runOnceBeforeEachTest() {
    }
    
    @After
    public void runOnceAfterEachTest() throws IOException {
        FileUtils.deleteFolder(GraficoUtilsTests.getTempTestsFolder());
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
        File localRepoFolder = new File(GraficoUtilsTests.getTempTestsFolder(), "testRepo");
        IArchiRepository repo = new ArchiRepository(localRepoFolder);
        String URL = "https://www.somewherethereish.net/myRepo.git";
        
        try(Git git = GraficoUtils.createNewLocalGitRepository(localRepoFolder, URL)) {
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


}
