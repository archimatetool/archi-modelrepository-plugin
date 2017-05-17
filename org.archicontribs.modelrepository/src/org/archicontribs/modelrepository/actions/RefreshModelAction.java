/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticater;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.EmptyProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Refresh model action
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class RefreshModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public RefreshModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REFRESH_16));
        setText("Refresh");
        setToolTipText("Refresh Local Copy");
    }

    @Override
    public void run() {
        // If user's local copy needs saving
        IArchimateModel openModel = GraficoUtils.locateModel(getGitRepository());
        if(openModel != null && IEditorModelManager.INSTANCE.isModelDirty(openModel)) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Refresh",
                    "Please save your model first.");
            return;
        }
        
        // TODO - Check whether there are actual changes rather than timestamp changes
        if(GraficoUtils.hasLocalChanges(getGitRepository())) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Refresh",
                    "Please commit your changes first.");
            return;
        }
        
        // Get Credentials
        String credentials[] = null;
        try {
            credentials = SimpleCredentialsStorage.getUserNameAndPasswordFromCredentialsFileOrDialog(getGitFolder(), 
                    IGraficoConstants.REPO_CREDENTIALS_FILE, fWindow.getShell());
        }
        catch(IOException ex) {
            displayErrorDialog(fWindow.getShell(), "Refresh", ex);
        }
        if(credentials == null) {
            return;
        }
        
        final String userName = credentials[0];
        final String userPassword = credentials[1];
        
        // To be safe, close the model
        if(openModel != null) {
            try {
                IEditorModelManager.INSTANCE.closeModel(openModel);
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        
        class Progress extends EmptyProgressMonitor implements IRunnableWithProgress {
            private IProgressMonitor monitor;

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    this.monitor = monitor;
                    
                    monitor.beginTask("Refreshing", IProgressMonitor.UNKNOWN);
                    
                    // Proxy
                    ProxyAuthenticater.update();
                    
                    // First we need to Pull and check for conflicts
                    PullResult pullResult = GraficoUtils.pullFromRemote(getGitRepository(), userName, userPassword, this);
                    
                    monitor.done();
                    
                    Display.getCurrent().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            // Conflict merger
                            if(!pullResult.isSuccessful()) {
                                try {
                                    MergeConflictHandler handler = new MergeConflictHandler(pullResult.getMergeResult(), getGitRepository(), fWindow.getShell());
                                    boolean result = handler.checkForMergeConflicts();
                                    if(result) {
                                        handler.mergeAndCommit();
                                    }
                                    else {
                                        // User cancelled - we assume user has committed all changes so we can reset
                                        handler.resetToLocalState();
                                        return;
                                    }
                                }
                                catch(IOException | GitAPIException ex) {
                                    displayErrorDialog(fWindow.getShell(), "Refresh", ex);
                                }
                            }
                            
                            // Reload the model
                            try {
                                // Reload the model from the Grafico XML files
                                IArchimateModel model = GraficoUtils.loadModelFromGraficoFiles(getGitRepository(), fWindow.getShell());
                                
                                // Open it, this will do the necessary checks and add a command stack and an archive manager
                                if(model != null) {
                                    IEditorModelManager.INSTANCE.openModel(model);
                                }
                            }
                            catch(IOException ex) {
                                displayErrorDialog(fWindow.getShell(), "Refresh", ex);
                            }
                        }
                    });
                }
                catch(GitAPIException | IOException ex) {
                    displayErrorDialog(fWindow.getShell(), "Refresh", ex);
                }
                finally {
                    monitor.done();
                }
            }

            @Override
            public void beginTask(String title, int totalWork) {
                monitor.subTask(title);
            }

            @Override
            public boolean isCancelled() {
                return monitor.isCanceled();
            }
        }
        
        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                    pmDialog.run(false, true, new Progress());
                }
                catch(InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
    }
}
