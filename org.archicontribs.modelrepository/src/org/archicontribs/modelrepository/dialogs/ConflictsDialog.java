/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.dialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
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
        
        SashForm sash = new SashForm(container, SWT.VERTICAL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        sash.setLayoutData(gd);
        
        createTableControl(sash);
        
        SashForm sash2 = new SashForm(sash, SWT.HORIZONTAL);
        sash2.setLayoutData(gd);
        
        fFileViewerOurs = createFileViewerControl(sash2, "Local version");
        fFileViewerDiff = createFileViewerControl(sash2, "Difference");
        fFileViewerTheirs = createFileViewerControl(sash2, "Online version");
        
        sash.setWeights(new int[] { 30, 70 });
        
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
        column1.getColumn().setText("Select to use online version, deselect to keep your version.");
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
                    fFileViewerOurs.setText(getFileContents(path, Constants.HEAD));
                    fFileViewerTheirs.setText(getFileContents(path, "origin/master")); //$NON-NLS-1$
                    fFileViewerDiff.setText(getWorkingTreeFileContents(path));
                }
                catch(IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
        
        // Label Provider
        fTableViewer.setLabelProvider(new LabelProvider());
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
    
    /**
     * Get the file contents of a file in HEAD or some other ref
     */
    private String getFileContents(String path, String ref) throws IOException {
        String str = ""; //$NON-NLS-1$
        
        try(Repository repository = Git.open(fHandler.getLocalGitFolder()).getRepository()) {
            ObjectId lastCommitId = repository.resolve(ref);

            try(RevWalk revWalk = new RevWalk(repository)) {
                RevCommit commit = revWalk.parseCommit(lastCommitId);
                RevTree tree = commit.getTree();

                // now try to find a specific file
                try(TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(path));

                    if(!treeWalk.next()) {
                        return "(File not found)";
                    }

                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);

                    str = new String(loader.getBytes());
                }

                revWalk.dispose();
            }
        }
        
        return str;
    }

    /**
     * Get the file contents of a file in working tree
     */
    private String getWorkingTreeFileContents(String path) throws IOException {
        String str = ""; //$NON-NLS-1$
        
        try(Git git = Git.open(fHandler.getLocalGitFolder())) {
            BufferedReader in = new BufferedReader(new FileReader(new File(fHandler.getLocalGitFolder(), path)));
            String line;
            while((line = in.readLine()) != null) {
                str += line + "\n"; //$NON-NLS-1$
            }

            in.close();
        }
        
        return str;
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