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
 * PushModelAction
 * 
 * @author Phillip Beauvoir
 */
public class PushModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public PushModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_PUSH_16));
        setText("Publish");
        setToolTipText("Publish Changes to Remote");
    }

    @Override
    public void run() {
        // TODO Check and warn if commits are needed
        // TODO Offer choice to Push anyway

        // If user's local copy needs saving
        IArchimateModel openModel = GraficoUtils.locateModel(getGitRepository());
        if(openModel != null && IEditorModelManager.INSTANCE.isModelDirty(openModel)) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Publish",
                    "Please save your model first.");
            return;
        }
        
        // TODO - Check whether there are actual changes rather than timestamp changes
        if(GraficoUtils.hasLocalChanges(getGitRepository())) {
            MessageDialog.openInformation(fWindow.getShell(),
                    "Publish",
                    "Please commit your changes first.");
            return;
        }
        
        boolean doPush = MessageDialog.openConfirm(fWindow.getShell(),
                "Publish",
                "Publish changes?");
        
        if(!doPush) {
            return;
        }
        
        String credentials[] = null;
        try {
            credentials = SimpleCredentialsStorage.getUserNameAndPasswordFromCredentialsFileOrDialog(getGitFolder(),
                    IGraficoConstants.REPO_CREDENTIALS_FILE, fWindow.getShell());
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
                    
                    // Proxy
                    ProxyAuthenticater.update();
                    
                    // First we need to Pull and resolve any conflicts
                    PullResult pullResult = GraficoUtils.pullFromRemote(getGitRepository(), userName, userPassword, this);
                    
                    if(!pullResult.isSuccessful()) {
                        monitor.done();
                        
                        Display.getCurrent().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    MergeConflictHandler handler = new MergeConflictHandler(pullResult.getMergeResult(), getGitRepository(),
                                            fWindow.getShell());
                                    boolean result = handler.checkForMergeConflicts();
                                    if(result) {
                                        handler.mergeAndCommit();
                                        // We should return now and ask the user to try again, in case there have been more changes since this
                                        MessageDialog.openInformation(fWindow.getShell(),
                                                "Publish",
                                                "Conflicts resolved. Please Publish again.");
                                    }
                                    else {
                                        // User cancelled - do nothing (I think!)
                                    }
                                }
                                catch(IOException | GitAPIException ex) {
                                    displayErrorDialog(fWindow.getShell(), "Publish", ex);
                                }
                            }
                        });
                    }
                    else {
                        monitor.beginTask("Publishing", IProgressMonitor.UNKNOWN);
                        
                        // Push
                        GraficoUtils.pushToRemote(getGitRepository(), userName, userPassword, this);
                    }
                }
                catch(IOException | GitAPIException ex) {
                    ex.printStackTrace();
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
