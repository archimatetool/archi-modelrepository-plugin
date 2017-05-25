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
 * 1. Offer to save the model
 * 2. If there are changes offer to Commit
 * 3. Get credentials for Pull
 * 4. Check Proxy
 * 5. Pull from Remote
 * 6. Handle Merge conflicts
 * 7. Reload temp file from Grafico files
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class RefreshModelAction extends AbstractModelAction {
	
    public RefreshModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REFRESH));
        setText(Messages.RefreshModelAction_0);
        setToolTipText(Messages.RefreshModelAction_1);
    }

    @Override
    public void run() {
        // This will either return the already open model or will actually open it
        // TODO We need to load a model without opening it in the models tree. But this will need a new API in IEditorModelManager
        IArchimateModel model = IEditorModelManager.INSTANCE.openModel(GraficoUtils.getModelFileName(getLocalRepositoryFolder()));
        
        if(model == null) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.RefreshModelAction_0,
                    Messages.RefreshModelAction_2);
            return;
        }
        
        // Offer to save it if dirty
        // We need to do this to keep grafico and temp files in sync
        if(IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }
        
        // Do the Grafico Export first
        exportModelToGraficoFiles(model, getLocalRepositoryFolder());
        
        // Then offer to Commit
        try {
            if(GraficoUtils.hasChangesToCommit(getLocalRepositoryFolder())) {
                if(!offerToCommitChanges()) {
                    return;
                }
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RefreshModelAction_3, ex);
            return;
        }
        
        // Get Credentials
        String credentials[] = null;
        try {
            credentials = SimpleCredentialsStorage.getUserNameAndPasswordFromCredentialsFileOrDialog(getLocalGitFolder(), 
                    IGraficoConstants.REPO_CREDENTIALS_FILE, fWindow.getShell());
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.RefreshModelAction_4, ex);
            return;
        }
        if(credentials == null) {
            return;
        }
        
        final String userName = credentials[0];
        final String userPassword = credentials[1];
        
        // Close the model
        // TODO this needs changing in the Archi API
        try {
            IEditorModelManager.INSTANCE.closeModel(model);
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.RefreshModelAction_5, ex);
        }
        
        class Progress extends EmptyProgressMonitor implements IRunnableWithProgress {
            private IProgressMonitor monitor;

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    this.monitor = monitor;
                    
                    monitor.beginTask(Messages.RefreshModelAction_6, IProgressMonitor.UNKNOWN);
                    
                    // Proxy
                    ProxyAuthenticater.update(GraficoUtils.getRepositoryURL(getLocalRepositoryFolder()));
                    
                    // First we need to Pull and check for conflicts
                    PullResult pullResult = GraficoUtils.pullFromRemote(getLocalRepositoryFolder(), userName, userPassword, this);
                    
                    monitor.done();
                    
                    Display.getCurrent().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            // Conflict merger
                            if(!pullResult.isSuccessful()) {
                                try {
                                    MergeConflictHandler handler = new MergeConflictHandler(pullResult.getMergeResult(), getLocalRepositoryFolder(), fWindow.getShell());
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
                                    displayErrorDialog(Messages.RefreshModelAction_0, ex);
                                }
                            }
                            
                            // Reload the model from the Grafico XML files
                            try {
                                loadModelFromGraficoFiles(getLocalRepositoryFolder());
                            }
                            catch(IOException ex) {
                                displayErrorDialog(Messages.RefreshModelAction_0, ex);
                            }
                        }
                    });
                }
                catch(GitAPIException | IOException ex) {
                    displayErrorDialog(Messages.RefreshModelAction_0, ex);
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
