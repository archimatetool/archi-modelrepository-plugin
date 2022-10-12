/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.GeneralSecurityException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticator;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.authentication.internal.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

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
            int status = init();
            if(status != USER_OK) {
                return;
            }
            
            // Check primary key set
            if(!EncryptedCredentialsStorage.checkPrimaryKeySet()) {
                return;
            }
            
            // Get this before opening the progress dialog
            // UsernamePassword is will be null if using SSH
            UsernamePassword npw = getUsernamePassword();
            // User cancelled on HTTP
            if(npw == null && GraficoUtils.isHTTP(getRepository().getOnlineRepositoryURL())) {
                return;
            }

            // Do main action with PM dialog
            Display.getCurrent().asyncExec(new Runnable() {
                @Override
                public void run() {
                    ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                    
                    try {
                        pmDialog.run(false, true, new IRunnableWithProgress() {
                            @Override
                            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                try {
                                    monitor.beginTask(Messages.PushModelAction_1, -1);
                                    
                                    // Update Proxy
                                    ProxyAuthenticator.update(getRepository().getOnlineRepositoryURL());
                                    
                                    // Pull
                                    int status = pull(npw, pmDialog);
                                    
                                    // Push
                                    if(status == PULL_STATUS_OK || status == PULL_STATUS_UP_TO_DATE) {
                                        Iterable<PushResult> pushResult = push(npw, pmDialog);
                                        
                                        // Get any errors in Push Results
                                        StringBuilder sb = new StringBuilder();
                                        
                                        pushResult.forEach(result -> {
                                            result.getRemoteUpdates().stream()
                                                    .filter(update -> update.getStatus() != RemoteRefUpdate.Status.OK)
                                                    .filter(update -> update.getStatus() != RemoteRefUpdate.Status.UP_TO_DATE)
                                                    .forEach(update -> {
                                                        sb.append(result.getMessages() + "\n"); //$NON-NLS-1$
                                                    });
                                            
                                        });
                                        
                                        if(sb.length() != 0) {
                                            pmDialog.getShell().setVisible(false);
                                            displayErrorDialog(Messages.PushModelAction_0, sb.toString());
                                        }
                                    }
                                }
                                catch(Exception ex) {
                                    pmDialog.getShell().setVisible(false);
                                    displayErrorDialog(Messages.PushModelAction_0, ex);
                                }
                                finally {
                                    try {
                                        saveChecksumAndNotifyListeners();
                                    }
                                    catch(IOException ex) {
                                        ex.printStackTrace();
                                    }
                                    
                                    // Clear credentials
                                    if(npw != null) {
                                        npw.clear();
                                    }
                                    
                                    // Clear Proxy
                                    ProxyAuthenticator.clear();
                                }
                            }
                        });
                    }
                    catch(InvocationTargetException | InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
        catch(GeneralSecurityException ex) {
            displayCredentialsErrorDialog(ex);
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.PushModelAction_0, ex);
        }
    }
    
    Iterable<PushResult> push(UsernamePassword npw, ProgressMonitorDialog pmDialog) throws IOException, GitAPIException {
        pmDialog.getProgressMonitor().subTask(Messages.PushModelAction_2);
        Display.getCurrent().readAndDispatch();  // update dialog
        return getRepository().pushToRemote(npw, new ProgressMonitorWrapper(pmDialog.getProgressMonitor()));
    }
}
