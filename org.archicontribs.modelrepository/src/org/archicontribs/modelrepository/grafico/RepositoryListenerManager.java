/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.grafico;

import java.beans.PropertyChangeEvent;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.emf.common.notify.Notification;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimatePackage;

/**
 * Central manager for notifying listeners about repository and model changes
 * 
 * @author Phillip Beauvoir
 */
public class RepositoryListenerManager {

    public static final RepositoryListenerManager INSTANCE = new RepositoryListenerManager();
    
    private CopyOnWriteArrayList<IRepositoryListener> listeners = new CopyOnWriteArrayList<>();
    
    private RepositoryListenerManager() {
        // Listen to open model changes
        IEditorModelManager.INSTANCE.addPropertyChangeListener(this::modelPropertyChanged);
    }

    public void addListener(IRepositoryListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(IRepositoryListener listener) {
        listeners.remove(listener);
    }

    public void fireRepositoryChangedEvent(String eventName, IArchiRepository repository) {
        if(repository == null) {
            return;
        }
        
        for(IRepositoryListener listener : listeners) {
            listener.repositoryChanged(eventName, repository);
        }
    }

    /**
     * EditorModelManager Property Change listener
     */
    private void modelPropertyChanged(PropertyChangeEvent evt) {
        // Notify on Save
        if(evt.getPropertyName().equals(IEditorModelManager.PROPERTY_MODEL_SAVED)) {
            notifyModelChanged((IArchimateModel)evt.getNewValue());
        }
        // Notify on model name change
        else if(evt.getPropertyName().equals(IEditorModelManager.PROPERTY_ECORE_EVENT)) {
            Notification msg = (Notification)evt.getNewValue();
            if(msg.getNotifier() instanceof IArchimateModel && msg.getFeature() == IArchimatePackage.Literals.NAMEABLE__NAME) {
                notifyModelChanged((IArchimateModel)msg.getNotifier());
            }
        }
    }

    /**
     * If model changed and is in a repo, send notification
     */
    private void notifyModelChanged(IArchimateModel model) {
        if(GraficoUtils.isModelInLocalRepository(model)) {
            IArchiRepository repo = new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model));
            fireRepositoryChangedEvent(IRepositoryListener.REPOSITORY_CHANGED, repo);
        }
    }
}
