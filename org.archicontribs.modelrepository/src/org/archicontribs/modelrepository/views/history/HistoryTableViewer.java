/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.history;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.ui.components.UpdatingTableColumnLayout;


/**
 * History Table Viewer
 */
public class HistoryTableViewer extends TableViewer {
    
    private RevCommit localCommit, originCommit;
    
    private Ref currentRef;
    
    /**
     * Constructor
     */
    public HistoryTableViewer(Composite parent) {
        super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        
        setup(parent);
        
        setContentProvider(new HistoryContentProvider());
        setLabelProvider(new HistoryLabelProvider());
        
        ColumnViewerToolTipSupport.enableFor(this);
    }

    /**
     * Set things up.
     */
    protected void setup(Composite parent) {
        getTable().setHeaderVisible(true);
        getTable().setLinesVisible(false);
        
        TableColumnLayout tableLayout = (TableColumnLayout)parent.getLayout();
        
        TableViewerColumn column = new TableViewerColumn(this, SWT.NONE, 0);
        column.getColumn().setText(Messages.HistoryTableViewer_0);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 1);
        column.getColumn().setText(Messages.HistoryTableViewer_1);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(50, false));

        column = new TableViewerColumn(this, SWT.NONE, 2);
        column.getColumn().setText(Messages.HistoryTableViewer_2);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(20, false));
    
        column = new TableViewerColumn(this, SWT.NONE, 3);
        column.getColumn().setText(Messages.HistoryTableViewer_3);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(20, false));
    }
    
    public void doSetInput(IArchiRepository repo) {
        currentRef = null;
        
        setInput(repo);
        
        // Do the Layout kludge
        ((UpdatingTableColumnLayout)getTable().getParent().getLayout()).doRelayout();

        // Select first row
        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                if(!getTable().isDisposed()) {
                    Object element = getElementAt(0);
                    if(element != null) {
                        setSelection(new StructuredSelection(element));
                    }
                }
            }
        });
    }
    
    public void setRef(Ref ref) {
        currentRef = ref;
        refresh();
        
        // Layout kludge
        ((UpdatingTableColumnLayout)getTable().getParent().getLayout()).doRelayout();
    }

    // ===============================================================================================
	// ===================================== Table Model ==============================================
	// ===============================================================================================
    
    /**
     * The Model for the Table.
     */
    class HistoryContentProvider implements IStructuredContentProvider {
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        public void dispose() {
        }
        
        public Object[] getElements(Object parent) {
            List<RevCommit> commits = new ArrayList<RevCommit>();
            localCommit = null;
            originCommit = null;
            
            if(!(parent instanceof IArchiRepository)) {
                return commits.toArray();
            }
            
            IArchiRepository repo = (IArchiRepository)parent;
            
            // Local Repo was deleted
            if(!repo.getLocalRepositoryFolder().exists()) {
                return commits.toArray();
            }
            

            try(Repository repository = Git.open(repo.getLocalRepositoryFolder()).getRepository()) {
                // a RevWalk allows to walk over commits based on some filtering that is defined
                try(RevWalk revWalk = new RevWalk(repository)) {
                    // Find the local branch
                    ObjectId objectID = repository.resolve("refs/heads/master");
                    if(currentRef != null) {
                        objectID = currentRef.getObjectId();
                    }
                    if(objectID != null) {
                        localCommit = revWalk.parseCommit(objectID);
                        revWalk.markStart(localCommit); 
                    }
                    
                    // Find the remote branch
//                    objectID = repository.resolve("refs/remotes/origin/master");
//                    if(objectID != null) {
//                        originCommit = revWalk.parseCommit(objectID);
//                        revWalk.markStart(originCommit);
//                    }
                    
                    // Collect the commits
                    for(RevCommit commit : revWalk ) {
                        commits.add(commit);
                    }
                    
                    revWalk.dispose();
                }
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
            
            return commits.toArray();
        }
    }
    
    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class HistoryLabelProvider extends CellLabelProvider {
        
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        
        public String getColumnText(RevCommit commit, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    return commit.getName().substring(0, 8);
                    
                case 1:
                    return commit.getShortMessage();
                    
                case 2:
                    return commit.getAuthorIdent().getName();
                
                case 3:
                    return dateFormat.format(new Date(commit.getCommitTime() * 1000L));
                    
                default:
                    return null;
            }
        }

        @Override
        public void update(ViewerCell cell) {
            if(cell.getElement() instanceof RevCommit) {
                RevCommit commit = (RevCommit)cell.getElement();
                
                cell.setText(getColumnText(commit, cell.getColumnIndex()));
                
                if(cell.getColumnIndex() == 1) {
                    Image image = null;
                    
                    if(commit.equals(localCommit) && commit.equals(originCommit)) {
                        image = IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_HISTORY_VIEW);
                    }
                    else if(commit.equals(originCommit)) {
                        image = IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_REMOTE);
                    }
                    else if(commit.equals(localCommit)) {
                        image = IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_LOCAL);
                    }
                    
                    cell.setImage(image);
                }
            }
        }
        
        @Override
        public String getToolTipText(Object element) {
            if(element instanceof RevCommit) {
                RevCommit commit = (RevCommit)element;
                
                String s = ""; //$NON-NLS-1$
                
                if(commit.equals(localCommit) && commit.equals(originCommit)) {
                    s += Messages.HistoryTableViewer_4 + " "; //$NON-NLS-1$
                }
                else if(commit.equals(localCommit)) {
                    s += Messages.HistoryTableViewer_5 + " "; //$NON-NLS-1$
                }

                else if(commit.equals(originCommit)) {
                    s += Messages.HistoryTableViewer_6 + " "; //$NON-NLS-1$
                }
                
                s += commit.getFullMessage().trim();
                
                return s;
            }
            
            return null;
        }
    }
}
