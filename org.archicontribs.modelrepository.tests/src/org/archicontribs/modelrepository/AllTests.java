/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;


import org.archicontribs.modelrepository.grafico.GraficoUtilsTests;

import junit.framework.TestSuite;

@SuppressWarnings("nls")
public class AllTests {

    public static junit.framework.Test suite() {
		TestSuite suite = new TestSuite("org.archicontribs.modelrepository");

		suite.addTest(GraficoUtilsTests.suite());
		
        return suite;
	}

}