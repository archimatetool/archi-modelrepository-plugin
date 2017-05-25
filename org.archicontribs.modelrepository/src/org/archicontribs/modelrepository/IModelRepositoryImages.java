/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;

import com.archimatetool.editor.ui.ImageFactory;




/**
 * Image Factory for this application
 * 
 * @author Phillip Beauvoir
 */
public interface IModelRepositoryImages {
    
    ImageFactory ImageFactory = new ImageFactory(ModelRepositoryPlugin.INSTANCE);

    String IMGPATH = "img/"; //$NON-NLS-1$
    
    String ICON_PLUGIN = IMGPATH + "plugin.png"; //$NON-NLS-1$
    
    String ICON_ABORT = IMGPATH + "abort.png"; //$NON-NLS-1$
    String ICON_CLONE = IMGPATH + "add_obj.png"; //$NON-NLS-1$
    String ICON_COMMIT = IMGPATH + "commit.png"; //$NON-NLS-1$
    String ICON_CREATE_REPOSITORY = IMGPATH + "createRepository.png"; //$NON-NLS-1$
    String ICON_DELETE = IMGPATH + "delete.png"; //$NON-NLS-1$
    String ICON_HISTORY_VIEW = IMGPATH + "history_view.png"; //$NON-NLS-1$
    String ICON_MODEL = IMGPATH + "elements_obj.png"; //$NON-NLS-1$
    String ICON_OPEN = IMGPATH + "open.png"; //$NON-NLS-1$
    String ICON_PUSH = IMGPATH + "push.png"; //$NON-NLS-1$
    String ICON_REFRESH = IMGPATH + "pull.png"; //$NON-NLS-1$
    
    String BANNER_COMMIT = IMGPATH + "commit_wizban.png"; //$NON-NLS-1$
}
