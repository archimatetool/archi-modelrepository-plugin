package org.archicontribs.modelrepository.grafico;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

public class GitExecutorTest {

	private static final Logger LOGGER = Logger.getLogger(GitExecutorTest.class.getName());

	private GitExecutor underTest;
	
	public GitExecutorTest() throws GitExecutionException {
		underTest = new GitExecutor(TestData.GIT_PATH, TestData.GIT_REPO);
	}

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
	public void canGitReset() throws GitExecutionException {
		GitExecutionResult res = underTest.reset(true, TestData.GIT_HISTORICAL_COMMIT_ID);
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}
	
	@Test
	public void canGitClean() throws GitExecutionException, IOException {
		File f = new File(TestData.GIT_FOLDER, "cleanMe");
		assertTrue(f.createNewFile());
		assertTrue(f.exists());

		GitExecutionResult res = underTest.clean();
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());		
		
		assertFalse(f.exists());
	}
	
	private void resetTestScenario() {
		try {
			canGitReset();
			canGitClean();
		} catch (GitExecutionException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void canGitFetch() throws GitExecutionException {
		GitExecutionResult res = underTest.fetch();
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	
	@Test
	public void canGitCommit() throws GitExecutionException {
		resetTestScenario(); // arrange
		
		GitExecutionResult res = underTest.commit("empty commit", false, true, false);
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitPull() throws GitExecutionException {
		resetTestScenario(); // arrange

		GitExecutionResult res = underTest.pull(); // FF_ONLY
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitPullDetectConflict() throws GitExecutionException {
		resetTestScenario(); // arrange

		GitExecutionResult resCommit = underTest.commit("empty commit", false, true, false);
		LOGGER.finest(resCommit.outputLine());

		GitExecutionResult res = underTest.pull();
		LOGGER.finest(res.outputLine());
		assertEquals(128, res.exitCode());
	}

	@Test
	public void canGitPullRebase() throws GitExecutionException {
		resetTestScenario(); // arrange

		GitExecutionResult resCommit = underTest.commit("empty commit", false, true, false);
		LOGGER.finest(resCommit.outputLine());

		GitExecutionResult res = underTest.pull(GitExecutor.PullMode.REBASE_MERGE);
		LOGGER.finest(res.outputLine());
		assertEquals(0, res.exitCode());
	}

	@Test
	public void canGitRebaseAbort() throws GitExecutionException, IOException {
		resetTestScenario(); // arrange

		File targetF = Paths.get(TestData.GIT_FOLDER.getPath(), TestData.GIT_FILE.getName()).toFile();
		Files.move(TestData.GIT_FILE.toPath(), targetF.toPath(),
				StandardCopyOption.REPLACE_EXISTING);

		GitExecutionResult resAdd = underTest.add(TestData.GIT_FILE);
		assertEquals(0, resAdd.exitCode());
		
		GitExecutionResult resCommit = underTest.commit("file deleted accidentaly oopsy");
		LOGGER.finest(resCommit.outputLine());

		GitExecutionResult res = underTest.rebase(TestData.GIT_HISTORICAL_ONTO_COMMIT_ID);
		LOGGER.finest(res.outputLine());
		assertEquals(1, res.exitCode());

		GitExecutionResult abortResult = underTest.rebaseAbort();
		assertEquals(0, abortResult.exitCode());
	}
	
	@Test
	public void canDetectUncommittedFile() throws GitExecutionException, IOException {
		resetTestScenario(); // arrange

		assertFalse(underTest.hasChanges());
		
		Files.move(TestData.GIT_FILE.toPath(), Paths.get(TestData.GIT_FOLDER.getPath(), TestData.GIT_FILE.getName()),
				StandardCopyOption.REPLACE_EXISTING);
		
		assertTrue(underTest.hasChanges());
	}

	@Test
	public void canStageAllFiles() throws GitExecutionException, IOException {
		resetTestScenario(); // arrange
		
		Files.move(TestData.GIT_FILE.toPath(), Paths.get(TestData.GIT_FOLDER.getPath(), TestData.GIT_FILE.getName()),
				StandardCopyOption.REPLACE_EXISTING);
		
		assertEquals(0, underTest.addAll().exitCode());
	}

	@Test
	public void canStageFile() throws GitExecutionException, IOException {
		resetTestScenario(); // arrange
		
		File targetF = Paths.get(TestData.GIT_FOLDER.getPath(), TestData.GIT_FILE.getName()).toFile();
		assertTrue(targetF.createNewFile());
		
		assertEquals(0, underTest.add(targetF).exitCode());
		
		File nonExistentF = Paths.get(TestData.GIT_FOLDER.getPath(), "nonExistentFile").toFile();
		assertFalse(nonExistentF.exists());
		
		assertNotEquals(0, underTest.add(nonExistentF).exitCode());
	}	
	
	@Test
	public void canRemoteSshCredentials() {
		// TODO
	}
}
