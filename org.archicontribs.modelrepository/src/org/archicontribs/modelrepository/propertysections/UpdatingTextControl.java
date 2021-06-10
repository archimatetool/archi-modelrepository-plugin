/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.propertysections;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;


/**
 * Wrapper Control for Text or StyledText Control to retrieve and update a text value
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public abstract class UpdatingTextControl {
    
    private Control fTextControl;
    private Listener eventListener = this::handleEvent;
    
    // Store previous text
    private String previousText = "";
    
    // Whether or not to pass on text changed events
    private boolean doNotify = true;
    
    /**
     * @param textControl The Text Control
     */
    public UpdatingTextControl(Control textControl) {
        fTextControl = textControl;
        
        // FocusOut updates the text
        textControl.addListener(SWT.FocusOut, eventListener);
        
        // Listen for Enter (or Ctrl+Enter) keypress
        textControl.addListener(SWT.DefaultSelection, eventListener);
        
        textControl.addDisposeListener(event -> {
            textControl.removeListener(SWT.FocusOut, eventListener);
            textControl.removeListener(SWT.DefaultSelection, eventListener);
            previousText = null;
        });
    }
    
    public Control getTextControl() {
        return fTextControl;
    }
    
    public void setEditable(boolean editable) {
        if(isStyledTextControl()) {
            ((StyledText)getTextControl()).setEditable(editable);
        }
        else {
            ((Text)getTextControl()).setEditable(editable);
        }
    }
    
    public void setEnabled(boolean enabled) {
        getTextControl().setEnabled(enabled);
    }
    
    public String getText() {
        if(isStyledTextControl()) {
            return ((StyledText)getTextControl()).getText();
        }
        else {
            return ((Text)getTextControl()).getText();
        }
    }
    
    public void setText(String s) {
        if(isStyledTextControl()) {
            ((StyledText)getTextControl()).setText(s);
        }
        else {
            ((Text)getTextControl()).setText(s);
        }
        
        previousText = s;
    }
    
    /**
     * Whether or not to notify when the text changes
     * Default is true
     */
    public void setNotifications(boolean enabled) {
        doNotify = enabled;
    }
    
    private void handleEvent(Event event) {
        if(doNotify && !getText().equals(previousText)) {
            textChanged(getText());
            previousText = getText();
        }
    }
    
    private boolean isStyledTextControl() {
        return getTextControl() instanceof StyledText;
    }

    /**
     * Clients must over-ride this to react to text changes
     * @param newText The new changed text
     */
    protected abstract void textChanged(String newText);
}
