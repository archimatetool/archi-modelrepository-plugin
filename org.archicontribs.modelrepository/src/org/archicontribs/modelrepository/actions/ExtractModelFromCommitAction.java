/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.ui.IWorkbenchWindow;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Checkout a commit and extract the .archimate file from it
 */
public class ExtractModelFromCommitAction extends AbstractModelAction {
    
    private RevCommit fCommit;
	
    public ExtractModelFromCommitAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IArchiImages.ImageFactory.getImageDescriptor(IArchiImages.ICON_MODELS));
        setText(Messages.ExtractModelFromCommitAction_0);
        setToolTipText(Messages.ExtractModelFromCommitAction_0);
    }

    public void setCommit(RevCommit commit) {
        fCommit = commit;
        setEnabled(shouldBeEnabled());
    }
    
    @Override
    public void run() {
        boolean confirm = MessageDialog.openConfirm(fWindow.getShell(), Messages.ExtractModelFromCommitAction_1, Messages.ExtractModelFromCommitAction_3);
        
        if(!confirm) {
            return;
        }
        
        File tempOutputFolder = getTempFolder();
        
        deleteFolder(tempOutputFolder);
        
        // Wlak the tree and get the contents of the commit
        try(Repository repository = Git.open(getRepository().getLocalRepositoryFolder()).getRepository()) {
            try(TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(fCommit.getTree());
                treeWalk.setRecursive(true);

                while(treeWalk.next()) {
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    
                    File file = new File(tempOutputFolder, treeWalk.getPathString());
                    file.getParentFile().mkdirs();
                    
                    try(FileOutputStream out = new FileOutputStream(file)) {
                        loader.copyTo(out);
                    }
                }
            }
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.ExtractModelFromCommitAction_1, ex);
        }
        
        // Open the model with no file name
        try {
            GraficoModelImporter importer = new GraficoModelImporter(tempOutputFolder);
            IArchimateModel graficoModel = importer.importAsModel();
            
            if(graficoModel != null) {
                // Open it, this will do the necessary checks and add a command stack and an archive manager
                IEditorModelManager.INSTANCE.openModel(graficoModel);
                
                // Set model name
                graficoModel.setName(graficoModel.getName() + " (" + fCommit.getName().substring(0, 8) + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            }
            else {
                MessageDialog.openError(fWindow.getShell(), Messages.ExtractModelFromCommitAction_1, Messages.ExtractModelFromCommitAction_2);
            }
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.ExtractModelFromCommitAction_1, ex);
        }
        
        // Clean up
        deleteFolder(tempOutputFolder);
    }
    
    private File getTempFolder() {
        File file = new File(System.getProperty("java.io.tmpdir"), "org.archicontribs.modelrepository.tmp"); //$NON-NLS-1$ //$NON-NLS-2$
        file.mkdirs();
        return file;
    }
    
    private void deleteFolder(File folder) {
        try {
            FileUtils.deleteFolder(folder);
        }
        catch(IOException ex) {
            displayErrorDialog(Messages.ExtractModelFromCommitAction_1, ex);
        }
    }

    @Override
    protected boolean shouldBeEnabled() {
        return fCommit != null && getRepository() != null;
    }
}
