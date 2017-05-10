/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;

import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.components.ExtendedTitleAreaDialog;

/**
 * Conflicts Dialog
 * 
 * @author Phil Beauvoir
 */
public class ConflictsDialog extends ExtendedTitleAreaDialog {
    
    private static String DIALOG_ID = "ConflictsDialog"; //$NON-NLS-1$

    private MergeConflictHandler fHandler;
    
    private CheckboxTableViewer fTableViewer;
    
    public ConflictsDialog(Shell parentShell, MergeConflictHandler handler) {
        super(parentShell, DIALOG_ID);
        setTitle("Conflicts");
        
        fHandler = handler;
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText("Conflicts");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage("There are Conflicts", IMessageProvider.ERROR);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_IMPORT_PREF_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);
        
        createTableControl(container);
        
        return area;
    }
    
    private void createTableControl(Composite parent) {
        Composite tableComp = new Composite(parent, SWT.BORDER);
        TableColumnLayout tableLayout = new TableColumnLayout();
        tableComp.setLayout(tableLayout);
        tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));

        Table table = new Table(tableComp, SWT.MULTI | SWT.FULL_SELECTION | SWT.CHECK);
        table.setHeaderVisible(true);
        fTableViewer = new CheckboxTableViewer(table);
        fTableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        fTableViewer.getTable().setLinesVisible(true);
        fTableViewer.setComparator(new ViewerComparator());

        // Columns
        TableViewerColumn column1 = new TableViewerColumn(fTableViewer, SWT.NONE, 0);
        column1.getColumn().setText("Select to use theirs");
        tableLayout.setColumnData(column1.getColumn(), new ColumnWeightData(100, true));

        // Content Provider
        fTableViewer.setContentProvider(new IStructuredContentProvider() {
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            public void dispose() {
            }

            public Object[] getElements(Object inputElement) {
                MergeResult result = fHandler.getMergeResult();
                return result.getConflicts().keySet().toArray();
            }
        });

        // Label Provider
        fTableViewer.setLabelProvider(new LabelProvider());
        fTableViewer.setInput(""); // anything will do //$NON-NLS-1$
    }
    
    @Override
    protected void okPressed() {
        List<String> ours = new ArrayList<String>();
        List<String> theirs = new ArrayList<String>();
        
        for(Object checked : fTableViewer.getCheckedElements()) {
            theirs.add((String)checked);
        }
        
        for(String s : fHandler.getMergeResult().getConflicts().keySet()) {
            if(!theirs.contains(s)) {
                ours.add(s);
            }
        }
        
        fHandler.setOursAndTheirs(ours, theirs);
        
        super.okPressed();
    }
    
    @Override
    protected Point getDefaultDialogSize() {
        return new Point(500, 350);
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }

}