package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.GitExecutor.PullMode;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ProgressMonitor;

public class ArchiRepositoryCyborg extends ArchiRepository {

	private final GitExecutor executor;
	private final JGitAdapter adapter = new JGitAdapter();
	
	public ArchiRepositoryCyborg(File localRepoFolder) {
		super(localRepoFolder);
		
		try {
			this.executor = new GitExecutor(localRepoFolder);
		} catch (GitExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public PullResult pullFromRemote(UsernamePassword npw, ProgressMonitor monitor)
			throws IOException, GitAPIException {
		try {
			GitExecutionResult result = executor.pull(PullMode.REBASE_MERGE);
			
			return adapter.pullResult(result.outputLine());
		} catch (GitExecutionException e) {
			throw new IOException(e);
		}
	}
	
	

}
