/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.history;

import java.io.IOException;
import java.util.List;

import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;


/**
 * Branches Table Viewer
 */
public class BranchesTableViewer extends TableViewer {
    
    /**
     * Constructor
     */
    public BranchesTableViewer(Composite parent) {
        super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        
        setup(parent);
        
        setContentProvider(new BranchesContentProvider());
        setLabelProvider(new BranchesLabelProvider());
    }

    /**
     * Set things up.
     */
    protected void setup(Composite parent) {
        getTable().setHeaderVisible(false);
        getTable().setLinesVisible(false);
        
        TableColumnLayout tableLayout = (TableColumnLayout)parent.getLayout();
        
        TableViewerColumn column = new TableViewerColumn(this, SWT.NONE, 0);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(100, false));
    }
    
    public void doSetInput(IArchiRepository repo) {
        setInput(repo);
        
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
    // ===============================================================================================
	// ===================================== Table Model ==============================================
	// ===============================================================================================
    
    /**
     * The Model for the Table.
     */
    class BranchesContentProvider implements IStructuredContentProvider {
        
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }
        
        public void dispose() {
        }
        
        public Object[] getElements(Object parent) {
            if(!(parent instanceof IArchiRepository)) {
                return new Object[0];
            }
            
            IArchiRepository repo = (IArchiRepository)parent;
            
            // Local Repo was deleted
            if(!repo.getLocalRepositoryFolder().exists()) {
                return new Object[0];
            }
            
            try(Git git = Git.open(repo.getLocalRepositoryFolder())) {
                //List<Ref> refs = git.branchList().call(); // Local branches
                List<Ref> refs = git.branchList().setListMode(ListMode.ALL).call(); // All
                return refs.toArray();
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
            }

            return new Object[0];
        }
    }
    
    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class BranchesLabelProvider extends CellLabelProvider {
        
        @Override
        public void update(ViewerCell cell) {
            Ref ref = (Ref)cell.getElement();
            
            cell.setText(ref.getName());
        }
        
    }
}
