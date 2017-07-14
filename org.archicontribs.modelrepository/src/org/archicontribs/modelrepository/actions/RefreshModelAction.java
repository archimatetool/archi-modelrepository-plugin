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
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
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
    
    public RefreshModelAction(IWorkbenchWindow window, IArchimateModel model) {
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
        try {
            getRepository().exportModelToGraficoFiles();
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.RefreshModelAction_0, ex);
            return;
        }
        
        // Then offer to Commit
        try {
            if(getRepository().hasChangesToCommit()) {
                if(!offerToCommitChanges()) {
                    return;
                }
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RefreshModelAction_3, ex);
            return;
        }
        
        // Get User Credentials first
        final UsernamePassword up = getUserNameAndPasswordFromCredentialsFileOrDialog(fWindow.getShell());
        if(up == null) {
            return;
        }
        
        /**
         * Wrapper class to handle progress monitor
         */
        class RefreshProgressHandler extends ProgressHandler {
            private PullResult pullResult;

            @Override
            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                super.run(monitor);
                
                try {
                    monitor.beginTask(Messages.RefreshModelAction_6, IProgressMonitor.UNKNOWN);
                    
                    // Proxy
                    ProxyAuthenticater.update(getRepository().getOnlineRepositoryURL());
                    
                    // First we need to Pull
                    try {
                        pullResult = getRepository().pullFromRemote(up.getUsername(), up.getPassword(), this);
                    }
                    catch(Exception ex) {
                        // Remote is blank with no master ref
                        if(ex instanceof RefNotAdvertisedException) {
                            return;
                        }
                        else {
                            throw ex;
                        }
                    }
                    
                    monitor.done();
                    
                    // Start a new thread because of dialog blocking
                    Display.getCurrent().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            // Merge failure
                            if(!pullResult.isSuccessful()) {
                                try {
                                    // Try to handle the merge conflict
                                    MergeConflictHandler handler = new MergeConflictHandler(pullResult.getMergeResult(), getRepository(), fWindow.getShell());
                                    boolean result = handler.checkForMergeConflicts();
                                    if(result) {
                                        handler.merge();
                                    }
                                    // User cancelled - we assume they committed all changes so we can reset
                                    else {
                                        handler.resetToLocalState();
                                        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                                        return;
                                    }
                                }
                                catch(IOException | GitAPIException ex) {
                                    displayErrorDialog(Messages.RefreshModelAction_0, ex);
                                }
                            }
                            
                            try {
                                // Reload the model from the Grafico XML files
                                loadModelFromGraficoFiles();
                                
                                // Do a commit if needed
                                if(getRepository().hasChangesToCommit()) {
                                    getRepository().commitChanges(Messages.RefreshModelAction_2, false);
                                }
                            }
                            catch(IOException | GitAPIException ex) {
                                displayErrorDialog(Messages.RefreshModelAction_0, ex);
                            }
                            
                            notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
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
        }

        Display.getCurrent().asyncExec(new Runnable() {
            @Override
            public void run() {
                try {
                    ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                    pmDialog.run(false, true, new RefreshProgressHandler());
                }
                catch(InvocationTargetException | InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
