/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.branches;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.BranchStatus;
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
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.archimatetool.editor.ui.FontFactory;
import com.archimatetool.editor.ui.components.UpdatingTableColumnLayout;


/**
 * Branches Table Viewer
 */
public class BranchesTableViewer extends TableViewer {
    
    /**
     * Constructor
     */
    public BranchesTableViewer(Composite parent) {
        super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        
        setup(parent);
        
        setContentProvider(new BranchesContentProvider());
        setLabelProvider(new BranchesLabelProvider());
    }

    /**
     * Set things up.
     */
    protected void setup(Composite parent) {
        getTable().setHeaderVisible(true);
        getTable().setLinesVisible(false);
        
        TableColumnLayout tableLayout = (TableColumnLayout)parent.getLayout();
        
        TableViewerColumn column = new TableViewerColumn(this, SWT.NONE, 0);
        column.getColumn().setText("Branch");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(50, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 1);
        column.getColumn().setText("Status");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(40, false));

        column = new TableViewerColumn(this, SWT.NONE, 2);
        column.getColumn().setText("Local");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
    
        column = new TableViewerColumn(this, SWT.NONE, 3);
        column.getColumn().setText("Remote");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));

        column = new TableViewerColumn(this, SWT.NONE, 4);
        column.getColumn().setText("Tracked");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
    }
    
    public void doSetInput(IArchiRepository archiRepo) {
        setInput(archiRepo);
        
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
            IArchiRepository repo = (IArchiRepository)parent;
            
            // Local Repo was deleted
            if(!repo.getLocalRepositoryFolder().exists()) {
                return new Object[0];
            }
            
            try {
                BranchStatus status = repo.getBranchStatus();
                if(status != null) {
                    return status.getLocalAndUntrackedRemoteBranches().toArray();
                }
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
        
        public String getColumnText(BranchInfo branchInfo, int columnIndex) {
            switch(columnIndex) {
                case 0:
                    String name = branchInfo.getShortName();
                    if(branchInfo.isCurrentBranch()) {
                        name += " " + "(current)";
                    }
                    return name;

                case 1:
                    if(branchInfo.isDeleted()) {
                        return "Deleted";
                    }
                    if(branchInfo.isPublished()) {
                        return "Published";
                    }
                    else {
                        return "Unpublished";
                    }
                    //return "";
                    
                default:
                    return null;
            }
        }

        @Override
        public void update(ViewerCell cell) {
            if(cell.getElement() instanceof BranchInfo) {
                BranchInfo branchInfo = (BranchInfo)cell.getElement();
                
                cell.setText(getColumnText(branchInfo, cell.getColumnIndex()));
                
                if(branchInfo.isCurrentBranch() && cell.getColumnIndex() == 0) {
                    cell.setFont(FontFactory.SystemFontBold);
                }
                
                switch(cell.getColumnIndex()) {
                    case 0:
                        cell.setImage(IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_BRANCH));
                        break;
                        
                    case 2:
                        if(branchInfo.isLocal()) {
                            cell.setImage(IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_TICK));
                        }
                        break;

                    case 3:
                        if(branchInfo.isRemote()) {
                            cell.setImage(IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_TICK));
                        }
                        break;

                    case 4:
                        if(branchInfo.hasTrackedRef()) {
                            cell.setImage(IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_TICK));
                        }
                        break;

                    default:
                        break;
                }
            }
        }
        
    }
}
