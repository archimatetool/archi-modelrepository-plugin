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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.CleanCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.model.IArchimateModel;

/**
 * Abort Changes Action
 * 
 * @author Phillip Beauvoir
 */
public class AbortChangesAction extends AbstractModelAction {
    
    public AbortChangesAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_ABORT));
        setText(Messages.AbortChangesAction_0);
        setToolTipText(Messages.AbortChangesAction_0);
    }

    public AbortChangesAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
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
        
        try(Git git = Git.open(getRepository().getLocalRepositoryFolder())) {
            // Reset to master
            ResetCommand resetCommand = git.reset();
            resetCommand.setRef("refs/heads/master"); //$NON-NLS-1$
            resetCommand.setMode(ResetType.HARD);
            resetCommand.call();
            
            // Clean extra files
            CleanCommand cleanCommand = git.clean();
            cleanCommand.setCleanDirectories(true);
            cleanCommand.call();
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.AbortChangesAction_0, ex);
        }
        
        try {
            loadModelFromGraficoFiles();
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.AbortChangesAction_0, ex);
        }
    }
}
