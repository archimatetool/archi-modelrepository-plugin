/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.process;

import org.archicontribs.modelrepository.merge.IMergeConflictHandler;

/**
 * Interface for a repository listener
 * 
 * @author Phillip Beauvoir
 * @author Michael Ansley
 * 
 */
public interface IRepositoryProcessListener {
    
	/**
	 * Sends a simple event to the event listener for this process
	 * 
	 * @param eventType		A string denoting the type of event, allowing the listener to discriminate
	 * @param object		A string naming the object sending the event
	 * @param summary		A summary of the event, e.g.: to be used in a warning dialog header
	 * @param detail		Event details, e.g.: the message in a warning dialog
	 */
    public void notifyEvent(int eventType, String object, String summary, String detail);

	/**
	 * Sends a request for conflict resolution to the event handler
	 * 
	 * @param conflictHandler	A conflict handler
	 * @return 					Indicator from the listener of whether the request was successfully processed
	 */
    public boolean resolveConflicts(IMergeConflictHandler conflictHandler);
    
}
