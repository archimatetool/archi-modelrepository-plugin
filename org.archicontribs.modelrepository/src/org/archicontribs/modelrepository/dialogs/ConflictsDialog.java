/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.IOException;

import org.archicontribs.modelrepository.grafico.MergeConflictHandler;
import org.archicontribs.modelrepository.grafico.MergeObjectInfo;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.ui.ArchiLabelProvider;
import com.archimatetool.editor.ui.ColorFactory;
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
    
    private TableViewer fTableViewer;
    
    private ObjectViewer fViewerOurs, fViewerTheirs;
    
    private String[] choices = {
            "Mine",
            "Theirs"
    };
    
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
        setMessage("There are conflicts between your version and the online version. Please resolve the conflicts.", IMessageProvider.WARNING);
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
        
        fViewerOurs = new ObjectViewer(sash2, MergeObjectInfo.OURS);
        fViewerTheirs = new ObjectViewer(sash2, MergeObjectInfo.THEIRS);
        
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

        fTableViewer = new TableViewer(tableComp, SWT.MULTI | SWT.FULL_SELECTION);
        fTableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
        fTableViewer.getTable().setHeaderVisible(true);
        fTableViewer.getTable().setLinesVisible(true);
        fTableViewer.setComparator(new ViewerComparator());

        // Columns
        TableViewerColumn column1 = new TableViewerColumn(fTableViewer, SWT.NONE, 0);
        column1.getColumn().setText("Type");
        tableLayout.setColumnData(column1.getColumn(), new ColumnWeightData(30, true));

        TableViewerColumn column2 = new TableViewerColumn(fTableViewer, SWT.NONE, 1);
        column2.getColumn().setText("Name");
        tableLayout.setColumnData(column2.getColumn(), new ColumnWeightData(40, true));

        TableViewerColumn column3 = new TableViewerColumn(fTableViewer, SWT.NONE, 2);
        column3.getColumn().setText("Status");
        tableLayout.setColumnData(column3.getColumn(), new ColumnWeightData(15, true));

        TableViewerColumn column4 = new TableViewerColumn(fTableViewer, SWT.NONE, 3);
        column4.getColumn().setText("Choice");
        tableLayout.setColumnData(column4.getColumn(), new ColumnWeightData(15, true));
        column4.setEditingSupport(new ComboChoiceEditingSupport(fTableViewer));

        // Content Provider
        fTableViewer.setContentProvider(new IStructuredContentProvider() {
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

            public void dispose() {
            }

            public Object[] getElements(Object inputElement) {
                try {
                    return fHandler.getMergeObjectInfos().toArray();
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
                
                return new Object[0];
            }
        });

        // Table Selection Listener
        fTableViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                MergeObjectInfo info = (MergeObjectInfo)((StructuredSelection)event.getSelection()).getFirstElement();
                fViewerOurs.setMergeInfo(info);
                fViewerTheirs.setMergeInfo(info);
            }
        });
        
        // Table Label Provider
        fTableViewer.setLabelProvider(new TableLabelProvider());
        
        // Start the table
        fTableViewer.setInput(""); // anything will do //$NON-NLS-1$
    }
    
    @Override
    protected void okPressed() {
//        List<String> ours = new ArrayList<String>();
//        List<String> theirs = new ArrayList<String>();
        
//        for(Object checked : fTableViewer.getCheckedElements()) {
//            theirs.add((String)checked);
//        }
//        
//        for(String s : fHandler.getMergeResult().getConflicts().keySet()) {
//            if(!theirs.contains(s)) {
//                ours.add(s);
//            }
//        }
        
//        fHandler.setOursAndTheirs(ours, theirs);
        
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


    // Object Viewer Control
    // ===========================================================
    
    private class ObjectViewer {
        private MergeObjectInfo mergeInfo;
        private int choice;
        
        private Button button;
        private Text text;
        
        ObjectViewer(Composite parent, int choice) {
            this.choice = choice;
            
            SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
            sash.setLayoutData(gd);
            
            Composite client = new Composite(sash, SWT.NONE);
            client.setLayoutData(new GridData(GridData.FILL_BOTH));
            client.setLayout(new GridLayout());
            
            button = new Button(client, SWT.PUSH);
            button.setText(choices[choice]);
            gd = new GridData(SWT.FILL, SWT.FILL, true, false);
            button.setLayoutData(gd);
            
            button.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    mergeInfo.setChoice(choice);
                    fTableViewer.update(mergeInfo, null);
                    fViewerOurs.update();
                    fViewerTheirs.update();
                }
            });
            
            text = new Text(client, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            text.setLayoutData(new GridData(GridData.FILL_BOTH));
            text.setBackground(fTableViewer.getControl().getBackground());
        }

        void setMergeInfo(MergeObjectInfo info) {
            mergeInfo = info;
            EObject eObject = mergeInfo.getEObject(choice);
            
            text.setText(eObject == null ? "Deleted" : eObject.toString());
            update();
        }
        
        void update() {
            String text = choices[choice];
            if(choice == mergeInfo.getChoice()) {
                text += " (selected)";
            }
            button.setText(text);
        }
    }
    
    
    // Table
    // ===========================================================
    
    // Label Provider
    private class TableLabelProvider extends LabelProvider implements ITableLabelProvider {
        
        public Image getColumnImage(Object element, int columnIndex) {
            if(columnIndex == 0) {
                MergeObjectInfo info = (MergeObjectInfo)element;
                return ArchiLabelProvider.INSTANCE.getImage(info.getDefault());
            }
            
            return null;
        }

        public String getColumnText(Object element, int columnIndex) {
            MergeObjectInfo info = (MergeObjectInfo)element;
            EObject eObject = info.getDefault();
            
            if(eObject == null) {
                return "(missing)";
            }
            
            switch(columnIndex) {
                case 0:
                    return ArchiLabelProvider.INSTANCE.getDefaultName(eObject.eClass());

                case 1:
                    return ArchiLabelProvider.INSTANCE.getLabel(eObject);

                case 2:
                    return info.getStatus();

                case 3:
                    return choices[info.getChoice()];

                default:
                    return "";
            }
        }
    }

    /**
     * Combo Choice Editor
     */
    private class ComboChoiceEditingSupport extends EditingSupport {
        private ComboBoxCellEditor cellEditor;
        
        public ComboChoiceEditingSupport(ColumnViewer viewer) {
            super(viewer);
            cellEditor = new ComboBoxCellEditor((Composite)viewer.getControl(), choices, SWT.READ_ONLY);
        }

        @Override
        protected CellEditor getCellEditor(Object element) {
            return cellEditor;
        }

        @Override
        protected boolean canEdit(Object element) {
            return true;
        }

        @Override
        protected Object getValue(Object element) {
            MergeObjectInfo info = (MergeObjectInfo)element;
            return info.getChoice();
        }

        @Override
        protected void setValue(Object element, Object value) {
            ((MergeObjectInfo)element).setChoice((int)value);
            fTableViewer.update(element, null);
            fViewerOurs.update();
            fViewerTheirs.update();
        }
    }

}