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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.diagram.util.DiagramUtils;
import com.archimatetool.editor.ui.ArchiLabelProvider;
import com.archimatetool.editor.ui.IArchiImages;
import com.archimatetool.editor.ui.components.ExtendedTitleAreaDialog;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDocumentable;
import com.archimatetool.model.INameable;
import com.archimatetool.model.IProperties;
import com.archimatetool.model.IProperty;
import com.archimatetool.model.util.ArchimateModelUtils;

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
        private Composite fieldsComposite;
        
        private Label labelDocumentation;
        private Text textType, textName, textDocumentation;
        private TableViewer propertiesTableViewer;
        
        private Image viewImage;
        private Label viewLabel;
        
        ObjectViewer(Composite parent, int choice) {
            this.choice = choice;
            
            // Dispose of image when done
            parent.addDisposeListener((e) -> {
                disposeImage();
            });
            
            SashForm sash = new SashForm(parent, SWT.HORIZONTAL);
            GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
            sash.setLayoutData(gd);
            
            Composite mainComposite = new Composite(sash, SWT.BORDER);
            mainComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            mainComposite.setLayout(new GridLayout());
            mainComposite.setBackground(fTableViewer.getControl().getBackground());
            mainComposite.setBackgroundMode(SWT.INHERIT_FORCE);
            
            button = new Button(mainComposite, SWT.PUSH);
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
            
            fieldsComposite = new Composite(mainComposite, SWT.NONE);
            fieldsComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
            fieldsComposite.setLayout(new GridLayout(2, false));
            
            // Type
            Label label = new Label(fieldsComposite, SWT.NONE);
            label.setText("Type:");
            textType = new Text(fieldsComposite, SWT.READ_ONLY | SWT.BORDER);
            textType.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
            
            // Name
            label = new Label(fieldsComposite, SWT.NONE);
            label.setText("Name:");
            textName = new Text(fieldsComposite, SWT.READ_ONLY | SWT.BORDER);
            textName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

            // Documentation / Purpose
            labelDocumentation = new Label(fieldsComposite, SWT.NONE);
            labelDocumentation.setText("Documentation:");
            labelDocumentation.setLayoutData(new GridData(SWT.TOP, SWT.TOP, false, false));
            textDocumentation = new Text(fieldsComposite, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            textDocumentation.setLayoutData(new GridData(GridData.FILL_BOTH));
            
            // Properties Table
            label = new Label(fieldsComposite, SWT.NONE);
            label.setText("Properties:");
            label.setLayoutData(new GridData(SWT.TOP, SWT.TOP, false, false));
            Composite tableComp = new Composite(fieldsComposite, SWT.BORDER);
            TableColumnLayout tableLayout = new TableColumnLayout();
            tableComp.setLayout(tableLayout);
            tableComp.setLayoutData(new GridData(GridData.FILL_BOTH));

            propertiesTableViewer = new TableViewer(tableComp, SWT.MULTI | SWT.FULL_SELECTION);
            propertiesTableViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
            propertiesTableViewer.getTable().setHeaderVisible(true);
            propertiesTableViewer.getTable().setLinesVisible(true);
            propertiesTableViewer.setComparator(new ViewerComparator());
            
            TableViewerColumn columnKey = new TableViewerColumn(propertiesTableViewer, SWT.NONE, 0);
            columnKey.getColumn().setText("Name");
            tableLayout.setColumnData(columnKey.getColumn(), new ColumnWeightData(25, true));

            TableViewerColumn columnValue = new TableViewerColumn(propertiesTableViewer, SWT.NONE, 1);
            columnValue.getColumn().setText("Value");
            tableLayout.setColumnData(columnValue.getColumn(), new ColumnWeightData(75, true));

            // Properties Table Content Provider
            propertiesTableViewer.setContentProvider(new IStructuredContentProvider() {
                public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                }

                public void dispose() {
                }

                public Object[] getElements(Object inputElement) {
                    if(inputElement instanceof IProperties) {
                        return ((IProperties)inputElement).getProperties().toArray();
                    }
                    return new Object[0];
                }
            });

            // Properties Table Label Provider
            class PropertiesLabelCellProvider extends LabelProvider implements ITableLabelProvider {
                public Image getColumnImage(Object element, int columnIndex) {
                    return null;
                }

                @Override
                public String getColumnText(Object element, int columnIndex) {
                    switch(columnIndex) {
                        case 0:
                            return ((IProperty)element).getKey();

                        case 1:
                            return ((IProperty)element).getValue();

                        default:
                            return null;
                    }
                }
            }

            propertiesTableViewer.setLabelProvider(new PropertiesLabelCellProvider());
            
            // View image previewer
            ScrolledComposite sc = new ScrolledComposite(fieldsComposite, SWT.H_SCROLL | SWT.V_SCROLL );
            gd = new GridData(GridData.FILL_BOTH);
            gd.heightHint = 120;
            gd.horizontalSpan = 2;
            sc.setLayoutData(gd);
            viewLabel = new Label(sc, SWT.NONE);
            sc.setContent(viewLabel);
        }

        void setMergeInfo(MergeObjectInfo info) {
            disposeImage();
            
            mergeInfo = info;
            EObject eObject = mergeInfo.getEObject(choice);
            
            // If object has been deleted hide fields
            fieldsComposite.setVisible(eObject != null);
            
            // Object was deleted
            if(eObject == null) {
                return;
            }
            
            // Type
            textType.setText(ArchiLabelProvider.INSTANCE.getDefaultName(eObject.eClass()));
            
            // Name
            if(eObject instanceof INameable) {
                textName.setText(((INameable)eObject).getName());
            }
            else {
                textName.setText(""); //$NON-NLS-1$
            }
            
            // Documentation / Purpose
            if(eObject instanceof IDocumentable) {
                labelDocumentation.setText("Documentation:");
                textDocumentation.setText(((IDocumentable)eObject).getDocumentation());
            }
            else if(eObject instanceof IArchimateModel) {
                labelDocumentation.setText("Purpose:");
                textDocumentation.setText(((IArchimateModel)eObject).getPurpose());
            }
            else {
                textDocumentation.setText(""); //$NON-NLS-1$
            }
            
            // Properties
            if(eObject instanceof IProperties) {
                propertiesTableViewer.setInput(eObject);
            }
            else {
                propertiesTableViewer.setInput(""); //$NON-NLS-1$
            }
            
            // View
            if(eObject instanceof IDiagramModel) {
                try {
                    IArchimateModel model = null;
                    if(choice == MergeObjectInfo.OURS) {
                        model = fHandler.getOurModel();
                    }
                    else {
                        model = fHandler.getTheirModel();
                    }
                    IDiagramModel view = (IDiagramModel)ArchimateModelUtils.getObjectByID(model, ((IDiagramModel)eObject).getId());
                    viewImage = DiagramUtils.createImage(view, 1, 0);
                    viewLabel.setImage(viewImage);
                    viewLabel.setSize(viewLabel.computeSize( SWT.DEFAULT, SWT.DEFAULT));
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
            else {
                viewLabel.setImage(null);
            }
            
            update();
        }
        
        void update() {
            String text = choices[choice];
            if(choice == mergeInfo.getChoice()) {
                text += " " + "(selected)"; //$NON-NLS-1$
            }
            button.setText(text);
        }
        
        private void disposeImage() {
            if(viewImage != null && !viewImage.isDisposed()) {
                viewImage.dispose();
                viewImage = null;
            }
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