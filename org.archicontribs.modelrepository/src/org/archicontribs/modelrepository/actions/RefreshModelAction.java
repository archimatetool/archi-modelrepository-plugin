/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticator;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.merge.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.osgi.util.NLS;
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
    
    protected static final int PULL_STATUS_ERROR = -1;
    protected static final int PULL_STATUS_OK = 0;
    protected static final int PULL_STATUS_UP_TO_DATE = 1;
    protected static final int PULL_STATUS_MERGE_CANCEL = 2;
    
    protected static final int USER_OK = 0;
    protected static final int USER_CANCEL = 1;
    
    public RefreshModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REFRESH));
        setText(Messages.RefreshModelAction_0);
        setToolTipText(Messages.RefreshModelAction_0);
    }
    
    public RefreshModelAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
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
            // UsernamePassword will be null if using SSH
            UsernamePassword npw = getUsernamePassword();
            // User cancelled on HTTP
            if(npw == null && GraficoUtils.isHTTP(getRepository().getOnlineRepositoryURL())) {
                return;
            }

            // Do main action with PM dialog
            Display.getCurrent().asyncExec(new Runnable() {
                @Override
                public void run() {
                    try {
                        ProgressMonitorDialog pmDialog = new ProgressMonitorDialog(fWindow.getShell());
                        
                        pmDialog.run(false, true, new IRunnableWithProgress() {
                            @Override
                            public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                try {
                                    // Update Proxy
                                    ProxyAuthenticator.update();
                                    
                                    monitor.beginTask(Messages.RefreshModelAction_5, -1);
                                    int status = pull(npw, pmDialog);
                                    if(status == PULL_STATUS_UP_TO_DATE) {
                                        pmDialog.getShell().setVisible(false);
                                        MessageDialog.openInformation(fWindow.getShell(), Messages.RefreshModelAction_0, Messages.RefreshModelAction_2);
                                    }
                                }
                                catch(Exception ex) {
                                    pmDialog.getShell().setVisible(false);
                                    displayErrorDialog(Messages.RefreshModelAction_0, ex);
                                }
                                finally {
                                    try {
                                        saveChecksumAndNotifyListeners();
                                    }
                                    catch(IOException ex) {
                                        ex.printStackTrace();
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
        catch(Exception ex) {
            displayErrorDialog(Messages.RefreshModelAction_0, ex);
        }
    }
    
    protected int init() throws IOException, GitAPIException {
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return USER_CANCEL;
            }
        }
        
        // Do the Grafico Export first
        getRepository().exportModelToGraficoFiles();
        
        // Then offer to Commit
        if(getRepository().hasChangesToCommit()) {
            if(!offerToCommitChanges()) {
                return USER_CANCEL;
            }
            notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
        }
        
        return USER_OK;
    }
    
    protected int pull(UsernamePassword npw, ProgressMonitorDialog pmDialog) throws IOException, GitAPIException  {
        PullResult pullResult = null;
        
        pmDialog.getProgressMonitor().subTask(Messages.RefreshModelAction_6);
        Display.getCurrent().readAndDispatch(); // update dialog
        
        try {
            pullResult = getRepository().pullFromRemote(npw, new ProgressMonitorWrapper(pmDialog.getProgressMonitor()));
        }
        catch(Exception ex) {
            // If this exception is thrown then the remote doesn't have the ref which can happen when pulling on a branch,
            // So quietly absorb this and return OK
            if(ex instanceof RefNotAdvertisedException) {
                return PULL_STATUS_OK;
            }
            
            throw ex;
        }
        
        // Check for tracking updates
        FetchResult fetchResult = pullResult.getFetchResult();
        boolean newTrackingRefUpdates = fetchResult != null && !fetchResult.getTrackingRefUpdates().isEmpty();
        
        // Merge is already up to date...
        if(pullResult.getMergeResult().getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE) {
            // Check if any tracked refs were updated
            if(newTrackingRefUpdates) {
                return PULL_STATUS_OK;
            }
            
            return PULL_STATUS_UP_TO_DATE;
        }
        
        pmDialog.getProgressMonitor().subTask(Messages.RefreshModelAction_7);
        
        BranchStatus branchStatus = getRepository().getBranchStatus();
        
        // Setup the Graphico Model Loader
        GraficoModelLoader loader = new GraficoModelLoader(getRepository());

        // Merge failure
        if(!pullResult.isSuccessful() && pullResult.getMergeResult().getMergeStatus() == MergeStatus.CONFLICTING) {
            // Get the remote ref name
            String remoteRef = branchStatus.getCurrentRemoteBranch().getFullName();
            
            // Try to handle the merge conflict
            MergeConflictHandler handler = new MergeConflictHandler(pullResult.getMergeResult(), remoteRef,
                    getRepository(), fWindow.getShell());
            
            try {
                handler.init(pmDialog.getProgressMonitor());
            }
            catch(IOException | GitAPIException ex) {
                handler.resetToLocalState(); // Clean up

                if(ex instanceof CanceledException) {
                    return PULL_STATUS_MERGE_CANCEL;
                }

                throw ex;
            }
            
            String dialogMessage = NLS.bind(Messages.RefreshModelAction_4, branchStatus.getCurrentLocalBranch().getShortName());
            
            pmDialog.getShell().setVisible(false);
            
            boolean result = handler.openConflictsDialog(dialogMessage);
            
            pmDialog.getShell().setVisible(true);

            if(result) {
                handler.merge();
            }
            // User cancelled - we assume they committed all changes so we can reset
            else {
                handler.resetToLocalState();
                return PULL_STATUS_MERGE_CANCEL;
            }
            
            // We now have to check if model can be reloaded
            pmDialog.getProgressMonitor().subTask(Messages.RefreshModelAction_8);
            
            // Reload the model from the Grafico XML files
            try {
            	loader.loadModel();
            }
            catch(IOException ex) {
            	handler.resetToLocalState(); // Clean up
            	throw ex;
            }
        } else { 
		    // Reload the model from the Grafico XML files
		    pmDialog.getProgressMonitor().subTask(Messages.RefreshModelAction_8);
			loader.loadModel();
        }
        
        // Do a commit if needed
        if(getRepository().hasChangesToCommit()) {
            pmDialog.getProgressMonitor().subTask(Messages.RefreshModelAction_9);
            
            String commitMessage = NLS.bind(Messages.RefreshModelAction_1, branchStatus.getCurrentLocalBranch().getShortName());
            
            // Did we restore any missing objects?
            String restoredObjects = loader.getRestoredObjectsAsString();
            
            // Add to commit message
            if(restoredObjects != null) {
                commitMessage += "\n\n" + Messages.RefreshModelAction_3 + "\n" + restoredObjects; //$NON-NLS-1$ //$NON-NLS-2$
            }

            // TODO - not sure if amend should be false or true here?
            getRepository().commitChanges(commitMessage, false);
        }
        
        return PULL_STATUS_OK;
    }
}
