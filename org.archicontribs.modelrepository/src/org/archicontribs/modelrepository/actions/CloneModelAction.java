/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.dialogs.CloneInputDialog;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.EmptyProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Clone a model
 */
public class CloneModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;
	
    public CloneModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE_16));
        setText(Messages.CloneModelAction_0);
        setToolTipText(Messages.CloneModelAction_1);
    }

    @Override
    public void run() {
        CloneInputDialog dialog = new CloneInputDialog(fWindow.getShell());
        if(dialog.open() != Window.OK) {
            return;
        }
    	
        final String repoURL = dialog.getURL();
        final String userName = dialog.getUsername();
        final String userPassword = dialog.getPassword();
        
        File localGitFolder = new File(ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder(),
                GraficoUtils.createLocalGitFolderName(repoURL));
        
        // Folder is not empty
        if(localGitFolder.exists() && localGitFolder.isDirectory() && localGitFolder.list().length > 0) {
            MessageDialog.openError(fWindow.getShell(),
                    Messages.CloneModelAction_0,
                    Messages.CloneModelAction_2);

            return;
        }
        
        class Progress extends EmptyProgressMonitor implements IRunnableWithProgress {
            private IProgressMonitor monitor;

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                try {
                    this.monitor = monitor;
                    
                    monitor.beginTask(Messages.CloneModelAction_4, 750);
                    
                    // Clone
                    GraficoUtils.cloneModel(localGitFolder, repoURL, userName, userPassword, this);
                    
                    monitor.subTask(Messages.CloneModelAction_5);
                    
                    // Load
                    GraficoUtils.loadModel(localGitFolder, fWindow.getShell());
                }
                catch(GitAPIException | IOException ex) {
                    MessageDialog.openError(fWindow.getShell(),
                            Messages.CloneModelAction_0,
                            Messages.CloneModelAction_3 + " " + //$NON-NLS-1$
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
