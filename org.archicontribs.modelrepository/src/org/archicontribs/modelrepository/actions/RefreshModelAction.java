/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
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
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

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
            UsernamePassword up = init();
            if(up != null) {
                int status = pull(up);
                if(status == PULL_STATUS_UP_TO_DATE) {
                    MessageDialog.openInformation(fWindow.getShell(), Messages.RefreshModelAction_0, Messages.RefreshModelAction_2);
                }
            }
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.RefreshModelAction_0, ex);
        }
    }
    
    protected UsernamePassword init() throws IOException, GitAPIException {
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return null;
            }
        }
        
        // Do the Grafico Export first
        getRepository().exportModelToGraficoFiles();
        
        // Then offer to Commit
        if(getRepository().hasChangesToCommit()) {
            if(!offerToCommitChanges()) {
                return null;
            }
            notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
        }
        
        // Get User Credentials first
        UsernamePassword up = getUserNameAndPasswordFromCredentialsFileOrDialog(fWindow.getShell());
        if(up == null) {
            return null;
        }
        
        // Proxy update
        ProxyAuthenticator.update(getRepository().getOnlineRepositoryURL());

        return up;
    }
    
    protected int pull(UsernamePassword up) throws Exception {
        PullResult[] pullResult = new PullResult[1];
        Exception[] exception = new Exception[1];
        
        IProgressService ps = PlatformUI.getWorkbench().getProgressService();
        ps.busyCursorWhile(new IRunnableWithProgress() {
            @Override
            public void run(IProgressMonitor pm) {
                try {
                    pullResult[0] = getRepository().pullFromRemote(up.getUsername(), up.getPassword(), new ProgressMonitorWrapper(pm));
                }
                catch(GitAPIException | IOException ex) {
                    exception[0] = ex;
                }
            }
        });
        
        if(exception[0] != null) {
            // If this exception is thrown then the remote is empty with no master ref, so quietly absorb this and return
            if(exception[0] instanceof RefNotAdvertisedException) {
                return PULL_STATUS_OK;
            }
            
            throw exception[0];
        }
        
        // Check for tracking updates
        FetchResult fetchResult = pullResult[0].getFetchResult();
        boolean newTrackingRefUpdates = fetchResult != null && !fetchResult.getTrackingRefUpdates().isEmpty();
        if(newTrackingRefUpdates) {
            notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
        }
        
        // Merge is already up to date...
        if(pullResult[0].getMergeResult().getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE) {
            // Check if any tracked refs were updated
            if(newTrackingRefUpdates) {
                notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                return PULL_STATUS_OK;
            }
            
            return PULL_STATUS_UP_TO_DATE;
        }

        BranchStatus branchStatus = getRepository().getBranchStatus();

        // Merge failure
        if(!pullResult[0].isSuccessful() && pullResult[0].getMergeResult().getMergeStatus() == MergeStatus.CONFLICTING) {
            // Get the remote ref name
            String remoteRef = branchStatus.getCurrentRemoteBranch().getFullName();
            
            // Try to handle the merge conflict
            MergeConflictHandler handler = new MergeConflictHandler(pullResult[0].getMergeResult(), remoteRef,
                    getRepository(), fWindow.getShell());
            
            ps.busyCursorWhile(new IRunnableWithProgress() {
                @Override
                public void run(IProgressMonitor pm) {
                    try {
                        handler.init(pm);
                    }
                    catch(IOException | GitAPIException ex) {
                        exception[0] = ex;
                    }
                }
            });
            
            if(exception[0] != null) {
                handler.resetToLocalState(); // Clean up

                if(exception[0] instanceof CanceledException) {
                    return PULL_STATUS_MERGE_CANCEL;
                }
                
                throw exception[0];
            }
            
            String dialogMessage = NLS.bind(Messages.RefreshModelAction_4, branchStatus.getCurrentLocalBranch().getShortName());
            
            boolean result = handler.openConflictsDialog(dialogMessage);

            if(result) {
                handler.merge();
            }
            // User cancelled - we assume they committed all changes so we can reset
            else {
                handler.resetToLocalState();
                notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                return PULL_STATUS_MERGE_CANCEL;
            }
        }
        
        // Reload the model from the Grafico XML files
        GraficoModelLoader loader = new GraficoModelLoader(getRepository());
        loader.loadModel();
        
        // Do a commit if needed
        if(getRepository().hasChangesToCommit()) {
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
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);

        return PULL_STATUS_OK;
    }
}
