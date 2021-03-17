/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.process;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.archicontribs.modelrepository.actions.ProgressMonitorWrapper;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticator;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.BranchStatus;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.merge.HeadlessMergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Commit/Refresh/Publish model action
 * 
 * 1. Expand the model to Grafico files
 * 2. Then Commit of there are changes necessary to Commit
 * 3. Check Proxy
 * 4. If a Refresh/Publish, then 
 * 4.a. Pull (if there is a merge conflict, the actionComplexEvent will be called) and if necessary, commit changes
 * 4.b. Notify pull status
 * 4.c. Notify end of pull
 * 5. If a Publish, then push changes to remote
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 * @author Michael Ansley
 * 
 * @see #PROCESS_COMMIT
 * @see #PROCESS_REFRESH
 * @see #PROCESS_PUBLISH
 * 
 * @see Notify events
 * 		@see #NOTIFY_START_COMMIT - Received before starting the commit process
 * 		@see #NOTIFY_END_COMMIT - Received after ending the commit 
 * 		@see #NOTIFY_START_PULL - Received before starting the pull process
 * 		@see #NOTIFY_END_PULL - Received after ending the pull process
 * 		@see #NOTIFY_START_PUSH - Received before starting the push process
 * 		@see #NOTIFY_END_PUSH - Received after ending the push process
 * 		@see #NOTIFY_PULL_STATUS - Received during the pull process with a pull status value in the summary field
 * 			@see #PULL_STATUS_ERROR
 * 			@see #PULL_STATUS_OK
 * 			@see #PULL_STATUS_UP_TO_DATE
 * 			@see #PULL_STATUS_MERGE_CANCEL
 * 		@see #NOTIFY_LOG_MESSAGE
 * 		@see #NOTIFY_LOG_ERROR
 * 
 * @see Request action events
 * 		@see #ACTION_REQUEST_CONFLICT_RESOLUTION - Use getConflictHandler to fetch the conflicts and handle them;
 * 												   Return either true or false, depending whether to continue or 
 * 												   cancel the merge process
 * 
 */
public class RepositoryModelProcess {

    public static final String NOTIFY_LOG_MESSAGE = Messages.AbstractRepositoryModelProcess_0; //$NON-NLS-1$
    public static final String NOTIFY_LOG_ERROR = Messages.AbstractRepositoryModelProcess_3; //$NON-NLS-1$
    // TODO: Not used, can be removed
    public static final String NOTIFY_STATUS_UPDATE = Messages.AbstractRepositoryModelProcess_4; //$NON-NLS-1$

	public static final int PROCESS_COMMIT = 1;
	public static final int PROCESS_REFRESH = 2;
	public static final int PROCESS_PUBLISH = 3;
	
	public static final String NOTIFY_START_COMMIT = Messages.AbstractRepositoryModelProcess_7; //$NON-NLS-1$
    public static final String NOTIFY_END_COMMIT = Messages.AbstractRepositoryModelProcess_8; //$NON-NLS-1$
    public static final String NOTIFY_START_PULL = Messages.RefreshModelProcess_10;
    public static final String NOTIFY_PULL_STATUS = Messages.RefreshModelProcess_12;
    public static final String NOTIFY_END_PULL = Messages.RefreshModelProcess_13;
    public static final String NOTIFY_START_PUSH = Messages.PushModelProcess_2;
    public static final String NOTIFY_END_PUSH = Messages.PushModelProcess_5;

    public static final String ACTION_REQUEST_CONFLICT_RESOLUTION = Messages.RefreshModelProcess_18;

    public static final int PULL_STATUS_ERROR = -1;
    public static final int PULL_STATUS_OK = 0;
    public static final int PULL_STATUS_UP_TO_DATE = 1;
    public static final int PULL_STATUS_MERGE_CANCEL = 2;

    static final String PREFIX = Messages.PushModelProcess_1;
    
    protected static final int USER_OK = 0;
    protected static final int USER_CANCEL = 1;
    
    protected int fProcessCommand = 0;
	private IArchiRepository fRepository;					// Repository with which this process will work
    private boolean fEnabled = false;						// Is this process enabled?
    protected IRepositoryProcessListener fEventListener;	// Event listener for callbacks
    private UsernamePassword fNpw;							// Security
    private IProgressMonitor pm;

    protected HeadlessMergeConflictHandler fMergeConflictHandler;
    protected String fCommitMessage = "";
    protected boolean fAmend = false;

    /**
     * Constructor
     * 
     * @param model			The model, the repository of which this process will manage
     * @param eventListener	The listener that will respond to events raised by this process
     * @param commitMessage	Set the local commit message, to be used when performing the commit in this process
     */
    public RepositoryModelProcess(int process, IArchimateModel model, IRepositoryProcessListener eventListener, IProgressMonitor progressMonitor, UsernamePassword npw, String commitMessage, boolean amend) {
        if ((process == PROCESS_COMMIT) || (process == PROCESS_REFRESH) || (process == PROCESS_PUBLISH)) {
        	fProcessCommand = process;
        }
		if((model != null) && (model.getFile() != null)) {
            try {
            	fRepository = new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model));
            } catch (Exception e) {
            	fRepository = null;
            }
        }
	    setEnabled(true);
		fEventListener = eventListener;
		if (progressMonitor == null) {
			pm = (IProgressMonitor) new NullProgressMonitor();
		} else {
			pm = progressMonitor;
		}
		fNpw = npw;
        if(StringUtils.isSet(commitMessage)) {
        	fCommitMessage = commitMessage;
        }
        fAmend = amend;
    }
    
    public void run() {

    	String messageSummary = Messages.AbstractRepositoryModelProcess_9;
		if (fProcessCommand==PROCESS_COMMIT) messageSummary = Messages.AbstractRepositoryModelProcess_2;
		if (fProcessCommand==PROCESS_REFRESH) messageSummary = Messages.RefreshModelProcess_0;
		if (fProcessCommand==PROCESS_PUBLISH) messageSummary = Messages.PushModelProcess_0;

		try {

        	// Are we enabled to run?
        	if (!isEnabled()) {
        		logError(messageSummary, Messages.AbstractRepositoryModelProcess_12);
        		return;
        	}
        	
            // Do the Grafico export first, assume that the model has been saved by the user/caller
            try {
                getRepository().exportModelToGraficoFiles();
            }
            catch(Exception ex) {
                logError(messageSummary, ex);
                return;
            }
            
            // Then commit
            try {
                if(getRepository().hasChangesToCommit()) {
            		sendSimpleEvent(NOTIFY_START_COMMIT, messageSummary, Messages.CommitModelProcess_6);
            		getRepository().commitChanges(fCommitMessage, fAmend);
                    getRepository().saveChecksum();
                    sendSimpleEvent(NOTIFY_END_COMMIT, messageSummary, Messages.CommitModelProcess_1);
                }
            }
            catch(Exception ex) {
                logError(messageSummary, ex);
            }

            // Update Proxy
            ProxyAuthenticator.update();


            if ((fProcessCommand == PROCESS_REFRESH) || (fProcessCommand == PROCESS_PUBLISH)) {
	            sendSimpleEvent(NOTIFY_START_PULL, messageSummary, Messages.RefreshModelProcess_6);
	            try {
		            int status = pull();
		            switch (status) {
		            	case PULL_STATUS_UP_TO_DATE: 	sendSimpleEvent(NOTIFY_PULL_STATUS, String.valueOf(PULL_STATUS_UP_TO_DATE), "");
		            									sendSimpleEvent(NOTIFY_END_PULL, messageSummary, Messages.RefreshModelProcess_2);
		            									break;
		            	case PULL_STATUS_OK: 			sendSimpleEvent(NOTIFY_PULL_STATUS, String.valueOf(PULL_STATUS_OK), "");
														sendSimpleEvent(NOTIFY_END_PULL, messageSummary, Messages.RefreshModelProcess_14);
														break;
		            	case PULL_STATUS_MERGE_CANCEL:	sendSimpleEvent(NOTIFY_PULL_STATUS, String.valueOf(PULL_STATUS_MERGE_CANCEL), "");
														sendSimpleEvent(NOTIFY_END_PULL, messageSummary, Messages.RefreshModelProcess_15);
		            									break;
		            	case PULL_STATUS_ERROR:			sendSimpleEvent(NOTIFY_PULL_STATUS, String.valueOf(PULL_STATUS_ERROR), "");
		            									sendSimpleEvent(NOTIFY_END_PULL, messageSummary, Messages.RefreshModelProcess_16);
		            									break;
		            	default:						logError(messageSummary, Messages.RefreshModelProcess_11);
		            									break;
		            }
		            
	                // Push
	                if((status == PULL_STATUS_OK || status == PULL_STATUS_UP_TO_DATE) && (fProcessCommand == PROCESS_PUBLISH)) {
	                    sendSimpleEvent(NOTIFY_START_PUSH, messageSummary, Messages.PushModelProcess_3);
	                    Iterable<PushResult> pushResult = getRepository().pushToRemote(getUsernamePassword(), new ProgressMonitorWrapper(getProgressMonitor()));
	                    
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
	                        logError(messageSummary, sb.toString());
	                    }
	                    sendSimpleEvent(NOTIFY_END_PUSH, messageSummary, Messages.PushModelProcess_5);
	                }
	
	            } catch(Exception ex) {
	            	logError(messageSummary, ex);
	            }
	            finally {
	                try {
	                    saveChecksum();
	                }
	                catch(IOException ex) {
	                    ex.printStackTrace();
	                }                
	                // Clear Proxy
	                ProxyAuthenticator.clear();
	            }
            }
        }
        catch(Exception ex) {
            logError(messageSummary, ex);
        }
    }
    /**
     * Implement the Git pull process
     * 
     * @return int indicating the result of the process
     * @see #AbstractRepositoryModelProcess.NOTIFY_STATUS_UPDATE
     * @see #PULL_STATUS_OK
     * @see #PULL_STATUS_UP_TO_DATE
     * @see #PULL_STATUS_MERGE_CANCEL
     * @see #PULL_STATUS_ERROR
     */
    protected int pull() throws IOException, GitAPIException  {
        PullResult pullResult = null;
        
        getProgressMonitor().subTask(Messages.RefreshModelProcess_6);
        
        try {
            pullResult = getRepository().pullFromRemote(getUsernamePassword(), new ProgressMonitorWrapper(getProgressMonitor()));
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

        getProgressMonitor().subTask(Messages.RefreshModelProcess_7);
        
        BranchStatus branchStatus = getRepository().getBranchStatus();
        
        // Setup the Graphico Model Loader
        GraficoModelLoader loader = new GraficoModelLoader(getRepository());

        // Merge failure
        if(!pullResult.isSuccessful() && pullResult.getMergeResult().getMergeStatus() == MergeStatus.CONFLICTING) {
            // Get the remote ref name
            String remoteRef = branchStatus.getCurrentRemoteBranch().getFullName();
            
            // Try to handle the merge conflict
            fMergeConflictHandler = new HeadlessMergeConflictHandler(pullResult.getMergeResult(), remoteRef,
                    getRepository());
            
            try {
            	fMergeConflictHandler.init(getProgressMonitor());
            }
            catch(IOException | GitAPIException ex) {
            	fMergeConflictHandler.resetToLocalState(); // Clean up

                if(ex instanceof CanceledException) {
                    return PULL_STATUS_MERGE_CANCEL;
                }

                throw ex;
            }
            
            //boolean result = handler.resolveConflicts(strMessage);
            boolean result = fEventListener.actionComplexEvent(ACTION_REQUEST_CONFLICT_RESOLUTION, getLogPrefix(), this);
            
            if(result) {
            	fMergeConflictHandler.merge();
            }
            // User cancelled - we assume they committed all changes so we can reset
            else {
            	fMergeConflictHandler.resetToLocalState();
                return PULL_STATUS_MERGE_CANCEL;
            }
            
            getProgressMonitor().subTask(Messages.RefreshModelProcess_8);
            // Reload the model from the Grafico XML files
            try {
            	loader.loadModel();
            }
            catch(IOException ex) {
            	fMergeConflictHandler.resetToLocalState(); // Clean up
            	throw ex;
            }
        } else { 
            getProgressMonitor().subTask(Messages.RefreshModelProcess_8);
		    // Reload the model from the Grafico XML files
			loader.loadModel();
        }
        
		  // Do a commit if needed 
        if(getRepository().hasChangesToCommit()) {
		  
        	String commitMessage = NLS.bind(Messages.RefreshModelProcess_1, branchStatus.getCurrentLocalBranch().getShortName());
		  
        	// Did we restore any missing objects?
        	String restoredObjects = loader.getRestoredObjectsAsString();
		  
        	// Add to commit message 
        	if(restoredObjects != null) {
        		commitMessage += "\n\n" + Messages.RefreshModelProcess_3 + "\n" + restoredObjects; //$NON-NLS-1$ //$NON-NLS-2$ 
        	}
		  
        	// TODO - not sure if amend should be false or true here?
        	getRepository().commitChanges(commitMessage, false);
        }
        
        return PULL_STATUS_OK;
    }

	/**
	 * Check if the conditions for enabling this process are fulfilled
	 * 
	 * @return true if this process can be enabled
	 * @see #setEnabled(boolean)
	 */
	protected boolean canBeEnabled() {
		return ((getRepository() != null) && (getRepository().getLocalRepositoryFolder().exists()) && 
		        ((fProcessCommand == PROCESS_COMMIT) || (fProcessCommand == PROCESS_REFRESH) || (fProcessCommand == PROCESS_PUBLISH)));
	}
    
    public HeadlessMergeConflictHandler getConflictHandler() {
    	return fMergeConflictHandler;
    }
	
    /**
     * Get the repository of this process
     * 
     * @return repository			The repository managed by this process
     */
	public IArchiRepository getRepository() {
	    return fRepository;
	}
	 
    /**
     * Attempts to set whether this process is enabled, although it will be constrained by 
     * calling canBeEnabled
     * 
     * @see #canBeEnabled()
     */
	public void setEnabled(boolean enabled) {
		if (enabled != fEnabled) {
			fEnabled = enabled && canBeEnabled();
		}
	}
	 
    /**
     * Whether this process is enabled
     * 
     * @see #setEnabled()
     */
	public boolean isEnabled() {
		return fEnabled;
	}

	/**
	 * Sends a simple event to the event listener for this process
	 * 
	 * @param eventType		A string denoting the type of event, allowing the listener to discriminate
	 * @param summary		A summary of the event, e.g.: to be used in a warning dialog header
	 * @param detail		Event details, e.g.: the message in a warning dialog
	 */
    protected IProgressMonitor getProgressMonitor() {
    	return pm;
    }
    
	/**
	 * Sends a simple event to the event listener for this process
	 * 
	 * @param eventType		A string denoting the type of event, allowing the listener to discriminate
	 * @param summary		A summary of the event, e.g.: to be used in a warning dialog header
	 * @param detail		Event details, e.g.: the message in a warning dialog
	 */
    protected void sendSimpleEvent(String eventType, String summary, String detail) {
    	if (fEventListener!=null) {
    		fEventListener.actionSimpleEvent(eventType, getLogPrefix(), summary, detail);
    	}
    }
    
	/**
	 * Sends a complex event to the event listener for this process
	 * 
	 * @param eventType			A string denoting the type of event, allowing the listener to discriminate
	 * @return 					Indicator from the listener of whether the request was successfully processed
	 */
    protected boolean sendComplexEvent(String eventType) {
    	if (fEventListener!=null) {
    		return fEventListener.actionComplexEvent(eventType, getLogPrefix(), this);
    	}
    	logMessage(Messages.AbstractRepositoryModelProcess_11, Messages.AbstractRepositoryModelProcess_10);
    	return true;
    }

    /**
     * Get user name and password
     * 
     * @return	UsernamePassword with the existing credentials
     */
    public UsernamePassword getUsernamePassword() {
    	return fNpw;
    }
    
    /**
     * Set user name and password
     * 
     * @param npw	UsernamePassword with credentials to use during the running of this process
     */
    public void setUsernamePassword(UsernamePassword npw) {
    	fNpw = npw;
    }
    
    /**
     * Save checksum
     * 
     * @throws IOException	If an input or output exception occurs
     */
    protected void saveChecksum() throws IOException {
        fRepository.saveChecksum();
    }
    
    /**
     * Convenience function to log a message
     * 
	 * @param title		A summary of the event, e.g.: to be used in a warning dialog header
	 * @param message	Event details, e.g.: the message in a warning dialog
     */
    protected void logMessage(String title, String message) {
		sendSimpleEvent(NOTIFY_LOG_MESSAGE, title, message);
    }
    
    /**
     * Convenience function to log an error message
     * 
	 * @param title		A summary of the event, e.g.: to be used in an error dialog header
	 * @param message	Event details, e.g.: the message in an error dialog
     */
    protected void logError(String title, String message) {
		sendSimpleEvent(NOTIFY_LOG_ERROR, title, message);
    }
        
    /**
     * Convenience function to log an error message using an Exception object
     * 
	 * @param title		A summary of the event, e.g.: to be used in an error dialog header
	 * @param ex		An exception object to be used to determine the message details
     */
    protected void logError(String title, Throwable ex) {
        ex.printStackTrace();
        
        String message = ex.getMessage();
        
        if(ex instanceof InvocationTargetException) {
            ex = ex.getCause();
        }
        
        if(ex instanceof JGitInternalException) {
            ex = ex.getCause();
        }
        
        if(ex != null) {
            message = ex.getMessage();
        }
        
        logError(title, message);
    }
    
    
	/**
	 * Returns a standard log prefix, should be overridden by all subclasses to ensure ease of understanding the log
	 * 
	 * @return Prefix that can be used by logging and event handling
	 */
    protected String getLogPrefix() {
        return ""; //$NON-NLS-1$
    }
}
