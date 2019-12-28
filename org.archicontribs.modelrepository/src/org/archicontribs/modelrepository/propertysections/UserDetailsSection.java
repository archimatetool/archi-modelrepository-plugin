/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import java.io.IOException;

import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.archimatetool.editor.propertysections.AbstractArchiPropertySection;
import com.archimatetool.editor.ui.ColorFactory;
import com.archimatetool.editor.ui.ThemeUtils;
import com.archimatetool.editor.utils.StringUtils;


/**
 * Property Section for Repo information
 * 
 * @author Phillip Beauvoir
 */
public class UserDetailsSection extends AbstractArchiPropertySection {
    
    public static class Filter implements IFilter {
        @Override
        public boolean select(Object object) {
            return object instanceof IArchiRepository;
        }
    }
    
    private IArchiRepository fRepository;
    
    private UserText fTextName;
    private UserText fTextEmail;
 
    private static final Color lightGrey = ColorFactory.get(188, 188, 188);
    private static final Color darkGrey = ColorFactory.get(112, 112, 112);
    
    private class UserText {
        Text text;
        String field;
        String localValue;
        String globalValue;
        private Color originalForeGroundColor;

        Listener listener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                String newValue = text.getText();
                
                // If it's an empty string set to global value
                if(!StringUtils.isSet(newValue)) {
                    newValue = globalValue;
                }
                
                switch(event.type) {
                    case SWT.FocusIn:
                        // If global value == new value, set to empty
                        if(globalValue.equals(newValue)) {
                            text.setForeground(originalForeGroundColor);
                            text.setText(""); //$NON-NLS-1$
                        }
                        break;

                    case SWT.FocusOut:
                        text.setText(newValue);
                        // Fall through...

                    case SWT.DefaultSelection:
                        // Different value so save and store
                        if(!localValue.equals(newValue)) {
                            localValue = newValue;
                            saveToLocalConfig(field, globalValue, localValue);
                        }
                        
                        updateColor();
                        break;
                    
                    default:
                        break;
                }
             }
        };
        
        UserText(Composite parent, String field) {
            this.field = field;
            
            text = createSingleTextControl(parent, SWT.NONE);
            
            text.addListener(SWT.DefaultSelection, listener);
            text.addListener(SWT.FocusIn, listener);
            text.addListener(SWT.FocusOut, listener);
            
            text.addDisposeListener((event) -> {
                text.removeListener(SWT.DefaultSelection, listener);
                text.removeListener(SWT.FocusIn, listener);
                text.removeListener(SWT.FocusOut, listener);
            });
        }

        void setText(String globalValue, String localValue) {
            this.globalValue = globalValue;
            this.localValue = localValue;
            refresh();
        }
        
        void refresh() {
            text.setText(localValue);
            updateColor();
        }
        
        void updateColor() {
            // The foreground of the text control is set later if we are using a theme
            if(originalForeGroundColor == null) {
                text.getDisplay().asyncExec(() -> {
                    if(!text.isDisposed()) {
                        originalForeGroundColor = text.getForeground();
                        _updateColor();
                    }
                });
            }
            else {
                _updateColor();
            }
        }
        
        void _updateColor() {
            if(globalValue.equals(localValue)) {
                text.setForeground(ThemeUtils.isDarkTheme() ? darkGrey : lightGrey);
            }
            else {
                text.setForeground(originalForeGroundColor);
            }
        }
    }
    
    public UserDetailsSection() {
    }

    @Override
    protected void createControls(Composite parent) {
        Group group = getWidgetFactory().createGroup(parent, Messages.UserDetailsSection_2);
        GridData gd = new GridData(GridData.FILL_BOTH);
        gd.horizontalSpan = 2;
        group.setLayoutData(gd);
        group.setLayout(new GridLayout(2, false));
        
        createLabel(group, Messages.UserDetailsSection_0, STANDARD_LABEL_WIDTH, SWT.CENTER);
        fTextName = new UserText(group, ConfigConstants.CONFIG_KEY_NAME);
        
        createLabel(group, Messages.UserDetailsSection_1, STANDARD_LABEL_WIDTH, SWT.CENTER);
        fTextEmail = new UserText(group, ConfigConstants.CONFIG_KEY_EMAIL);
    }
    
    @Override
    protected void handleSelection(IStructuredSelection selection) {
        if(selection.getFirstElement() instanceof IArchiRepository) {
            fRepository = (IArchiRepository)selection.getFirstElement();
            
            String globalName = "", globalEmail = ""; //$NON-NLS-1$ //$NON-NLS-2$
            String localName = "", localEmail = ""; //$NON-NLS-1$ //$NON-NLS-2$
            
            // Get global name, email
            try {
                PersonIdent global = GraficoUtils.getGitConfigUserDetails();
                globalName = global.getName();
                globalEmail = global.getEmailAddress();
            }
            catch(IOException | ConfigInvalidException ex) {
                ex.printStackTrace();
            }
            
            // Get local name, email
            try {
                PersonIdent local = fRepository.getUserDetails();
                localName = local.getName();
                localEmail = local.getEmailAddress();
            }
            catch(IOException ex) {
                ex.printStackTrace();
            }
            
            fTextName.setText(globalName, localName);
            fTextEmail.setText(globalEmail, localEmail);
        }
        else {
            System.err.println(getClass() + " failed to get element for " + selection.getFirstElement()); //$NON-NLS-1$
        }
    }
    
    private void saveToLocalConfig(String name, String globalValue, String localValue) {
        try(Git git = Git.open(fRepository.getLocalRepositoryFolder())) {
            StoredConfig config = git.getRepository().getConfig();
            
            // Unset if blank or same as 
            if(!StringUtils.isSet(localValue) || globalValue.equals(localValue)) {
                config.unset(ConfigConstants.CONFIG_USER_SECTION, null, name);
            }
            // Set
            else {
                config.setString(ConfigConstants.CONFIG_USER_SECTION, null, name, localValue);
            }
            
            config.save();
        }
        catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}
