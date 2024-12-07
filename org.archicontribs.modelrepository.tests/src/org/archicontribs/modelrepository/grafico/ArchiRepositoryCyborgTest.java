package org.archicontribs.modelrepository.grafico;

import java.io.IOException;

import org.archicontribs.modelrepository.grafico.ArchiRepositoryCyborg;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;

public class ArchiRepositoryCyborgTest {

	private final ArchiRepositoryCyborg underTest = new ArchiRepositoryCyborg(TestData.GIT_FOLDER);

	@Test
	public void canPull() throws IOException, GitAPIException {
		underTest.pullFromRemote(null, null);
	}
}
