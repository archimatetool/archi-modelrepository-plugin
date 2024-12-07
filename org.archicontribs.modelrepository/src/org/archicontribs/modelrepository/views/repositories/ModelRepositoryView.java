/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.repositories;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.actions.AbortChangesAction;
import org.archicontribs.modelrepository.actions.CloneModelAction;
import org.archicontribs.modelrepository.actions.CommitModelAction;
import org.archicontribs.modelrepository.actions.DeleteModelAction;
import org.archicontribs.modelrepository.actions.IGraficoModelAction;
import org.archicontribs.modelrepository.actions.OpenModelAction;
import org.archicontribs.modelrepository.actions.PropertiesAction;
import org.archicontribs.modelrepository.actions.PushModelAction;
import org.archicontribs.modelrepository.actions.RefreshModelAction;
import org.archicontribs.modelrepository.actions.ShowInBranchesViewAction;
import org.archicontribs.modelrepository.actions.ShowInHistoryAction;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.preferences.IPreferenceConstants;
import org.archicontribs.modelrepository.views.repositories.ModelRepositoryTreeViewer.ModelRepoTreeLabelProvider;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.tabbed.ITabbedPropertySheetPageContributor;
import org.eclipse.ui.views.properties.tabbed.TabbedPropertySheetPage;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;


/**
 * Model Repository ViewPart for managing models
 */
public class ModelRepositoryView
extends ViewPart
implements IContextProvider, ISelectionListener, ITabbedPropertySheetPageContributor {

	public static String ID = ModelRepositoryPlugin.PLUGIN_ID + ".modelRepositoryView"; //$NON-NLS-1$
    public static String HELP_ID = ModelRepositoryPlugin.PLUGIN_ID + ".modelRepositoryViewHelp"; //$NON-NLS-1$
    
    /**
     * The Repository Viewer
     */
    private ModelRepositoryTreeViewer fTreeViewer;
    
    /*
     * Actions
     */
    private IGraficoModelAction fActionClone;
    
    private IGraficoModelAction fActionOpen;
    private IGraficoModelAction fActionRefresh;
    private IGraficoModelAction fActionDelete;
    
    private IGraficoModelAction fActionAbortChanges;
    
    private IGraficoModelAction fActionCommit;
    private IGraficoModelAction fActionPush;
    
    private IGraficoModelAction fActionShowInHistory;
    private IGraficoModelAction fActionShowInBranches;
    private IGraficoModelAction fActionProperties;
    

    @Override
    public void createPartControl(Composite parent) {
        // Create the Tree Viewer first
        fTreeViewer = new ModelRepositoryTreeViewer(parent);

        
        makeActions();
        registerGlobalActions();
        hookContextMenu();
        makeLocalMenuActions();
        makeLocalToolBarActions();
        
        // Register us as a selection provider so that Actions can pick us up
        getSite().setSelectionProvider(getViewer());
        
        /*
         * Listen to Selections to update local Actions
         */
        getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                updateActions(event.getSelection());
                updateStatusBar(event.getSelection());
            }
        });
        
        // Listen to workbench selections
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
        
        // Initialise with whatever is selected in the workbench
        selectionChanged(getSite().getWorkbenchWindow().getPartService().getActivePart(),
                getSite().getWorkbenchWindow().getSelectionService().getSelection());
        
        /*
         * Listen to Double-click Action
         */
        getViewer().addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                Object obj = ((IStructuredSelection)event.getSelection()).getFirstElement();
                if(obj instanceof IArchiRepository) {
                    IArchiRepository repo = (IArchiRepository)obj;
                    BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
                        @Override
                        public void run() {
                            IEditorModelManager.INSTANCE.openModel(repo.getTempModelFile());
                        }
                    });
                }
            }
        });

        // Register Help Context
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getViewer().getControl(), HELP_ID);
    }
    
    /**
     * Make local actions
     */
    private void makeActions() {
        fActionClone = new CloneModelAction(getViewSite().getWorkbenchWindow());
        
        fActionOpen = new OpenModelAction(getViewSite().getWorkbenchWindow());
        fActionOpen.setEnabled(false);
        
        fActionRefresh = new RefreshModelAction(getViewSite().getWorkbenchWindow());
        fActionRefresh.setEnabled(false);
        
        fActionDelete = new DeleteModelAction(getViewSite().getWorkbenchWindow());
        fActionDelete.setEnabled(false);
        
        fActionAbortChanges = new AbortChangesAction(getViewSite().getWorkbenchWindow());
        fActionAbortChanges.setEnabled(false);
        
        fActionCommit = new CommitModelAction(getViewSite().getWorkbenchWindow());
        fActionCommit.setEnabled(false);
        
        fActionPush = new PushModelAction(getViewSite().getWorkbenchWindow());
        fActionPush.setEnabled(false);
        
        fActionShowInHistory = new ShowInHistoryAction(getViewSite().getWorkbenchWindow());
        fActionShowInHistory.setEnabled(false);
        
        fActionShowInBranches = new ShowInBranchesViewAction(getViewSite().getWorkbenchWindow());
        fActionShowInBranches.setEnabled(false);

        fActionProperties = new PropertiesAction(getViewSite().getWorkbenchWindow());
        fActionProperties.setEnabled(false);
        
        // Register the Keybinding for actions
//        IHandlerService service = (IHandlerService)getViewSite().getService(IHandlerService.class);
//        service.activateHandler(fActionRefresh.getActionDefinitionId(), new ActionHandler(fActionRefresh));
    }

    /**
     * Register Global Action Handlers
     */
    private void registerGlobalActions() {
        IActionBars actionBars = getViewSite().getActionBars();
        
        // Register our interest in the global menu actions
        actionBars.setGlobalActionHandler(ActionFactory.PROPERTIES.getId(), fActionProperties);
    }

    /**
     * Hook into a right-click menu
     */
    private void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#RepoViewerPopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        
        Menu menu = menuMgr.createContextMenu(getViewer().getControl());
        getViewer().getControl().setMenu(menu);
        
        getSite().registerContextMenu(menuMgr, getViewer());
    }
    
    /**
     * Make Any Local Bar Menu Actions
     */
    private void makeLocalMenuActions() {
        IActionBars actionBars = getViewSite().getActionBars();

        // Local menu items go here
        IMenuManager manager = actionBars.getMenuManager();
        
        // Fetch in Background preference
        IPreferenceStore store = ModelRepositoryPlugin.getInstance().getPreferenceStore();
        
        IAction fetchAction = new Action(Messages.ModelRepositoryView_1, IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                store.setValue(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND, isChecked());
            }
        };
        
        manager.add(fetchAction);
        fetchAction.setChecked(store.getBoolean(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND));
        
        IPropertyChangeListener listener = new IPropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent event) {
                fetchAction.setChecked(store.getBoolean(IPreferenceConstants.PREFS_FETCH_IN_BACKGROUND));
            }
        };
        
        store.addPropertyChangeListener(listener);
        
        getViewer().getControl().addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                store.removePropertyChangeListener(listener);
            }
        });
    }

    /**
     * Make Local Toolbar items
     */
    private void makeLocalToolBarActions() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager manager = bars.getToolBarManager();

        manager.add(new Separator(IWorkbenchActionConstants.NEW_GROUP));
        
        manager.add(fActionClone);
        manager.add(fActionDelete);
    }
    
    /**
     * Update the Local Actions depending on the selection 
     * @param selection
     */
    private void updateActions(ISelection selection) {
        Object obj = ((IStructuredSelection)selection).getFirstElement();
        
        if(obj instanceof IArchiRepository) {
            IArchiRepository repo = (IArchiRepository)obj;
            
            fActionRefresh.setRepository(repo);
            fActionOpen.setRepository(repo);
            fActionDelete.setRepository(repo);
            fActionAbortChanges.setRepository(repo);
            
            fActionCommit.setRepository(repo);
            fActionPush.setRepository(repo);
            
            fActionShowInHistory.setRepository(repo);
            fActionShowInBranches.setRepository(repo);
            
            fActionProperties.setRepository(repo);
        }
    }
    
    private void updateStatusBar(ISelection selection) {
        Object obj = ((IStructuredSelection)selection).getFirstElement();
        
        if(obj instanceof IArchiRepository) {
            IArchiRepository repo = (IArchiRepository)obj;
            ModelRepoTreeLabelProvider labelProvider = (ModelRepoTreeLabelProvider)getViewer().getLabelProvider();
            Image image = labelProvider.getImage(repo);
            String text = repo.getName() + " - " + labelProvider.getStatusText(repo); //$NON-NLS-1$
            getViewSite().getActionBars().getStatusLineManager().setMessage(image, text);
        }
        else {
            getViewSite().getActionBars().getStatusLineManager().setMessage(null, ""); //$NON-NLS-1$
        }
    }
    
    private void fillContextMenu(IMenuManager manager) {
        boolean isEmpty = getViewer().getSelection().isEmpty();

        if(isEmpty) {
            manager.add(fActionClone);
        }
        else {
            manager.add(fActionOpen);
            manager.add(fActionRefresh);
            manager.add(new Separator());
            manager.add(fActionShowInHistory);
            manager.add(fActionShowInBranches);
            manager.add(new Separator());
            manager.add(fActionDelete);
            manager.add(new Separator());
            manager.add(fActionProperties);
        }
    }

    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if(part == null || part == this) {
            return;
        }
        
        // Model selected, but is it in a git repo?
        IArchimateModel model = part.getAdapter(IArchimateModel.class);
        if(model != null) {
            if(GraficoUtils.isModelInLocalRepository(model)) {
                IArchiRepository selectedRepository = new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model));
                getViewer().setSelection(new StructuredSelection(selectedRepository));
            }
        }
    }

    /**
     * @return The Viewer
     */
    public TreeViewer getViewer() {
        return fTreeViewer;
    }
    
    @Override
    public void setFocus() {
        if(getViewer() != null) {
            getViewer().getControl().setFocus();
        }
    }
    
    public void selectObject(Object object) {
        getViewer().setSelection(new StructuredSelection(object));
    }
    
    @Override
    public String getContributorId() {
        return ModelRepositoryPlugin.PLUGIN_ID;
    }
    
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        /*
         * Return the PropertySheet Page
         */
        if(adapter == IPropertySheetPage.class) {
            return adapter.cast(new TabbedPropertySheetPage(this));
        }
        
        /*
         * Return the active repository
         */
        if(adapter == IArchiRepository.class) {
            Object obj = getViewer().getStructuredSelection().getFirstElement();
            if(obj instanceof IArchiRepository) {
                return adapter.cast(obj);
            }
        }
        
        return super.getAdapter(adapter);
    }

    @Override
    public void dispose() {
        super.dispose();
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
    }

    // =================================================================================
    //                       Contextual Help support
    // =================================================================================
    
    @Override
    public int getContextChangeMask() {
        return NONE;
    }

    @Override
    public IContext getContext(Object target) {
        return HelpSystem.getContext(HELP_ID);
    }

    @Override
    public String getSearchExpression(Object target) {
        return Messages.ModelRepositoryView_0;
    }
}
