package org.archicontribs.modelrepository.actions;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.archicontribs.modelrepository.actions.messages"; //$NON-NLS-1$

    public static String CloneModelAction_0;

    public static String CloneModelAction_1;

    public static String CloneModelAction_2;

    public static String CloneModelAction_3;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
