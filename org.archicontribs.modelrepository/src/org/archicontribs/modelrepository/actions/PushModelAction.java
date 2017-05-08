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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.EmptyProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

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
        setText("Push");
        setToolTipText("Push Changes to Remote");
    }

    @Override
    public void run() {
        boolean doPush = MessageDialog.openConfirm(fWindow.getShell(),
                "Push",
                "Push changes?");
        
        if(!doPush) {
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
                    
                    monitor.beginTask("Pushing", IProgressMonitor.UNKNOWN);
                    
                    // Push
                    GraficoUtils.pushToRemote(getGitRepository(), userName, userPassword, this);
                }
                catch(GitAPIException | IOException ex) {
                    ex.printStackTrace();
                    MessageDialog.openError(fWindow.getShell(),
                            "Push",
                            "There was an error:" + " " +
                                ex.getMessage());
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
