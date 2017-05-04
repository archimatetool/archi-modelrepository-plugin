/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
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

    // TEMPORARY FOR TESTING!
    public static final File TEST_LOCAL_FILE = new File("/temp.archimate"); //$NON-NLS-1$

    public static final File TEST_LOCAL_GIT_FOLDER = new File("/testGit"); //$NON-NLS-1$
    public static final String TEST_REPO_URL = ""; //$NON-NLS-1$
    public static final String TEST_USER_NAME = ""; //$NON-NLS-1$
    public static final String TEST_USER_PASSWORD = ""; //$NON-NLS-1$
    

    /**
     * Clone a model
     * @param localGitFolder
     * @param repoURL
     * @param userName
     * @param userPassword
     * @throws GitAPIException
     */
    public static void cloneModel(File localGitFolder, String repoURL, String userName, String userPassword) throws GitAPIException {
        Git git = null;
        
        try {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setDirectory(localGitFolder);
            cloneCommand.setURI(repoURL);
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            
            git = cloneCommand.call();
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
            model.setFile(TEST_LOCAL_FILE); // TEMPORARY FOR TESTING!
            IEditorModelManager.INSTANCE.openModel(model);
        }

        return model;
    }

    public static void commitModel(IArchimateModel model, File localGitFolder) throws GitAPIException, IOException {
        Git git = null;
        
        try {
            GraficoModelExporter exporter = new GraficoModelExporter();
            //exporter.exportModelToLocalGitRepository(model, localGitFolder);
            
            git = Git.open(localGitFolder);
            
            Status status = git.status().call();
            Set<String> set = status.getUncommittedChanges();
            
            for(String s : set) {
                System.out.println(s);
            }
            
//            AddCommand addCommand = git.add();
//            addCommand.addFilepattern(".");
//            addCommand.call();

//            CommitCommand commitCommand = git.commit();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }

    }
}
