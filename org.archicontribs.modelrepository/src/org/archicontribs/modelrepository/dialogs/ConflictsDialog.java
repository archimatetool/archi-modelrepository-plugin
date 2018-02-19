/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

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
    
    private Text fFileViewerOurs, fFileViewerTheirs, fFileViewerDiff;
    
    public ConflictsDialog(Shell parentShell, MergeConflictHandler handler) {
        super(parentShell, DIALOG_ID);
        setTitle(Messages.ConflictsDialog_0);
        
        fHandler = handler;
    }
    
    @Override
    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(Messages.ConflictsDialog_0);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        setMessage(Messages.ConflictsDialog_1, IMessageProvider.ERROR);
        setTitleImage(IArchiImages.ImageFactory.getImage(IArchiImages.ECLIPSE_IMAGE_IMPORT_PREF_WIZARD));

        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout layout = new GridLayout(1, false);
        container.setLayout(layout);
        
        SashForm sash = new SashForm(container, SWT.VERTICAL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        sash.setLayoutData(gd);
        
        createTableControl(sash);
        
        SashForm sash2 = new SashForm(sash, SWT.HORIZONTAL);
        sash2.setLayoutData(gd);
        
        fFileViewerOurs = createFileViewerControl(sash2, Messages.ConflictsDialog_2);
        fFileViewerDiff = createFileViewerControl(sash2, Messages.ConflictsDialog_3);
        fFileViewerTheirs = createFileViewerControl(sash2, Messages.ConflictsDialog_4);
        
        sash.setWeights(new int[] { 30, 70 });
        
        // Select first object in table
        Object first = fTableViewer.getElementAt(0);
        if(first != null) {
            fTableViewer.setSelection(new StructuredSelection(first));
        }
        
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
        column1.getColumn().setText(Messages.ConflictsDialog_5);
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

        fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                String path = (String)((StructuredSelection)event.getSelection()).getFirstElement();
                
                try {
                    fFileViewerOurs.setText(fHandler.getArchiRepository().getFileContents(path, IGraficoConstants.HEAD));
                    fFileViewerTheirs.setText(fHandler.getArchiRepository().getFileContents(path, IGraficoConstants.ORIGIN_MASTER));
                    fFileViewerDiff.setText(fHandler.getArchiRepository().getWorkingTreeFileContents(path));
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        // Label Provider
        fTableViewer.setLabelProvider(new LabelProvider());
        
        // Start the table
        fTableViewer.setInput(""); // anything will do //$NON-NLS-1$
    }
    
    private Text createFileViewerControl(Composite parent, String labelText) {
        SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        sash.setLayoutData(gd);
        
        Composite client = new Composite(sash, SWT.NONE);
        client.setLayoutData(new GridData(GridData.FILL_BOTH));
        client.setLayout(new GridLayout());
        
        Label label = new Label(client, SWT.NONE);
        label.setText(labelText);
        
        Text text = new Text(client, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        text.setBackground(fTableViewer.getControl().getBackground());
        
        return text;
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
        return new Point(700, 550);
    }
    
    @Override
    protected boolean isResizable() {
        return true;
    }

}