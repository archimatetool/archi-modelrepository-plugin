package org.archicontribs.modelrepository.views.repositories;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.archicontribs.modelrepository.views.repositories.messages"; //$NON-NLS-1$

    public static String ModelRepositoryTreeViewer_0;

    public static String ModelRepositoryView_0;
    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
