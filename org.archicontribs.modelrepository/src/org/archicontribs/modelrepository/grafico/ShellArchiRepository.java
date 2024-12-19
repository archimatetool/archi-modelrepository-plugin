package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.GitExecutor.PullMode;
import org.eclipse.jgit.api.errors.GitAPIException;

public class ShellArchiRepository {

	private final GitExecutor executor;
	
	public ShellArchiRepository(File localRepoFolder) {		
		try {
			this.executor = new GitExecutor(localRepoFolder);
		} catch (GitExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public GitExecutionResult pullFromRemote(UsernamePassword npw)
			throws IOException, GitAPIException {
		try {
			GitExecutionResult result = executor.pull(PullMode.REBASE_MERGE);
			
			return result;
		} catch (GitExecutionException e) {
			throw new IOException(e);
		}
	}
	
	

}
