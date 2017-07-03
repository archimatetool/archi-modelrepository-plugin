/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.repositories;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;


/**
 * Repository Tree Viewer
 */
public class ModelRepositoryTreeViewer extends TreeViewer implements IRepositoryListener {
    /**
     * The Root Folder we are exploring
     */
    private File fRootFolder;
    
    /**
     * Refresh timer
     */
    private Runnable fTimer;
    
    /**
     * Refresh timer interval of 5 seconds
     */
    static int TIMERDELAY = 5000;
        
    /**
     * Constructor
     */
    public ModelRepositoryTreeViewer(File rootFolder, Composite parent) {
        super(parent, SWT.MULTI);
        
        fRootFolder = rootFolder;
        
        setupRefreshTimer();
        
        setContentProvider(new ModelRepoTreeContentProvider());
        setLabelProvider(new ModelRepoTreeLabelProvider());
        
        fRootFolder.mkdirs();
        setInput(fRootFolder);
        
        RepositoryListenerManager.INSTANCE.addListener(this);
        
        // Dispose of this
        getTree().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                if(fTimer != null) {
                    Display.getDefault().timerExec(-1, fTimer);
                    fTimer = null;
                }
                
                RepositoryListenerManager.INSTANCE.removeListener(ModelRepositoryTreeViewer.this);
            }
        });
    }

    /**
     * Set up the Refresh timer
     */
    protected void setupRefreshTimer() {
        fTimer = new Runnable() {
            public void run() { 
                if(!getTree().isDisposed()) { // this is important!
                    refresh();
                    Display.getDefault().timerExec(TIMERDELAY, this);  // run again
                }
            }
        };
        
        Display.getDefault().timerExec(TIMERDELAY, fTimer);
    }

    @Override
    public void repositoryChanged(String eventName, IArchiRepository repository) {
        if(IRepositoryListener.REPOSITORY_DELETED.equals(eventName) || IRepositoryListener.REPOSITORY_ADDED.equals(eventName)) {
            refresh();
        }
    }
    
    // ===============================================================================================
	// ===================================== Tree Model ==============================================
	// ===============================================================================================
    
    /**
     * The model for the Tree.
     */
    class ModelRepoTreeContentProvider implements ITreeContentProvider {
        
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }
        
        public void dispose() {
        }
        
        public Object[] getElements(Object parent) {
            return getChildren(parent);
        }
        
        public Object getParent(Object child) {
            if(child instanceof File) {
                return ((File)child).getParentFile();
            }
            if(child instanceof IArchiRepository) {
                return ((IArchiRepository)child).getLocalRepositoryFolder().getParentFile();
            }
            return null;
        }
        
        public Object [] getChildren(Object parent) {
        	// Only show top level folders that are git repos
            List<ArchiRepository> repos = new ArrayList<ArchiRepository>();
            
            if(parent instanceof File) {
                if(((File)parent).exists()) {
                    for(File file : ((File)parent).listFiles()) {
                        if(GraficoUtils.isGitRepository(file)) {
                            repos.add(new ArchiRepository(file));
                        }
                    }
                }
            }
            
            return repos.toArray();
        }
        
        public boolean hasChildren(Object parent) {
            return false;
        }
    }
    
    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class ModelRepoTreeLabelProvider extends LabelProvider {
        
        @Override
        public String getText(Object obj) {
        	if(obj instanceof IArchiRepository) {
        	    IArchiRepository repo = (IArchiRepository)obj;
        	    return repo.getName();
        	}
        	else if(obj instanceof File) {
        	    return ((File)obj).getName();
        	}
        	
        	return ""; //$NON-NLS-1$
        }
        
        @Override
        public Image getImage(Object obj) {
            Image image = null;
            
            if(obj instanceof IArchiRepository) {
                image = IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_MODEL);
            }
            else if(obj instanceof File) {
                File file = (File)obj;
                
                if(file.isDirectory()) {
                    image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
                }
                else {
                    image = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FILE);
                }
            }
            
            return image;
        }
    }
}
