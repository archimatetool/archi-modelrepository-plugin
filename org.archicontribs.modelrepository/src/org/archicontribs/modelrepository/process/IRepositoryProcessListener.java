/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.process;

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
    public void actionSimpleEvent(String eventType, String object, String summary, String detail);

	/**
	 * Sends a complex event to the event listener for this process
	 * 
	 * @param eventType		A string denoting the type of event, allowing the listener to discriminate
	 * @param object		A string naming the object sending the event
	 * @param process		The process object sending the event, this is to allow the event handler
	 * 						to make changes to anything exposed by the process, for example, to open 
	 * 						a dialog for the user to make a choice, and then updating the process with 
	 * 						the results of the choice
	 * @return 				Indicator from the listener of whether the request was successfully processed
	 */
    public boolean actionComplexEvent(String eventType, String object, RepositoryModelProcess process);
    
}
