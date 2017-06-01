/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Abort Changes Action
 * 
 * @author Phillip Beauvoir
 */
public class AbortChangesAction extends AbstractModelAction {
    
    private IArchimateModel fModel;
	
    public AbortChangesAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_ABORT));
        setText(Messages.AbortChangesAction_0);
        setToolTipText(Messages.AbortChangesAction_0);
    }

    public AbortChangesAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        fModel = model;
        if(fModel != null) {
            setLocalRepositoryFolder(fModel.getFile().getParentFile().getParentFile());
        }
    }

    @Override
    public void run() {
        boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                Messages.AbortChangesAction_0,
                Messages.AbortChangesAction_1);
        if(!response) {
            return;
        }
        
        try(Git git = Git.open(getLocalRepositoryFolder())) {
            // Reset to master
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef("refs/heads/master"); //$NON-NLS-1$
            resetCommand.setMode(ResetType.HARD);
            resetCommand.call();
            
            // Clean extra files
            CleanCommand cleanCommand = git.clean();
            cleanCommand.call();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.AbortChangesAction_0, ex);
        }
        
        try {
            IArchimateModel model = fModel;
            
            if(model == null) {
                model = GraficoUtils.locateModel(getLocalRepositoryFolder());
            }
            
            IEditorModelManager.INSTANCE.closeModel(model);
            loadModelFromGraficoFiles(getLocalRepositoryFolder());
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.AbortChangesAction_0, ex);
        }
    }
}
