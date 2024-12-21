package org.archicontribs.modelrepository.grafico;

import java.io.File;

interface TestData {

	final File GIT_REPO = new File("/home/jan/projs/egit");
	final File GIT_FILE = new File(GIT_REPO, "pom.xml");
	final File GIT_FOLDER = new File(GIT_REPO, "icons");
	final File GIT_PATH = new File("/usr/local/bin/git");
	final String GIT_HISTORICAL_COMMIT_ID = "e90d864edca6eb34d0b7a1f0dcc767bcd4970bb5";
	final String GIT_HISTORICAL_ONTO_COMMIT_ID = "cd8c66d521371cbd1163b136f991a9598055d84a";
	
}