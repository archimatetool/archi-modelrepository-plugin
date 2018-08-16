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
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;

import com.archimatetool.editor.ui.ColorFactory;
import com.archimatetool.editor.utils.StringUtils;


/**
 * Repository Tree Viewer
 */
public class ModelRepositoryTreeViewer extends TreeViewer implements IRepositoryListener {

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
        Job refreshFileSystemJob = new Job("Refresh File System Job") { //$NON-NLS-1$
            long lastModified = 0L; // last modified
            
            @Override
            public IStatus run(IProgressMonitor monitor) {
                // Check first thing on entry
                if(getControl().isDisposed()) {
                    return Status.OK_STATUS;
                }
                
                // If rootFolder has been modifed (child folder added/deleted/renamed) refresh
                File rootFolder = getRootFolder();
                
                if(lastModified != 0L && rootFolder.lastModified() != lastModified) {
                    refreshInBackground();
                }

                lastModified = rootFolder.lastModified();

                if(!getControl().isDisposed()) {
                    schedule(5000);// Schedule again in 5 seconds
                }
                
                return Status.OK_STATUS;
            }
        };
        
        refreshFileSystemJob.schedule(5000);
        
        // Fetch Job
        Job fetchJob = new Job("Fetch Job") { //$NON-NLS-1$
            @Override
            public IStatus run(IProgressMonitor monitor) {
                // Check first thing on entry
                if(getControl().isDisposed()) {
                    return Status.OK_STATUS;
                }
                
                boolean needsRefresh = false;

                for(IArchiRepository repo : getRepositories(getRootFolder())) {
                    // Check also in for loop
                    if(getControl().isDisposed()) {
                        return Status.OK_STATUS;
                    }

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

                if(!getControl().isDisposed()) {
                    schedule(20000); // Schedule again in 20 seconds if not disposed
                }
                
                return Status.OK_STATUS;
            }
            
            @Override
            protected void canceling() {
                /*
                 * Because the Git Fetch process doesn't respond to cancel requests we can't cancel it when it is running.
                 * So, if the user closes the app this job might be running. So we will wait for this job to finish
                 */
                int timeout = 0;
                final int delay = 100;
                
                try {
                    while(getState() == Job.RUNNING) {
                        Thread.sleep(delay);
                        timeout += delay;
                        if(timeout > 30000) { // don't wait longer than this
                            break;
                        }
                    }
                }
                catch(InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        };
        
        fetchJob.schedule(1000);
        
        // Explicitly cancel running jobs on control dispose
        getTree().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                refreshFileSystemJob.cancel();
                fetchJob.cancel();
            }
        });
    }
    
    protected void refreshInBackground() {
        if(!getControl().isDisposed()) {
            getControl().getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if(!getControl().isDisposed()) {
                        refresh();
                    }
                }
            });
        }
    }

    @Override
    public void repositoryChanged(String eventName, IArchiRepository repository) {
        switch(eventName) {
            case IRepositoryListener.REPOSITORY_ADDED:
                refresh();
                setSelection(new StructuredSelection(repository));
                break;
                
            case IRepositoryListener.REPOSITORY_DELETED:
                refresh();
                break;

            default:
                refresh(repository);
                break;
        }
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
            boolean hasLocalChanges;
            
            public StatusCache(boolean hasUnpushedCommits, boolean hasRemoteCommits, boolean hasLocalChanges) {
                this.hasUnpushedCommits = hasUnpushedCommits;
                this.hasRemoteCommits = hasRemoteCommits;
                this.hasLocalChanges = hasLocalChanges;
            }
        }
        
        Map<IArchiRepository, StatusCache> cache = new Hashtable<IArchiRepository, StatusCache>();
        
        Image getImage(IArchiRepository repo) {
            Image image = IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_MODEL);
            
            StatusCache sc = cache.get(repo);
            if(sc != null) {
                if(sc.hasLocalChanges) {
                    image = IModelRepositoryImages.ImageFactory.getOverlayImage(image,
                            IModelRepositoryImages.ICON_LEFT_BALL_OVERLAY, IDecoration.BOTTOM_LEFT);
                }
                
                if(sc.hasUnpushedCommits) {
                    image = IModelRepositoryImages.ImageFactory.getOverlayImage(image,
                            IModelRepositoryImages.ICON_RIGHT_BALL_OVERLAY, IDecoration.BOTTOM_RIGHT);
                }
                
                if(sc.hasRemoteCommits) {
                    image = IModelRepositoryImages.ImageFactory.getOverlayImage(image,
                            IModelRepositoryImages.ICON_TOP_BALL_OVERLAY, IDecoration.TOP_RIGHT);
                }
            }
            
            return image;
        }
        
        String getStatusText(IArchiRepository repo) {
            String s = ""; //$NON-NLS-1$
            
            StatusCache sc = cache.get(repo);
            if(sc != null) {
                if(sc.hasLocalChanges) {
                    s += Messages.ModelRepositoryTreeViewer_2;
                }
                if(sc.hasUnpushedCommits) {
                    if(StringUtils.isSet(s)) {
                        s += " | "; //$NON-NLS-1$
                    }
                    s += Messages.ModelRepositoryTreeViewer_0;
                }
                if(sc.hasRemoteCommits) {
                    if(StringUtils.isSet(s)) {
                        s += " | "; //$NON-NLS-1$
                    }
                    s += Messages.ModelRepositoryTreeViewer_1;
                }
                if(!StringUtils.isSet(s)) {
                    s = Messages.ModelRepositoryTreeViewer_3;
                }
            }
            
            return s;
        }
        
        @Override
        public void update(ViewerCell cell) {
            if(cell.getElement() instanceof IArchiRepository) {
                IArchiRepository repo = (IArchiRepository)cell.getElement();
                
                // Local repo was perhaps deleted
                if(!repo.getLocalRepositoryFolder().exists()) {
                    return;
                }
                
                // Image
                try {
                    boolean hasUnpushedCommits = repo.hasUnpushedCommits(IGraficoConstants.REFS_HEADS_MASTER);
                    boolean hasRemoteCommits = repo.hasRemoteCommits(IGraficoConstants.REFS_HEADS_MASTER);
                    boolean hasLocalChanges = repo.hasLocalChanges();
                    
                    StatusCache sc = new StatusCache(hasUnpushedCommits, hasRemoteCommits, hasLocalChanges);
                    cache.put(repo, sc);

                    if(hasUnpushedCommits || hasRemoteCommits || hasLocalChanges) {
                        cell.setForeground(ColorFactory.get(255, 64, 0));
                    }
                    else {
                        cell.setForeground(null);
                    }
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
                
                cell.setText(repo.getName());
                cell.setImage(getImage(repo));
            }
        }
        
        @Override
        public String getToolTipText(Object element) {
            if(element instanceof IArchiRepository) {
                IArchiRepository repo = (IArchiRepository)element;
                
                String s = repo.getName();
                
                String status = getStatusText(repo);
                if(StringUtils.isSet(status)) {
                    s += "\n" + status.replaceAll(" \\| ", "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                }
                
                return s;
            }
            
            return null;
        }
    }
}
