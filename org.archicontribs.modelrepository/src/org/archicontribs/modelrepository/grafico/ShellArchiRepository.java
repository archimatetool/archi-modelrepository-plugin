package org.archicontribs.modelrepository.grafico;

import java.io.File;
import java.io.IOException;

import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

import com.archimatetool.editor.actions.AbstractModelAction;

import de.esserjan.edu.imbecile.GitExecutionException;
import de.esserjan.edu.imbecile.GitExecutionResult;
import de.esserjan.edu.imbecile.GitExecutor;
import de.esserjan.edu.imbecile.GitExecutor.PullMode;

/**
 * ShellArchiRepository challenges ArchiRepository with alternative GIT
 * back-end.
 * 
 * Hexagonal force field: driven by: descendants of {@link AbstractModelAction},
 * driver: git-executor
 */
public class ShellArchiRepository {

	public static enum PullOutcome {
		ALREADY_UP_TO_DATE, PULLED_SUCCESSFULLY, PULL_INCOMPLETE
	}

	private GitExecutor executor;
	
	public ShellArchiRepository() {		
		// inject manually as this isn't managed instance
		BundleContext bundleContext = FrameworkUtil.getBundle(getClass()).getBundleContext();
		this.executor = bundleContext.getService(bundleContext.getServiceReference(GitExecutor.class));
		this.executor.setGitExecutable(new File("/snap/eclipse-pde/current/usr/bin/git"));
	}

	public void setLocalRepoFolder(File localRepoFolder) {
		this.executor.setGitRepo(localRepoFolder);		
	}
	
	public PullOutcome pullFromRemote(UsernamePassword npw) throws IOException {
		try {
			GitExecutionResult result = executor.pull(PullMode.REBASE_MERGE);
			switch (result.exitCode()) {
			case 0:
				if (result.outputText().endsWith("."))
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
