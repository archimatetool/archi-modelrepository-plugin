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
    
    String ICON_ABORT_16 = IMGPATH + "abort.png"; //$NON-NLS-1$
    String ICON_CLONE_16 = IMGPATH + "add_obj.png"; //$NON-NLS-1$
    String ICON_COMMIT_16 = IMGPATH + "commit.png"; //$NON-NLS-1$
    String ICON_DELETE_16 = IMGPATH + "delete.png"; //$NON-NLS-1$
    String ICON_HISTORY_VIEW_16 = IMGPATH + "history_view.png"; //$NON-NLS-1$
    String ICON_MODEL = IMGPATH + "elements_obj.png"; //$NON-NLS-1$
    String ICON_OPEN_16 = IMGPATH + "open.png"; //$NON-NLS-1$
    String ICON_PUSH_16 = IMGPATH + "push.png"; //$NON-NLS-1$
    String ICON_REFRESH_16 = IMGPATH + "pull.png"; //$NON-NLS-1$
    
    String BANNER_COMMIT = IMGPATH + "commit_wizban.png"; //$NON-NLS-1$
}
