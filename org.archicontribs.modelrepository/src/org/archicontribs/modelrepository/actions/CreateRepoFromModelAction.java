/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticator;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.dialogs.NewModelRepoDialog;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.Git;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Create an online repo from an existing model
 * 
 * @author Phillip Beauvoir
 */
public class CreateRepoFromModelAction extends AbstractModelAction {
    
    private IArchimateModel fModel;
	
    public CreateRepoFromModelAction(IWorkbenchWindow window, IArchimateModel model) {
        super(window);
        
        fModel = model;
        
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CREATE_REPOSITORY));
        setText(Messages.CreateRepoFromModelAction_0);
        setToolTipText(Messages.CreateRepoFromModelAction_0);
    }

    @Override
    public void run() {
        NewModelRepoDialog dialog = new NewModelRepoDialog(fWindow.getShell());
        if(dialog.open() != Window.OK) {
            return;
        }
        
        final String repoURL = dialog.getURL();
        final boolean storeCredentials = dialog.doStoreCredentials();
        final UsernamePassword npw = dialog.getUsernamePassword();
        
        if(!StringUtils.isSet(repoURL)) {
            return;
        }
        
        if(GraficoUtils.isHTTP(repoURL) && !StringUtils.isSet(npw.getUsername()) && !StringUtils.isSet(npw.getPassword())) {
            MessageDialog.openError(fWindow.getShell(), 
                    Messages.CreateRepoFromModelAction_0,
                    Messages.CreateRepoFromModelAction_3);

            return;
        }
        
        File localRepoFolder = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                GraficoUtils.getLocalGitFolderName(repoURL));
        
        // Folder is not empty
        if(localRepoFolder.exists() && localRepoFolder.isDirectory() && localRepoFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.CreateRepoFromModelAction_1,
                    Messages.CreateRepoFromModelAction_2 +
                    " " + localRepoFolder.getAbsolutePath()); //$NON-NLS-1$

            return;
        }
        
        setRepository(new ArchiRepository(localRepoFolder));
        
        try {
            // Proxy check
            ProxyAuthenticator.update(repoURL);
            
            // Create a new repo
            try(Git git = getRepository().createNewLocalGitRepository(repoURL)) {
            }
            
            // TODO: If the model has not been saved yet this is fine but if the model already exists
            // We should tell the user this is the case
            
            // Set new file location
            fModel.setFile(getRepository().getTempModelFile());
            
            // And Save it
            IEditorModelManager.INSTANCE.saveModel(fModel);
            
            // Export to Grafico
            getRepository().exportModelToGraficoFiles();
            
            // Commit changes
            getRepository().commitChanges(Messages.CreateRepoFromModelAction_5, false);
            
            // Push
            Exception[] exception = new Exception[1];
            IProgressService ps = PlatformUI.getWorkbench().getProgressService();
            ps.busyCursorWhile(new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor pm) {
                    try {
                        pm.beginTask(Messages.CreateRepoFromModelAction_4, -1);
                        getRepository().pushToRemote(npw, new ProgressMonitorWrapper(pm));
                    }
                    catch(Exception ex) {
                        exception[0] = ex;
                    }
                }
            });

            if(exception[0] != null) {
                throw exception[0];
            }

            // Store repo credentials if HTTP and option is set
            if(GraficoUtils.isHTTP(repoURL) && storeCredentials) {
                SimpleCredentialsStorage scs = new SimpleCredentialsStorage(new File(getRepository().getLocalGitFolder(), IGraficoConstants.REPO_CREDENTIALS_FILE));
                scs.store(npw);
            }
            
            // Save the checksum
            getRepository().saveChecksum();
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.CreateRepoFromModelAction_7, ex);
        }
    }
}
