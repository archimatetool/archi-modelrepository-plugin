/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.GitHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;


@SuppressWarnings("nls")
public class GraficoUtilsTests {
    
    @BeforeEach
    public void runOnceBeforeEachTest() {
    }
    
    @AfterEach
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
    public void getUniqueLocalFolder() throws Exception {
        String repoURL = "https://githosting.org/path/archi-demo-grafico.git";
        File tmpFolder = new File(GitHelper.getTempTestsFolder(), "testFolder");
        
        // Empty and OK
        File folder = GraficoUtils.getUniqueLocalFolder(tmpFolder, repoURL);
        assertEquals(new File(tmpFolder, "archi-demo-grafico"), folder);
        
        // Exists and OK
        folder.mkdirs();
        folder = GraficoUtils.getUniqueLocalFolder(tmpFolder, repoURL);
        assertEquals(new File(tmpFolder, "archi-demo-grafico"), folder);
        
        // Exists but is not empty so has "_1" appended
        File.createTempFile("architest", null, folder);
        folder = GraficoUtils.getUniqueLocalFolder(tmpFolder, repoURL);
        assertEquals(new File(tmpFolder, "archi-demo-grafico_1"), folder);
        
        // Add another folder so has "_2" appended
        folder.mkdirs();
        File.createTempFile("architest", null, new File(tmpFolder, "archi-demo-grafico_1"));
        folder = GraficoUtils.getUniqueLocalFolder(tmpFolder, repoURL);
        assertEquals(new File(tmpFolder, "archi-demo-grafico_2"), folder);
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
