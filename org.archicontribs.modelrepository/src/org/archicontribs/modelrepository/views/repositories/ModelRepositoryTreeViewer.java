/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.repositories;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.authentication.SimpleCredentialsStorage;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.archimatetool.editor.ui.ColorFactory;


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
        
        ColumnViewerToolTipSupport.enableFor(this);
        
        setInput(""); //$NON-NLS-1$
        
        startBackgroundJobs();
    }

    /**
     * Set up and start background jobs
     */
    protected void startBackgroundJobs() {
        // Refresh file system job
        Job refreshFileSystemJob = new Job("Refresh File System") { //$NON-NLS-1$
            long lastModified = 0L; // last modified
            
            @Override
            public IStatus run(IProgressMonitor monitor) {
                // If rootFolder has been modifed (child folder added/deleted/renamed) refresh
                File rootFolder = getRootFolder();
                
                if(lastModified != 0L && rootFolder.lastModified() != lastModified) {
                    refreshInBackground();
                }

                lastModified = rootFolder.lastModified();

                schedule(5000);// Schedule again in 5 seconds
                
                return Status.OK_STATUS;
            }
        };
        fRunningJobs.add(refreshFileSystemJob);
        
        // Fetch
        Job fetchJob = new Job("Fetch") { //$NON-NLS-1$
            @Override
            public IStatus run(IProgressMonitor monitor) {
                if(!getControl().isDisposed()) {
                    boolean needsRefresh = false;
                    
                    for(IArchiRepository repo : getRepositories(getRootFolder())) {
                        // If the user name and password are stored
                        SimpleCredentialsStorage scs = new SimpleCredentialsStorage(new File(repo.getLocalGitFolder(), IGraficoConstants.REPO_CREDENTIALS_FILE));
                        try {
                            String userName = scs.getUsername();
                            String userPassword = scs.getPassword();
                            if(userName != null && userPassword != null) {
                                repo.fetchFromRemote(userName, userPassword, null, false);
                                needsRefresh = true;
                            }
                        }
                        catch(IOException | GitAPIException ex) {
                            // silence is golden
                        }
                    }
                    
                    if(needsRefresh) {
                        refreshInBackground();
                    }

                    schedule(20000); // Schedule again in 20 seconds
                }
                
                return Status.OK_STATUS;
            }
        };
        fRunningJobs.add(fetchJob);

        // Start all jobs 1 second from now
        for(Job j : fRunningJobs) {
            j.schedule(1000);
        }
    }
    
    protected void stopBackgroundJobs() {
        for(Job job : fRunningJobs) {
            job.cancel();
        }
    }
    
    protected void refreshInBackground() {
        if(!getControl().isDisposed()) {
            getControl().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    refresh();
                }
            });
        }
    }

    @Override
    public void repositoryChanged(String eventName, IArchiRepository repository) {
        refresh();
    }
    
    /**
     * @return Root folder of model repos
     */
    protected File getRootFolder() {
        return ModelRepositoryPlugin.INSTANCE.getUserModelRepositoryFolder();
    }
    
    /**
     * @return All repos in the file system
     */
    protected List<IArchiRepository> getRepositories(File folder) {
        // Only show top level folders that are git repos
        List<IArchiRepository> repos = new ArrayList<IArchiRepository>();
        
        if(folder.exists() && folder.isDirectory()) {
            for(File file : getRootFolder().listFiles()) {
                if(GraficoUtils.isGitRepository(file)) {
                    repos.add(new ArchiRepository(file));
                }
            }
        }
        
        return repos;
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
            return getChildren(getRootFolder());
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
        
        public Object[] getChildren(Object parent) {
            if(parent instanceof File) {
                return getRepositories((File)parent).toArray();
            }
            
            return new Object[0];
        }
        
        public boolean hasChildren(Object parent) {
            return false;
        }
    }
    
    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class ModelRepoTreeLabelProvider extends CellLabelProvider {
        // Cache status for expensive calls
        private class StatusCache {
            boolean hasUnpushedCommits;
            boolean hasRemoteCommits;
            
            public StatusCache(boolean hasUnpushedCommits, boolean hasRemoteCommits) {
                this.hasUnpushedCommits = hasUnpushedCommits;
                this.hasRemoteCommits = hasRemoteCommits;
            }
        }
        
        Map<IArchiRepository, StatusCache> cache = new Hashtable<IArchiRepository, StatusCache>();
        
        @Override
        public void update(ViewerCell cell) {
            if(cell.getElement() instanceof IArchiRepository) {
                IArchiRepository repo = (IArchiRepository)cell.getElement();
                
                // Text
                cell.setText(repo.getName());
                
                // Image
                Image image = IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_MODEL);
                
                try {
                    boolean hasUnpushedCommits = repo.hasUnpushedCommits("refs/heads/master"); //$NON-NLS-1$
                    boolean hasRemoteCommits = repo.hasRemoteCommits("refs/heads/master"); //$NON-NLS-1$
                    
                    StatusCache sc = new StatusCache(hasUnpushedCommits, hasRemoteCommits);
                    cache.put(repo, sc);
                    
                    if(hasUnpushedCommits) {
                        image = IModelRepositoryImages.getOverlayImage(image,
                                IModelRepositoryImages.ICON_WARNING_OVERLAY, IDecoration.BOTTOM_LEFT);
                    }
                    
                    if(hasRemoteCommits) {
                        image = IModelRepositoryImages.getOverlayImage(image,
                                IModelRepositoryImages.ICON_HAS_REMOTE_COMMITS_OVERLAY, IDecoration.BOTTOM_RIGHT);
                    }
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
                
                cell.setImage(image);
            }
        }
        
        @Override
        public String getToolTipText(Object element) {
            if(element instanceof IArchiRepository) {
                IArchiRepository repo = (IArchiRepository)element;
                
                String s = "'" + repo.getName() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
                
                StatusCache sc = cache.get(repo);
                if(sc != null) {
                    if(sc.hasUnpushedCommits) {
                        s += "\n" + //$NON-NLS-1$
                             Messages.ModelRepositoryTreeViewer_0;
                    }
                    if(sc.hasRemoteCommits) {
                        s += "\n" + //$NON-NLS-1$
                             Messages.ModelRepositoryTreeViewer_1;
                    }
                }
                
                return s;
            }
            
            return null;
        }
        
        @Override
        public Color getToolTipForegroundColor(Object object) {
            if(object instanceof IArchiRepository) {
                IArchiRepository repo = (IArchiRepository)object;
                
                StatusCache sc = cache.get(repo);
                if(sc != null) {
                    if(sc.hasUnpushedCommits || sc.hasRemoteCommits) {
                        return ColorFactory.get(255, 0, 0);
                    }
                }
            }
            
            return null;
        }
    }
}
