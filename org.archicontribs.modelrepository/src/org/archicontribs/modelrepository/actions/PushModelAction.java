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
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Push Model Action ("Publish")
 * 
 * 1. Offer to save the model
 * 2. If there are changes offer to Commit
 * 3. Get credentials for Push
 * 4. Check Proxy
 * 5. Pull from Remote
 * 6. Handle Merge conflicts
 * 7. Push to Remote
 * 
 * @author Phillip Beauvoir
 */
public class PushModelAction extends AbstractModelAction {
    
    public PushModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_PUSH));
        setText(Messages.PushModelAction_0);
        setToolTipText(Messages.PushModelAction_1);
    }

    public PushModelAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }

    @Override
    public void run() {
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return;
            }
        }

        // Do the Grafico Export first
        exportModelToGraficoFiles();
        
        // Then offer to Commit
        try {
            if(getRepository().hasChangesToCommit()) {
                if(!offerToCommitChanges()) {
                    return;
                }
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.PushModelAction_4, ex);
            return;
        }
        
        // Get User Credentials first
        final UsernamePassword up = getUserNameAndPasswordFromCredentialsFileOrDialog(IGraficoConstants.REPO_CREDENTIALS_FILE, fWindow.getShell());
        if(up == null) {
            return;
        }
        
        /**
         * Wrapper class to handle progress monitor
         */
        class PushProgressHandler extends ProgressHandler {
            
            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                super.run(monitor);
                
                try {
                    // Proxy
                    ProxyAuthenticater.update(getRepository().getOnlineRepositoryURL());
                    
                    // First we need to Pull and resolve any conflicts
                    PullResult pullResult = GraficoUtils.pullFromRemote(getRepository().getLocalRepositoryFolder(), up.getUsername(), up.getPassword(), this);
                    
                    if(!pullResult.isSuccessful()) {
                        monitor.done();
                        
                        Display.getCurrent().asyncExec(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    MergeConflictHandler handler = new MergeConflictHandler(pullResult.getMergeResult(), getRepository().getLocalRepositoryFolder(),
                                            fWindow.getShell());
                                    boolean result = handler.checkForMergeConflicts();
                                    if(result) {
                                        handler.mergeAndCommit(Messages.PushModelAction_8);
                                        // We should return now and ask the user to try again, in case there have been more changes since this
                                        MessageDialog.openInformation(fWindow.getShell(),
                                                Messages.PushModelAction_0,
                                                Messages.PushModelAction_6);
                                    }
                                    else {
                                        // User cancelled - do nothing (I think!)
                                    }
                                }
                                catch(IOException | GitAPIException ex) {
                                    displayErrorDialog(Messages.PushModelAction_0, ex);
                                }
                            }
                        });
                    }
                    else {
                        monitor.beginTask(Messages.PushModelAction_7, IProgressMonitor.UNKNOWN);
                        
                        // Push
                        GraficoUtils.pushToRemote(getRepository().getLocalRepositoryFolder(), up.getUsername(), up.getPassword(), this);
                    }
                    
                    notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                }
                catch(IOException | GitAPIException ex) {
                    displayErrorDialog(Messages.PushModelAction_0, ex);
                }
                finally {
                    monitor.done();
                }
            }
        }
        
        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                    pmDialog.run(false, true, new PushProgressHandler());
                }
                catch(InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
    

}
