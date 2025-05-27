/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.branches;

import java.io.IOException;
import java.text.Collator;
import java.text.DateFormat;
import java.util.Date;

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
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.archimatetool.editor.ui.ColorFactory;
import com.archimatetool.editor.ui.FontFactory;
import com.archimatetool.editor.ui.UIUtils;


/**
 * Branches Table Viewer
 */
public class BranchesTableViewer extends TableViewer {
    
    /**
     * Constructor
     */
    public BranchesTableViewer(Composite parent) {
        super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
        
        // Mac Item height
        UIUtils.fixMacSiliconItemHeight(getTable());
        
        getTable().setHeaderVisible(true);
        getTable().setLinesVisible(false);
        
        TableColumnLayout tableLayout = (TableColumnLayout)parent.getLayout();
        
        TableViewerColumn column = new TableViewerColumn(this, SWT.NONE, 0);
        column.getColumn().setText(Messages.BranchesTableViewer_0);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 1);
        column.getColumn().setText(Messages.BranchesTableViewer_1);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 2);
        column.getColumn().setText(Messages.BranchesTableViewer_6);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 3);
        column.getColumn().setText(Messages.BranchesTableViewer_7);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 4);
        column.getColumn().setText(Messages.BranchesTableViewer_8);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 5);
        column.getColumn().setText(Messages.BranchesTableViewer_9);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));

        setContentProvider(new BranchesContentProvider());
        setLabelProvider(new BranchesLabelProvider());
        
        setComparator(new ViewerComparator(Collator.getInstance()) {
            @Override
            public int compare(Viewer viewer, Object e1, Object e2) {
                BranchInfo b1 = (BranchInfo)e1;
                BranchInfo b2 = (BranchInfo)e2;
                return getComparator().compare(b1.getShortName(), b2.getShortName());
            }
        });
    }

    public void doSetInput(IArchiRepository archiRepo) {
        setInput(archiRepo);
        
        // Do the Layout kludge
        getTable().getParent().layout();

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
            if(parent instanceof IArchiRepository) {
                IArchiRepository repo = (IArchiRepository)parent;
                
                // Local Repo was deleted
                if(!repo.getLocalRepositoryFolder().exists()) {
                    return new Object[0];
                }
                
                try {
                    BranchStatus status = repo.getBranchStatus();
                    return status.getLocalAndUntrackedRemoteBranches().toArray();
                }
                catch(IOException | GitAPIException ex) {
                    ex.printStackTrace();
                }
            }
            
            return new Object[0];
        }
    }

    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class BranchesLabelProvider extends CellLabelProvider {
        
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        
        public String getColumnText(BranchInfo branchInfo, int columnIndex) {
            RevCommit commit = branchInfo.getLatestCommit();
            
            // If we are going to do this then use an ArchiRepositoryStatus class
            //IArchiRepository repo = (IArchiRepository)getInput();
            //boolean hasLocalChanges = repo.hasLocalChanges();
            
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
                 
                case 2:
                    return commit == null ? "" : commit.getCommitterIdent().getName(); //$NON-NLS-1$
                    
                case 3:
                    return commit == null ? "" : dateFormat.format(new Date(commit.getCommitTime() * 1000L)); //$NON-NLS-1$
                    
                case 4:
                    String text;
                    
                    if(branchInfo.hasUnpushedCommits()) {
                        text = Messages.BranchesTableViewer_10;
                    }
                    else if(branchInfo.hasRemoteCommits() || branchInfo.isRemote()) {
                        text = Messages.BranchesTableViewer_11;
                    }
                    else if(branchInfo.hasUnpushedCommits() && branchInfo.hasRemoteCommits()) {
                        text = Messages.BranchesTableViewer_12;
                    }
                    else {
                        text = Messages.BranchesTableViewer_14;
                    }
                    
                    return text;
                    
                case 5:
                    return branchInfo.isMerged() ? Messages.BranchesTableViewer_15 : Messages.BranchesTableViewer_16;
                    
                default:
                    return ""; //$NON-NLS-1$
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
                
                // Red text for "deleted" branches
                if(branchInfo.isRemoteDeleted()) {
                    cell.setForeground(ColorFactory.get(255, 64, 0));
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
