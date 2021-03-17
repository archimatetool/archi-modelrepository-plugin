/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.process.IRepositoryProcessListener;
import org.archicontribs.modelrepository.process.RepositoryModelProcess;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Commit Model Action
 * 
 * 1. Offer to save the model
 * 2. Create Grafico files from the model
 * 3. Check if there is anything to Commit
 * 4. Show Commit dialog
 * 5. Commit
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class CommitModelAction2 extends AbstractModelAction implements IRepositoryProcessListener {
    
    public CommitModelAction2(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_COMMIT));
        setText(Messages.CommitModelAction_0);
        setToolTipText(Messages.CommitModelAction_0);
    }

    public CommitModelAction2(IWorkbenchWindow window, IArchimateModel model) {
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
                return;
            } else {
                try {
                    if (!IEditorModelManager.INSTANCE.saveModel(model)) {
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
            return;
        }

        // Then Commit
        try {
            if(getRepository().hasChangesToCommit()) {
                if(shouldCommitChanges()) {
                    try {

                        RepositoryModelProcess process = new RepositoryModelProcess(RepositoryModelProcess.PROCESS_COMMIT,
                        													model, 
                        													(IRepositoryProcessListener) this, 
                        													new NullProgressMonitor(),
                        													null,
                        													fCommitMessage, 
                        													fAmend);
                        process.run();
                        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                    }
                    catch(Exception ex) {
                        displayErrorDialog(Messages.AbstractModelAction_6, ex);
                        return;
                    }
                }
            }
            else {
                MessageDialog.openInformation(fWindow.getShell(),
                        Messages.CommitModelAction_0,
                        Messages.CommitModelAction_2);
            }
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.CommitModelAction_0, ex);
        }
        
    }

	@Override
	public void actionSimpleEvent(String eventType, String object, String summary, String detail) {
		if (eventType.equals(RepositoryModelProcess.NOTIFY_LOG_ERROR)) {
			displayErrorDialog(summary, detail);
		} else if (eventType.equals(RepositoryModelProcess.NOTIFY_LOG_MESSAGE)){
            MessageDialog.openInformation(fWindow.getShell(), summary, detail);
		} else if (eventType.equals(RepositoryModelProcess.NOTIFY_END_COMMIT) ) {
            notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
		}
	}

	@Override
	public boolean actionComplexEvent(String eventType, String object, RepositoryModelProcess process) {
		// TODO Auto-generated method stub
		return true;
	}
}
