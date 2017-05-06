/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import junit.framework.JUnit4TestAdapter;


@SuppressWarnings("nls")
public class GraficoUtilsTests {
    
    public static junit.framework.Test suite() {
        return new JUnit4TestAdapter(GraficoUtilsTests.class);
    }
    
    @Before
    public void runOnceBeforeEachTest() {
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
        tmpFolder.delete();
    }

    @Test
    public void isGitRepository_HasGitFolder() {
        File tmpFolder = new File(getTempTestsFolder(), "testFolder");
        File gitFolder = new File(tmpFolder, ".git");
        gitFolder.mkdirs();
        
        assertTrue(GraficoUtils.isGitRepository(tmpFolder));
        gitFolder.delete();
        tmpFolder.delete();
    }
    
    private File getTempTestsFolder() {
        File file = new File(System.getProperty("java.io.tmpdir"), "org.archicontribs.modelrepository.tests.tmp");
        file.deleteOnExit();
        file.mkdirs();
        return file;
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

}
