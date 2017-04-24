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
    String ICON_MODEL = IMGPATH + "elements_obj.png"; //$NON-NLS-1$
    
    String ICON_CLONE_16 = IMGPATH + "add_obj.png"; //$NON-NLS-1$
    String ICON_REFRESH_16 = IMGPATH + "refresh.gif"; //$NON-NLS-1$
    String ICON_OPEN_16 = IMGPATH + "export_wiz.png"; //$NON-NLS-1$
    String ICON_DELETE_16 = IMGPATH + "delete.png"; //$NON-NLS-1$
    String ICON_SAVE_16 = IMGPATH + "import_wiz.png"; //$NON-NLS-1$
    String ICON_COMMIT_16 = IMGPATH + "forward_nav.png"; //$NON-NLS-1$
    String ICON_PUSH_16 = IMGPATH + "synced.png"; //$NON-NLS-1$
    
}
