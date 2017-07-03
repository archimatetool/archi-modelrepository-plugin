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
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;


/**
 * Repository Tree Viewer
 */
public class ModelRepositoryTreeViewer extends TreeViewer implements IRepositoryListener {
    /**
     * Background jobs
     */
    private List<Job> fRunningJobs = new ArrayList<Job>();
    
    /**
     * Constructor
     */
    public ModelRepositoryTreeViewer(Composite parent) {
        super(parent, SWT.MULTI);
        
        setContentProvider(new ModelRepoTreeContentProvider());
        setLabelProvider(new ModelRepoTreeLabelProvider());
        
        RepositoryListenerManager.INSTANCE.addListener(this);
        
        // Dispose of this and clean up
        getTree().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                RepositoryListenerManager.INSTANCE.removeListener(ModelRepositoryTreeViewer.this);
                stopBackgroundJobs();
            }
        });
        
        setInput(""); //$NON-NLS-1$
        
        startBackgroundJobs();
    }

    /**
     * Set up and start background jobs
     */
    protected void startBackgroundJobs() {
        // Refresh file system job
        Job job = new UIJob(getControl().getDisplay(), "Refresh File System") { //$NON-NLS-1$
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if(!getTree().isDisposed()) { // this is important!
                    refresh();
                    schedule(10000);// Schedule again in 10 seconds
                }
                return Status.OK_STATUS;
            }
        };
        fRunningJobs.add(job);
        
        // Start all jobs 5 seconds from now
        for(Job j : fRunningJobs) {
            j.schedule(5000);
        }
    }
    
    protected void stopBackgroundJobs() {
        for(Job job : fRunningJobs) {
            job.cancel();
        }
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
            File reposFolder = ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder();
            return getChildren(reposFolder);
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
            
            if(parent instanceof File && ((File)parent).exists()) {
                for(File file : ((File)parent).listFiles()) {
                    if(GraficoUtils.isGitRepository(file)) {
                        repos.add(new ArchiRepository(file));
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
