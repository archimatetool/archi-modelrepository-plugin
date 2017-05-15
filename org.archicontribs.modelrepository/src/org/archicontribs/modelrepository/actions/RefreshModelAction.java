/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.UserDetails;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
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
        // TODO we need to prompt user to save/commit changes before a pull and reload
        
        if(IEditorModelManager.INSTANCE.isModelLoaded(getGitRepository())) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Refresh",
                    "Model is already open. Close it and retry.");
            return;
        }
        
        String credentials[] = null;
        try {
            credentials = UserDetails.getUserNameAndPasswordFromCredentialsFileOrDialog(getGitRepository(), fWindow.getShell());
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        if(credentials == null) {
            return;
        }
        
        final String userName = credentials[0];
        final String userPassword = credentials[1];
        
        class Progress extends EmptyProgressMonitor implements IRunnableWithProgress {
            private IProgressMonitor monitor;

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    this.monitor = monitor;
                    
                    monitor.beginTask("Refreshing", IProgressMonitor.UNKNOWN);
                    
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
                            
                            // Load
                            try {
                                GraficoUtils.loadModel(getGitRepository(), fWindow.getShell());
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
