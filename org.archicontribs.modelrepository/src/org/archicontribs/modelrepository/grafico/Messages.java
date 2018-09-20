package org.archicontribs.modelrepository.grafico;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.archicontribs.modelrepository.grafico.messages"; //$NON-NLS-1$

    public static String ArchiRepository_0;

    public static String GraficoModelLoader_0;

    public static String GraficoResolutionHandler_0;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
