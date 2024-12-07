package org.archicontribs.modelrepository.grafico;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

public class GitExecutorTest {

	private static final Logger LOGGER = Logger.getLogger(GitExecutorTest.class.getName());

	private GitExecutor underTest = new GitExecutor(TestData.GIT_PATH, TestData.GIT_REPO);

	@Test
	public void canFindGitOnPath() throws IOException, InterruptedException {
		Process process = Runtime.getRuntime().exec(new String[] { "which", "git" });
		assertEquals(0, process.waitFor());
	}

	@Test
	public void canGitVersion() throws GitExecutionException {
		GitExecutionResult res = underTest.version();
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitFetch() throws GitExecutionException {
		GitExecutionResult res = underTest.fetch();
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitReset() throws GitExecutionException {
		GitExecutionResult res = underTest.reset(true, TestData.GIT_HISTORICAL_COMMIT_ID);
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitCommit() throws GitExecutionException {
		GitExecutionResult res = underTest.commit("empty commit", false, true, false);
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitPull() throws GitExecutionException {
		canGitReset(); // arrange

		GitExecutionResult res = underTest.pull(); // FF_ONLY
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitPullDetectConflict() throws GitExecutionException {
		canGitReset(); // arrange

		GitExecutionResult resCommit = underTest.commit("empty commit", false, true, false);
		LOGGER.finest(resCommit.outputLine());

		GitExecutionResult res = underTest.pull();
		LOGGER.finest(res.outputLine());
		assertEquals(128, res.exitCode());
	}

	@Test
	public void canGitPullRebase() throws GitExecutionException {
		canGitReset(); // arrange

		GitExecutionResult resCommit = underTest.commit("empty commit", false, true, false);
		LOGGER.finest(resCommit.outputLine());

		GitExecutionResult res = underTest.pull(GitExecutor.PullMode.REBASE_MERGE);
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitRebaseAbort() throws GitExecutionException, IOException {
		canGitReset(); // arrange

		Files.move(TestData.GIT_FILE.toPath(), Paths.get(TestData.GIT_FOLDER.getPath(), TestData.GIT_FILE.getName()),
				StandardCopyOption.REPLACE_EXISTING);

		GitExecutionResult resCommit = underTest.commit("file moved accidentaly oopsy", false, true, true);
		LOGGER.finest(resCommit.outputLine());

		GitExecutionResult res = underTest.rebase(TestData.GIT_HISTORICAL_ONTO_COMMIT_ID);
		LOGGER.finest(res.outputLine());
		assertEquals(1, res.exitCode());

		GitExecutionResult abortResult = underTest.rebaseAbort();
		assertEquals(0, abortResult.exitCode());
	}

}
