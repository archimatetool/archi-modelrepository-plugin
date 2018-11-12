/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.archimatetool.model.IArchimateModel;

/**
 * Push Model Action ("Publish")
 * 
 * 1. Do actions in Refresh Model Action
 * 2. If OK then Push to Remote
 * 
 * @author Phillip Beauvoir
 */
public class PushModelAction extends RefreshModelAction {
    
    public PushModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_PUSH));
        setText(Messages.PushModelAction_0);
        setToolTipText(Messages.PushModelAction_0);
    }

    public PushModelAction(IWorkbenchWindow window, IArchimateModel model) {
        super(window, model);
    }

    @Override
    public void run() {
        try {
            // Init
            UsernamePassword up = init();
            if(up != null) {
                // Pull
                int status = pull(up);
                if(status == PULL_STATUS_OK || status == PULL_STATUS_UP_TO_DATE) {
                    // Push
                    push(up);
                }
            }
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.PushModelAction_0, ex);
        }
    }
    
    protected void push(UsernamePassword up) throws InvocationTargetException, InterruptedException {
        Exception[] exception = new Exception[1];
        
        IProgressService ps = PlatformUI.getWorkbench().getProgressService();
        ps.busyCursorWhile(new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor pm) {
                try {
                    getRepository().pushToRemote(up.getUsername(), up.getPassword(), new ProgressMonitorWrapper(pm));
                }
                catch(GitAPIException | IOException ex) {
                    exception[0] = ex;
                }
            }
        });
        
        if(exception[0] != null) {
            displayErrorDialog(Messages.PushModelAction_0, exception[0]);
        }

        // Don't do this in the progress service as it will cause an Invalid thread access
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
    }
}
