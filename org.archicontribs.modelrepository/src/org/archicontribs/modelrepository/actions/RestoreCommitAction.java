/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Restore to a particular commit
 */
public class RestoreCommitAction extends AbstractModelAction {
    
    private RevCommit fCommit;
	
    public RestoreCommitAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_SYNCED));
        setText(Messages.RestoreCommitAction_0);
        setToolTipText(Messages.RestoreCommitAction_0);
    }

    public void setCommit(RevCommit commit) {
        fCommit = commit;
        setEnabled(shouldBeEnabled());
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
            displayErrorDialog(Messages.RestoreCommitAction_0, ex);
            return;
        }
        
        try {
            // If there are changes to commit then they'll have to be committed first or abandoned
            if(getRepository().hasChangesToCommit()) {
                if(!MessageDialog.openConfirm(fWindow.getShell(),
                        Messages.RestoreCommitAction_0,
                        Messages.UndoLastCommitAction_2)) {
                    return;
                }
            }
            // Else, confirm
            else {
                boolean response = MessageDialog.openConfirm(fWindow.getShell(),
                        Messages.RestoreCommitAction_0,
                        Messages.RestoreCommitAction_1);

                if(!response) {
                    return;
                }
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RestoreCommitAction_0, ex);
            return;
        }
        
        // Delete the content folders first
        try {
            File modelFolder = new File(getRepository().getLocalRepositoryFolder(), IGraficoConstants.MODEL_FOLDER);
            FileUtils.deleteFolder(modelFolder);
            modelFolder.mkdirs();

            File imagesFolder = new File(getRepository().getLocalRepositoryFolder(), IGraficoConstants.IMAGES_FOLDER);
            FileUtils.deleteFolder(imagesFolder);
            imagesFolder.mkdirs();

        }
        catch(IOException ex) {
            displayErrorDialog(Messages.RestoreCommitAction_0, ex);
            return;
        }
        
        // Walk the tree and get the contents of the commit
        try(Repository repository = Git.open(getRepository().getLocalRepositoryFolder()).getRepository()) {
            try(TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(fCommit.getTree());
                treeWalk.setRecursive(true);

                while(treeWalk.next()) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    
                    File file = new File(getRepository().getLocalRepositoryFolder(), treeWalk.getPathString());
                    file.getParentFile().mkdirs();
                    
                    GraficoUtils.writeObjectToFileWithSystemLineEndings(file, loader);
                }
            }
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.RestoreCommitAction_0, ex);
            return;
        }

        // Reload the model from the Grafico XML files
        try {
            IArchimateModel graficoModel = new GraficoModelLoader(getRepository()).loadModel();
            // If this is null then it failed because of no model in this commit
            if(graficoModel == null) {
                // Reset
                getRepository().resetToRef("refs/heads/master"); //$NON-NLS-1$
                MessageDialog.openError(fWindow.getShell(), Messages.RestoreCommitAction_0, Messages.RestoreCommitAction_2);
                return;
            }
        }
        catch(IOException | GitAPIException ex) {
            displayErrorDialog(Messages.RestoreCommitAction_0, ex);
        }
        
        // Commit changes
        try {
            getRepository().commitChanges(Messages.RestoreCommitAction_3 + " '" + fCommit.getShortMessage() + "'", false); //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch(GitAPIException | IOException ex) {
            displayErrorDialog(Messages.RestoreCommitAction_0, ex);
        }
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
    }
    
    
    @Override
    protected boolean shouldBeEnabled() {
        boolean isHead = false;
        try {
            isHead = isCommitLocalHead();
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
        
        return fCommit != null && getRepository() != null && !isHead;
    }
    
    protected boolean isCommitLocalHead() throws IOException {
        if(fCommit == null) {
            return false;
        }
        
        try(Repository repo = Git.open(getRepository().getLocalRepositoryFolder()).getRepository()) {
            ObjectId headID = repo.resolve("refs/heads/master"); //$NON-NLS-1$
            ObjectId commitID = fCommit.getId();
            return commitID.equals(headID);
        }
    }
}
