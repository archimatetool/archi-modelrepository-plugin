/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.history;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;


/**
 * History Table Viewer
 */
public class HistoryTableViewer extends TableViewer {
    /**
     * Constructor
     */
    public HistoryTableViewer(Composite parent) {
        super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        
        setup(parent);
        
        setContentProvider(new HistoryContentProvider());
        setLabelProvider(new HistoryLabelProvider());
    }

    /**
     * Set things up.
     */
    protected void setup(Composite parent) {
        getTable().setHeaderVisible(true);
        getTable().setLinesVisible(false);
        
        TableColumnLayout tableLayout = (TableColumnLayout)parent.getLayout();
        
        TableViewerColumn column = new TableViewerColumn(this, SWT.NONE, 0);
        column.getColumn().setText("Id");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 1);
        column.getColumn().setText("Message");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(50, false));

        column = new TableViewerColumn(this, SWT.NONE, 2);
        column.getColumn().setText("Author");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(20, false));
    
        column = new TableViewerColumn(this, SWT.NONE, 3);
        column.getColumn().setText("Date");
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(20, false));
    
    }
    
    
    // ===============================================================================================
	// ===================================== Table Model ==============================================
	// ===============================================================================================
    
    /**
     * The Model for the Table.
     */
    class HistoryContentProvider implements IStructuredContentProvider {
        List<RevCommit> commits = new ArrayList<RevCommit>();
        
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
            if(!(newInput instanceof File)) {
                return;
            }
            
            File folder = (File)newInput;
            
            commits = new ArrayList<RevCommit>();
            
            // TODO See https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/porcelain/ShowLog.java
            try(Git git = Git.open(folder)) {
                // get a list of all known heads, tags, remotes, ...
                Collection<Ref> allRefs = git.getRepository().getAllRefs().values();
                
                // a RevWalk allows to walk over commits based on some filtering that is defined
                try(RevWalk revWalk = new RevWalk(git.getRepository())) {
                    for(Ref ref : allRefs ) {
                        revWalk.markStart(revWalk.parseCommit(ref.getObjectId()));
                    }
                    
                    for(RevCommit commit : revWalk ) {
                        commits.add(commit);
                    }
                }
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
        }
        
        public void dispose() {
        }
        
        public Object[] getElements(Object parent) {
            return commits.toArray();
        }
    }
    
    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class HistoryLabelProvider extends LabelProvider implements ITableLabelProvider {
        
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        
        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            RevCommit commit = (RevCommit)element;
            
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
    }
}
