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
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
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
    
    // Cache status for expensive calls
    private class StatusCache {
        BranchInfo branchInfo;
        boolean hasLocalChanges;
        
        public StatusCache(BranchInfo branchInfo, boolean hasLocalChanges) {
            this.branchInfo = branchInfo;
            this.hasLocalChanges = hasLocalChanges;
        }
    }
    
    private Map<IArchiRepository, StatusCache> cache;

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
        
        setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                IArchiRepository r1 = (IArchiRepository)e1;
                IArchiRepository r2 = (IArchiRepository)e2;
                return r1.getName().compareToIgnoreCase(r2.getName());
            }
        });
        
        setInput(""); //$NON-NLS-1$
        
        // Refresh File System Job
        new RefreshFilesJob(this);
        
        // Fetch Job
        FetchJob.getInstance().init(this);
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
                refresh();
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
    
    /**
     * Update the status cache
     */
    private void updateStatusCache(List<IArchiRepository> repos) {
        cache = new Hashtable<IArchiRepository, StatusCache>();
        
        for(IArchiRepository repo : repos) {
            try {
                BranchInfo branchInfo = repo.getBranchStatus().getCurrentLocalBranch();
                if(branchInfo != null) { // This can be null!!
                    StatusCache sc = new StatusCache(branchInfo, repo.hasLocalChanges());
                    cache.put(repo, sc);
                }
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
                ModelRepositoryPlugin.INSTANCE.log(IStatus.ERROR, "Error getting Model Repository Status", ex); //$NON-NLS-1$
            }
        }
    }
    
    // ===============================================================================================
	// ===================================== Tree Model ==============================================
	// ===============================================================================================
    
    /**
     * The model for the Tree.
     */
    class ModelRepoTreeContentProvider implements ITreeContentProvider {
        
        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }
        
        @Override
        public void dispose() {
        }
        
        @Override
        public Object[] getElements(Object parent) {
            return getChildren(getRootFolder());
        }
        
        @Override
        public Object getParent(Object child) {
            if(child instanceof File) {
                return ((File)child).getParentFile();
            }
            if(child instanceof IArchiRepository) {
                return ((IArchiRepository)child).getLocalRepositoryFolder().getParentFile();
            }
            return null;
        }
        
        @Override
        public Object[] getChildren(Object parent) {
            if(parent instanceof File) {
                List<IArchiRepository> repos = getRepositories((File)parent);
                updateStatusCache(repos); // update status cache
                return repos.toArray();
            }
            
            return new Object[0];
        }
        
        @Override
        public boolean hasChildren(Object parent) {
            return false;
        }
    }
    
    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class ModelRepoTreeLabelProvider extends CellLabelProvider {
        Image getImage(IArchiRepository repo) {
            Image image = IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_MODEL);
            
            StatusCache sc = cache.get(repo);
            if(sc != null) {
                if(sc.hasLocalChanges) {
                    image = IModelRepositoryImages.ImageFactory.getOverlayImage(image,
                            IModelRepositoryImages.ICON_LEFT_BALL_OVERLAY, IDecoration.BOTTOM_LEFT);
                }
                
                if(sc.branchInfo.hasUnpushedCommits()) {
                    image = IModelRepositoryImages.ImageFactory.getOverlayImage(image,
                            IModelRepositoryImages.ICON_RIGHT_BALL_OVERLAY, IDecoration.BOTTOM_RIGHT);
                }
                
                if(sc.branchInfo.hasRemoteCommits()) {
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
                if(sc.branchInfo.hasUnpushedCommits()) {
                    if(StringUtils.isSet(s)) {
                        s += " | "; //$NON-NLS-1$
                    }
                    s += Messages.ModelRepositoryTreeViewer_0;
                }
                if(sc.branchInfo.hasRemoteCommits()) {
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
                
                // Clear this first
                cell.setForeground(null);
                
                StatusCache sc = cache.get(repo);
                if(sc != null) {
                    // Repository name and current branch
                    cell.setText(repo.getName() + " [" + sc.branchInfo.getShortName() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
                    
                    // Red text
                    if(sc.branchInfo.hasUnpushedCommits() || sc.branchInfo.hasRemoteCommits() || sc.hasLocalChanges) {
                        cell.setForeground(ColorFactory.get(255, 64, 0));
                    }
                }
                else {
                    cell.setText(repo.getName());
                }

                // Image
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
