/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.GitHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        FileUtils.deleteFolder(GitHelper.getTempTestsFolder());
    }
    
    @Test
    public void isSSH() {
        // True
        String[] trueOnes = {
                "git@github.com:archimatetool/archi-modelrepository-plugin.git",
                "ssh://user@host.xz/path/to/repo.git/",
                "ssh://user@host.xz:4019/path/to/repo.git/",
                "ssh://user:password@host.xz/path/to/repo.git/",
                "ssh://host.xz/path/to/repo.git/",
                "ssh://user@host.xz/path/to/repo.git/",
                "ssh://host.xz/path/to/repo.git/",
                "ssh://user@host.xz/~user/path/to/repo.git/",
                "ssh://host.xz/~user/path/to/repo.git/",
                "ssh://user@host.xz/~/path/to/repo.git",
                "ssh://host.xz/~/path/to/repo.git",
                "user@host.xz:/path/to/repo.git/",
                "host.xz:/path/to/repo.git/",
                "user@host.xz:~user/path/to/repo.git/",
                "host.xz:~user/path/to/repo.git/",
                "user@host.xz:path/to/repo.git",
                "host.xz:path/to/repo.git"
        };
        
        for(String url : trueOnes) {
            assertTrue(GraficoUtils.isSSH(url));
        }
        
        // False
        String[] falseOnes = {
                "ssh://user@host.example.com",
                "https://githosting.org/path/archi-demo-grafico.git",
                "http://githosting.org/path/archi-demo-grafico.git",
                "ssh://:8888/path/to/repo.git/",
        };
        
        for(String url : falseOnes) {
            assertFalse(GraficoUtils.isSSH(url));
        }
    }

    @Test
    public void getLocalGitFolderName_ShouldReturnCorrectName() {
        String repoURL = "https://githosting.org/path/archi-demo-grafico.git";
        assertEquals("archi-demo-grafico", GraficoUtils.getLocalGitFolderName(repoURL));
        
        repoURL = "ssh://githosting.org/path/archi-demo-grafico";
        assertEquals("archi-demo-grafico", GraficoUtils.getLocalGitFolderName(repoURL));
        
        repoURL = "ssh://githosting.org/This_One";
        assertEquals("this_one", GraficoUtils.getLocalGitFolderName(repoURL));        
    }

    @Test
    public void isGitRepository_FileShouldNotBe() throws Exception {
        File tmpFile = File.createTempFile("architest", null);
        assertFalse(GraficoUtils.isGitRepository(tmpFile));
        tmpFile.delete();
    }

    @Test
    public void isGitRepository_EmptyFolderIsNotGitFolder() {
        File tmpFolder = new File(GitHelper.getTempTestsFolder(), "testFolder");
        tmpFolder.mkdirs();
        
        assertFalse(GraficoUtils.isGitRepository(tmpFolder));
    }

    @Test
    public void isGitRepository_HasGitFolder() {
        File tmpFolder = new File(GitHelper.getTempTestsFolder(), "testFolder");
        File gitFolder = new File(tmpFolder, ".git");
        gitFolder.mkdirs();
        
        assertTrue(GraficoUtils.isGitRepository(tmpFolder));
    }
    
    @Test
    public void isModelInGitRepository_IsCorrect() {
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        File file = new File("parent/parent/.git/" + IGraficoConstants.LOCAL_ARCHI_FILENAME);
        model.setFile(file);
        assertTrue(GraficoUtils.isModelInLocalRepository(model));
    }

    @Test
    public void getLocalGitFolderForModel_IsCorrect() {
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        File expected = new File("parent/parent/");
        File file = new File(expected, ".git/" + IGraficoConstants.LOCAL_ARCHI_FILENAME);
        model.setFile(file);
        assertEquals(expected, GraficoUtils.getLocalRepositoryFolderForModel(model));
    }
    
    @Test
    public void getLocalGitFolderForModel_IsWrong() {
        IArchimateModel model = IArchimateFactory.eINSTANCE.createArchimateModel();
        File file = new File("parent/parent/.git/dobbin.archimate");
        model.setFile(file);
        assertNull(GraficoUtils.getLocalRepositoryFolderForModel(model));
    }


}
