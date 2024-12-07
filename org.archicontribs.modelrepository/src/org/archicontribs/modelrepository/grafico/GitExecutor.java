package org.archicontribs.modelrepository.grafico;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * GitExecutor allows direct access to git-cli.
 * 
 * Majorly inspired by
 * https://github.com/JetBrains/intellij-community/tree/8b6e5ebc0cfccaad14323e47337ddac47c8347aa/plugins/git4idea/src/git4idea
 * 
 * @author Jan Esser <jesser@gmx.de>
 */
public class GitExecutor {

	public static enum PullMode {
		FF_ONLY, REBASE_MERGE
	}

	private static final String[] from(String command, String... commandArgs) {
		List<String> commandStrings = new ArrayList<String>(1 + (commandArgs != null ? commandArgs.length : 0));
		commandStrings.add(command);

		for (String commandArg : commandArgs)
			if (commandArg != null && commandArg != "")
				commandStrings.add(commandArg);

		return commandStrings.toArray(new String[0]);
	}

	private final File gitPath;
	private final File gitRepo;

	public GitExecutor(File localRepoFolder) throws GitExecutionException {
		this(new File("git"), localRepoFolder);
	}

	public GitExecutor(File gitPath, File gitRepo) throws GitExecutionException {
		this.gitPath = gitPath;
		this.gitRepo = gitRepo;

		version(); // assure git is operational
	}

	private GitExecutionResult gitExec(File gitPath, File workingDir, String... gitCommands)
			throws IOException, InterruptedException {
		String[] commandStrings = from(gitPath.getPath(), gitCommands);

		ProcessBuilder pb = new ProcessBuilder(commandStrings);
		pb.redirectErrorStream(true);
		pb.directory(workingDir);
		Process p = pb.start();
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		// TODO add supplier -> consumer her to also process structured/multi-line
		String line = br.readLine();
		return new GitExecutionResult(p.waitFor(), line);
	}

	private GitExecutionResult gitExec(String gitCommand, String... gitCommandArgs) throws GitExecutionException {
		try {
			return gitExec(gitPath, gitRepo, from(gitCommand, gitCommandArgs));
		} catch (IOException | InterruptedException ex) {
			throw new GitExecutionException(ex);
		}
	}

	public GitExecutionResult version() throws GitExecutionException {
		return gitExec("--version");
	}

	public GitExecutionResult fetch() throws GitExecutionException {
		return gitExec("fetch");
	}

	public GitExecutionResult pull() throws GitExecutionException {
		return pull(PullMode.FF_ONLY);
	}

	public GitExecutionResult pull(PullMode mode) throws GitExecutionException {
		String pullArg;
		switch (mode) {
		case FF_ONLY:
			pullArg = "--ff-only";
			break;
		case REBASE_MERGE:
			pullArg = "--rebase=merges";
			break;
		default:
			throw new RuntimeException("NOT IMPLEMENTED");
		}

		return gitExec("pull", pullArg);
	}

	public GitExecutionResult reset(boolean hard, String commitId) throws GitExecutionException {
		return gitExec("reset", hard ? "--hard" : "", commitId);
	}

	public GitExecutionResult commit(String message) throws GitExecutionException {
		return commit(message, false, false, false);
	}

	public GitExecutionResult commit(String message, boolean amend, boolean allowEmpty, boolean commitAll)
			throws GitExecutionException {
		if (commitAll)
			this.addAll();
		
		return gitExec("commit", //
				"-m", message, //
				amend ? "--amend" : "", //
				allowEmpty ? "--allow-empty" : "" //
		);
	}

	public GitExecutionResult rebase(String onto) throws GitExecutionException {
		return gitExec("rebase", "--onto", onto);
	}

	public GitExecutionResult rebaseAbort() throws GitExecutionException {
		return gitExec("rebase", "--abort");
	}

	public boolean hasChanges() throws GitExecutionException {
		// https://stackoverflow.com/a/3879077
		GitExecutionResult refreshIndexResult = gitExec("update-index", "--refresh");
		if (refreshIndexResult.exitCode() != 0)
			return true;

		GitExecutionResult diffIndexResult = gitExec("diff-index", "--quiet", "HEAD");
		if (diffIndexResult.exitCode() != 0)
			return true;

		return false; // otherwise
	}

	public GitExecutionResult addAll() throws GitExecutionException {
		return gitExec("add", "-A", ":/");
	}
	
	public GitExecutionResult add(File f) throws GitExecutionException {
		return gitExec("add", "-A", this.gitRepo.toPath().relativize(f.toPath()).toString());
	}

	public GitExecutionResult clean() throws GitExecutionException {
		return gitExec("clean", "-fd", ":/");
	}
}