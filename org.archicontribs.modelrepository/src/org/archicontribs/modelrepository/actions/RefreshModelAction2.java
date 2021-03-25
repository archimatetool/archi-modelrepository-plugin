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
import org.archicontribs.modelrepository.authentication.EncryptedCredentialsStorage;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.merge.IMergeConflictHandler;
import org.archicontribs.modelrepository.merge.MergeConflictHandler;
import org.archicontribs.modelrepository.process.IRepositoryProcessListener;
import org.archicontribs.modelrepository.process.RepositoryModelProcess;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.errors.GitAPIException;
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
public class RefreshModelAction2 extends AbstractModelAction implements IRepositoryProcessListener {
    
    protected static final int PULL_STATUS_ERROR = -1;
    protected static final int PULL_STATUS_OK = 0;
    protected static final int PULL_STATUS_UP_TO_DATE = 1;
    protected static final int PULL_STATUS_MERGE_CANCEL = 2;
    
    protected static final int USER_OK = 0;
    protected static final int USER_CANCEL = 1;
    
    public RefreshModelAction2(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REFRESH));
        setText(Messages.RefreshModelAction_0);
        setToolTipText(Messages.RefreshModelAction_0);
    }
    
    public RefreshModelAction2(IWorkbenchWindow window, IArchimateModel model) {
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
            if(!shouldSaveModel(model)) {
            	// User doesn't want to save the model, no further action
                return;
            } else {
                try {
                    if (!IEditorModelManager.INSTANCE.saveModel(model)) {
                    	// Model could not be saved, no further action
                    	return;
                    }
                }
                catch(IOException ex) {
                    displayErrorDialog(Messages.AbstractModelAction_1, ex);
                }
            }
        }
        
        // Do the Grafico export
        try {
            getRepository().exportModelToGraficoFiles();
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.CommitModelAction_0, ex);
            // Could not complete the grafico export, no further action
            return;
        }

    	try {
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

            // Then offer to Commit
            if(getRepository().hasChangesToCommit()) {
            	// Check whether to commit and if yes, get the commit message and amend flag
                if (!shouldCommitChanges()) {
                	// If the user doesn't want to commit the changes, no further action
                	return;
                }
            }
            try {
            	IRepositoryProcessListener l = this;
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
                                        monitor.beginTask(Messages.RefreshModelAction_5, -1);
                                        RepositoryModelProcess process = new RepositoryModelProcess(RepositoryModelProcess.PROCESS_REFRESH,
                                        		model, 
												l, 
												monitor,
												npw,
												fCommitMessage,
												fAmend);
									    process.run();
									    notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
									    notifyChangeListeners(IRepositoryListener.BRANCHES_CHANGED);
									    monitor.done();
                                        pmDialog.getShell().setVisible(false);
                                    }
                                    catch(Exception ex) {
                                        displayErrorDialog(Messages.RefreshModelAction_0, ex);
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
                return;
            }
        }
        catch(GeneralSecurityException ex) {
            displayCredentialsErrorDialog(ex);
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.RefreshModelAction_0, ex);
        }
    }

	@Override
	public void notifyEvent(int eventType, String object, String summary, String detail) {
        if(eventType==RepositoryModelProcess.NOTIFY_PULL_STATUS) {
        	if (summary.equals(String.valueOf(RepositoryModelProcess.PULL_STATUS_UP_TO_DATE))) {
        		MessageDialog.openInformation(fWindow.getShell(), Messages.RefreshModelAction_0, Messages.RefreshModelAction_2);
        	}
        } else if (eventType==RepositoryModelProcess.NOTIFY_LOG_ERROR) {
    		MessageDialog.openError(fWindow.getShell(), summary, detail);
        } else if (eventType==RepositoryModelProcess.NOTIFY_LOG_MESSAGE) {
    		MessageDialog.openInformation(fWindow.getShell(), summary, detail);
        }
        
		
	}

	@Override
	public boolean resolveConflicts(IMergeConflictHandler conflictHandler) {
		try {
            String dialogMessage = NLS.bind(Messages.RefreshModelAction_4, conflictHandler.getArchiRepository().getBranchStatus().getCurrentLocalBranch().getShortName());

            MergeConflictHandler c = (MergeConflictHandler) conflictHandler;
            return c.openConflictsDialog(dialogMessage);
	        
		} catch (IOException | GitAPIException ex) {
    		MessageDialog.openError(fWindow.getShell(), Messages.RefreshModelAction_0, ex.getMessage());
			return false;
		}
	}
}
