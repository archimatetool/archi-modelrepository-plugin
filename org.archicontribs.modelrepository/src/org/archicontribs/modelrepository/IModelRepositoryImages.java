/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.swt.graphics.Image;

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
    String ICON_RESET = IMGPATH + "reset.gif"; //$NON-NLS-1$
    String ICON_REVERT = IMGPATH + "revert.gif"; //$NON-NLS-1$
    String ICON_SYNCED = IMGPATH + "synced.png"; //$NON-NLS-1$
    String ICON_UNDO_COMMIT = IMGPATH + "undo_commit.png"; //$NON-NLS-1$
    String ICON_UNSTAGE = IMGPATH + "unstage.gif"; //$NON-NLS-1$
    String ICON_WARNING_OVERLAY = IMGPATH + "warning_ovr.png"; //$NON-NLS-1$
    
    String BANNER_COMMIT = IMGPATH + "commit_wizban.png"; //$NON-NLS-1$
    
    
    /*
     * TODO - This is temporarily here until the next version of Archi is released which has this method
     */
    static public Image getOverlayImage(Image underlay, String overlayName, int quadrant) {
        // Make a registry name, cached
        String key_name = overlayName + quadrant;

        Image image = ImageFactory.getImage(key_name);
        
        // Make it and cache it
        if(image == null) {
            ImageDescriptor overlay = ImageFactory.getImageDescriptor(overlayName);
            if(overlay != null) {
                image = new DecorationOverlayIcon(underlay, overlay, quadrant).createImage();
                if(image != null) {
                    ImageRegistry registry = ModelRepositoryPlugin.INSTANCE.getImageRegistry();
                    registry.put(key_name, image);
                }
            }
        }
        
        return image;
    }

}
