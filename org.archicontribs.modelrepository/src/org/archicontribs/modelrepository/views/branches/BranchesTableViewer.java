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
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

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
        
        getTable().setHeaderVisible(true);
        getTable().setLinesVisible(false);
        
        TableColumnLayout tableLayout = (TableColumnLayout)parent.getLayout();
        
        TableViewerColumn column = new TableViewerColumn(this, SWT.NONE, 0);
        column.getColumn().setText(Messages.BranchesTableViewer_0);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(50, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 1);
        column.getColumn().setText(Messages.BranchesTableViewer_1);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(40, false));
        
        setContentProvider(new BranchesContentProvider());
        setLabelProvider(new BranchesLabelProvider());
        
        setComparator(new ViewerComparator() {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                BranchInfo b1 = (BranchInfo)e1;
                BranchInfo b2 = (BranchInfo)e2;
                return b1.getShortName().compareToIgnoreCase(b2.getShortName());
            }
        });
    }

    public void doSetInput(IArchiRepository archiRepo) {
        setInput(archiRepo);
        
        // Do the Layout kludge
        ((UpdatingTableColumnLayout)getTable().getParent().getLayout()).doRelayout();

        // Select first row
        //Object element = getElementAt(0);
        //if(element != null) {
        //    setSelection(new StructuredSelection(element));
        //}
    }
    
    // ===============================================================================================
	// ===================================== Table Model ==============================================
	// ===============================================================================================
    
    /**
     * The Model for the Table.
     */
    class BranchesContentProvider implements IStructuredContentProvider {
        @Override
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        }

        @Override
        public void dispose() {
        }
        
        @Override
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
                        name += " " + Messages.BranchesTableViewer_2; //$NON-NLS-1$
                    }
                    return name;

                case 1:
                    if(branchInfo.isRemoteDeleted()) {
                        return Messages.BranchesTableViewer_3;
                    }
                    if(branchInfo.hasRemoteRef()) {
                        return Messages.BranchesTableViewer_4;
                    }
                    else {
                        return Messages.BranchesTableViewer_5;
                    }
                    
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
                else {
                    cell.setFont(null);
                }
                
                switch(cell.getColumnIndex()) {
                    case 0:
                        cell.setImage(IModelRepositoryImages.ImageFactory.getImage(IModelRepositoryImages.ICON_BRANCH));
                        break;

                    default:
                        break;
                }
            }
        }
        
    }
}
