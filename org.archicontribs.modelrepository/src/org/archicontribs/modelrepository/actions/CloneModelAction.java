/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.File;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Clone a model
 */
public class CloneModelAction extends AbstractModelAction {
	
	private IWorkbenchWindow fWindow;

    public CloneModelAction(IWorkbenchWindow window) {
        fWindow = window;
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE_16));
        setText("Clone");
        setToolTipText("Clone Remote Model");
    }

    @Override
    public void run() {
        try {
            CloneCommand cloneCommand = Git.cloneRepository();
            cloneCommand.setDirectory(new File("/testGit"));
            cloneCommand.setURI("");
            cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider("username", "password") );
            cloneCommand.call();
        }
        catch(GitAPIException ex) {
            ex.printStackTrace();
        }
    }
}
