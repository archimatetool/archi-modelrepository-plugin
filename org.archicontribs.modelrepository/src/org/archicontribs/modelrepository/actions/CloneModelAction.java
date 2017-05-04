/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Clone a model
 */
public class CloneModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public CloneModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE_16));
        setText("Download");
        setToolTipText("Download Remote Model");
    }

    @Override
    public void run() {
        File localFolder = new File("/testGit"); //$NON-NLS-1$
        String repoURL = ""; //$NON-NLS-1$
        String userName = ""; //$NON-NLS-1$
        String userPassword = ""; //$NON-NLS-1$
        
        if(localFolder.exists() && localFolder.isDirectory() && localFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    "Import",
                    "Local folder is not empty.");
            return;
        }
        
        Git git = null;
        
        try {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setDirectory(localFolder);
            cloneCommand.setURI(repoURL);
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, userPassword));
            
            git = cloneCommand.call();
        }
        catch(GitAPIException ex) {
            ex.printStackTrace();
        }
        finally {
            if(git != null) {
                git.close();
            }
        }
        
        try {
            GraficoModelImporter importer = new GraficoModelImporter();
            IArchimateModel model = importer.importLocalGitRepositoryAsModel(localFolder);
            
            if(importer.getResolveStatus() != null) {
                ErrorDialog.openError(fWindow.getShell(),
                        "Import",
                        "Errors occurred during import",
                        importer.getResolveStatus());

            }
            else {
                IEditorModelManager.INSTANCE.openModel(model);
            }
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}
