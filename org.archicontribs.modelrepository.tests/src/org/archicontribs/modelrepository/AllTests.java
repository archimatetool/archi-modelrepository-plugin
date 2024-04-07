/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;


import org.archicontribs.modelrepository.authentication.CryptoDataTests;
import org.archicontribs.modelrepository.grafico.ArchiRepositoryTests;
import org.archicontribs.modelrepository.grafico.GraficoUtilsTests;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SelectClasses({
    ArchiRepositoryTests.class,
    GraficoUtilsTests.class,
    CryptoDataTests.class
})
@SuiteDisplayName("All Model Repository Tests")
public class AllTests {
}
