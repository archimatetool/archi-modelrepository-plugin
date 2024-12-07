package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.GitExecutor.PullMode;

import com.archimatetool.editor.actions.AbstractModelAction;

/**
 * ShellArchiRepository challenges ArchiRepository with alternative GIT
 * back-end.
 * 
 * Hexagonal force field: driven by: descendants of {@link AbstractModelAction}, driver: git-executor
 */
public class ShellArchiRepository {

	public static enum PullOutcome {
		ALREADY_UP_TO_DATE, PULLED_SUCCESSFULLY, PULL_INCOMPLETE
	}

	private final GitExecutor executor;

	public ShellArchiRepository(File localRepoFolder) {
		try {
			this.executor = new GitExecutor(new File("/snap/eclipse-pde/current/usr/bin/git"), localRepoFolder);
		} catch (GitExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	public PullOutcome pullFromRemote(UsernamePassword npw) throws IOException {
		try {
			GitExecutionResult result = executor.pull(PullMode.REBASE_MERGE);
			switch (result.exitCode()) {
			case 0:
				if (result.outputLine().endsWith("."))
					return PullOutcome.ALREADY_UP_TO_DATE;
				else
					return PullOutcome.PULLED_SUCCESSFULLY;
			default:
				return PullOutcome.PULL_INCOMPLETE;
			}
		} catch (GitExecutionException e) {
			throw new IOException(e);
		}
	}

	public boolean hasChanges() throws IOException {
		try {
			return executor.hasChanges();
		} catch (GitExecutionException e) {
			throw new IOException(e);
		}
	}

	public boolean commit(String commitMessage, boolean amend) throws IOException {
		try {
			return 0 == executor.commit(commitMessage, amend, false, true).exitCode();
		} catch (GitExecutionException e) {
			throw new IOException(e);
		}
	}

}
