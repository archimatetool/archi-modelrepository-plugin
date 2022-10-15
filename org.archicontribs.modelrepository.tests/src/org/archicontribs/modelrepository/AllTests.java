/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository;


import org.archicontribs.modelrepository.authentication.CryptoDataTests;
import org.archicontribs.modelrepository.grafico.ArchiRepositoryTests;
import org.archicontribs.modelrepository.grafico.GraficoUtilsTests;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)

@Suite.SuiteClasses({
    ArchiRepositoryTests.class,
    GraficoUtilsTests.class,
    CryptoDataTests.class
})

public class AllTests {
}
