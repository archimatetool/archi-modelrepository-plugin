/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.commandline;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator;
import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator.SSHIdentityProvider;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.BranchInfo;
import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IGraficoConstants;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.commandline.AbstractCommandLineProvider;
import com.archimatetool.commandline.CommandLineState;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;

/**
 * Command Line interface for loading a repository model and cloning an Archi model from online Repository
 * 
 * Usage - (should be all on one line):
 * 
 * Archi -consoleLog -nosplash -application com.archimatetool.commandline.app
   --modelrepository.cloneModel "url"
   --modelrepository.loadModel "cloneFolder"
   --modelrepository.userName "userName"
   --modelrepository.passFile "/pathtoPasswordFile"
   --modelrepository.identityFile "/pathtoIdentityFile"
   --modelrepository.overwriteModelFile "true/false"
 * 
 * This will clone an online Archi model repository into clonefolder.
 * 
 * @author Phillip Beauvoir
 */
public class LoadModelFromRepositoryProvider extends AbstractCommandLineProvider {

    static final String PREFIX = Messages.LoadModelFromRepositoryProvider_0;
    
    static final String OPTION_CLONE_MODEL = "modelrepository.cloneModel"; //$NON-NLS-1$
    static final String OPTION_LOAD_MODEL = "modelrepository.loadModel"; //$NON-NLS-1$
    static final String OPTION_CHANGE_BRANCH = "modelrepository.changeBranch"; //$NON-NLS-1$
    static final String OPTION_USERNAME = "modelrepository.userName"; //$NON-NLS-1$
    static final String OPTION_PASSFILE = "modelrepository.passFile"; //$NON-NLS-1$
    static final String OPTION_SSH_IDENTITY_FILE = "modelrepository.identityFile"; //$NON-NLS-1$
    static final String OPTION_OVERWRITE_MODEL_FILE = "modelrepository.overwriteModelFile"; //$NON-NLS-1$
    
    public LoadModelFromRepositoryProvider() {
    }
    
    @Override
    public void run(CommandLine commandLine) throws Exception {
        if(!hasCorrectOptions(commandLine)) {
            return;
        }
        
        // loadModel folder must be set in both cases:
        // 1. Just load an existing local repository grafico model
        // 2. Clone online repository grafico model to folder specified in (1)
        
        String sFolder = commandLine.getOptionValue(OPTION_LOAD_MODEL);
        if(!StringUtils.isSet(sFolder)) {
            logError(NLS.bind(Messages.LoadModelFromRepositoryProvider_1, OPTION_LOAD_MODEL));
            return;
        }
        
        File cloneFolder = new File(sFolder);

        // Clone
        if(commandLine.hasOption(OPTION_CLONE_MODEL)) {
            String url = commandLine.getOptionValue(OPTION_CLONE_MODEL);
            String username = commandLine.getOptionValue(OPTION_USERNAME);
            String password = getPasswordFromFile(commandLine);
            File identityFile = getSSHIdentityFile(commandLine);
            
            boolean isSSH = GraficoUtils.isSSH(url);
            
            if(!StringUtils.isSet(url)) {
                logError(Messages.LoadModelFromRepositoryProvider_2);
                return;
            }
            
            if(!isSSH && !StringUtils.isSet(username)) {
                logError(Messages.LoadModelFromRepositoryProvider_3);
                return;
            }
            
            if(!isSSH && !StringUtils.isSet(password)) {
                logError(Messages.LoadModelFromRepositoryProvider_17);
                return;
            }
            
            if(isSSH && identityFile == null) {
                logError(Messages.LoadModelFromRepositoryProvider_18);
                return;
            }
            
            logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_4, url, cloneFolder));
            
            FileUtils.deleteFolder(cloneFolder);
            
            // Set this to return our details rather than using the defaults from App prefs
            CredentialsAuthenticator.setSSHIdentityProvider(new SSHIdentityProvider() {
                @Override
                public File getIdentityFile() {
                    return identityFile;
                }

                @Override
                public char[] getIdentityPassword() {
                    return password.toCharArray();
                }
            });
            
            IArchiRepository repo = new ArchiRepository(cloneFolder);
            repo.cloneModel(url, new UsernamePassword(username, password.toCharArray()), null);
            
            logMessage(Messages.LoadModelFromRepositoryProvider_5);
        }

        // Change branch
        if(commandLine.hasOption(OPTION_CHANGE_BRANCH)) {
            String strTargetBranch = commandLine.getOptionValue(OPTION_CHANGE_BRANCH);
            
            IArchiRepository repo = new ArchiRepository(cloneFolder);
            BranchInfo info = repo.getBranchStatus().getCurrentLocalBranch();
            		
            // If the current branch in the repo is not the branch specified in options, change
            if (info.getShortName()!=strTargetBranch) {
                try(Repository gitRepo = Git.open(repo.getLocalRepositoryFolder()).getRepository()) {
                	// Get a BranchInfo for the (short) branch name provided in the options
                	BranchInfo branch = null;
                	Iterator<BranchInfo> ib = repo.getBranchStatus().getAllBranches().iterator();
                    while (ib.hasNext()){
                    	BranchInfo b = ib.next();
                    	// TODO: Check with Phil/JB whether this is a good test... and also whether
                    	//		 the branch name provided in options rather be a full name?
                    	// Get the branch that matches short name, prefer local ref if it exists, but otherwise
                    	// get the remote ref
                    	if (b.getShortName().equals(strTargetBranch)) { 
                    		if (branch==null) {
                    			// If the branch hasn't yet been set, use this one
                        		branch = b;
                    		} else if (b.isLocal()) {
                    			// If the branch has been set, prefer this one only if it is local
                        		branch = b;
                    		}
                    	}
                    }
	            	
	            	// If we found BranchInfo for the branch, switch
	            	if (branch!=null) {
	            		switchBranch(repo, branch, false);
	            		logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_24, branch.getShortName()));
	            	} else {
	            		logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_25, strTargetBranch));	            		
	            	}
	            }
            }
        }
        
        // Load
    	boolean overwriteModelFile = true;
        // Deal with file overwrite option
        if(commandLine.hasOption(OPTION_OVERWRITE_MODEL_FILE)) {
            String strOverwriteModelFile = commandLine.getOptionValue(OPTION_OVERWRITE_MODEL_FILE);
            if (!strOverwriteModelFile.equals("true")) {
            	overwriteModelFile = false;
            }
        }
        logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_6, cloneFolder));
        IArchimateModel model = loadModel(cloneFolder, overwriteModelFile);
        logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_7, model.getName()));
    }
    
    private IArchimateModel loadModel(File folder, boolean overwriteModelFile) throws IOException {

    	IArchimateModel model = null;
    	if (!overwriteModelFile) {
	    	IArchiRepository repo = new ArchiRepository(folder);
	        File fileName = repo.getTempModelFile();
	        model = IEditorModelManager.INSTANCE.loadModel(fileName);
    	}
        if (model==null) {
	        
	        GraficoModelImporter importer = new GraficoModelImporter(folder);
	        model = importer.importAsModel();
	        
	        if(model == null) {
	            throw new IOException(NLS.bind(Messages.LoadModelFromRepositoryProvider_21, folder));
	        }
	        
	        if(importer.getUnresolvedObjects() != null) {
	            throw new IOException(Messages.LoadModelFromRepositoryProvider_8);
	        }
	        
	        // Set the model filename
	        // TODO: Check with Phil if this shouldn't be extracted to GraficoUtils somehow
	        File saveFile = new File(folder, "/.git/" + IGraficoConstants.LOCAL_ARCHI_FILENAME);
	        model.setFile(saveFile);
	        IEditorModelManager.INSTANCE.saveModel(model);
        }
        
        CommandLineState.setModel(model);
        
        return model;
    }
    
    protected void switchBranch(IArchiRepository repo, BranchInfo branchInfo, boolean doReloadGrafico) throws IOException, GitAPIException {
        try(Git git = Git.open(repo.getLocalRepositoryFolder())) {
            // If the branch is local just checkout
            if(branchInfo.isLocal()) {
                git.checkout().setName(branchInfo.getFullName()).call();
            }
            // If the branch is remote and has no local ref we need to create the local branch and switch to that
            else if(branchInfo.isRemote() && !branchInfo.hasLocalRef()) {
                String branchName = branchInfo.getShortName();
                
                // Create local branch at point of remote branch ref
                Ref ref = git.branchCreate()
                        .setName(branchName)
                        .setStartPoint(branchInfo.getFullName())
                        .call();
                
                // checkout
                git.checkout().setName(ref.getName()).call();
            }
            
            // Reload the model from the Grafico XML files
            if(doReloadGrafico) {
                new GraficoModelLoader(repo).loadModel();
                
                // Save the checksum
                repo.saveChecksum();
            }
        }
    }

    private String getPasswordFromFile(CommandLine commandLine) throws IOException {
        String password = null;
        
        String path = commandLine.getOptionValue(OPTION_PASSFILE);
        if(StringUtils.isSet(path)) {
            File file = new File(path);
            if(file.exists() && file.canRead()) {
                password = new String(Files.readAllBytes(Paths.get(file.getPath())));
                password = password.trim();
            }
        }

        return password;
    }
            
    private File getSSHIdentityFile(CommandLine commandLine) {
        String path = commandLine.getOptionValue(OPTION_SSH_IDENTITY_FILE);
        if(StringUtils.isSet(path)) {
            File file = new File(path);
            if(file.exists() && file.canRead()) {
                return file;
            }
        }
        
        return null;
    }
    
    @Override
    public Options getOptions() {
        Options options = new Options();
        
        Option option = Option.builder()
                .longOpt(OPTION_LOAD_MODEL)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_9)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_10, OPTION_CLONE_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_CLONE_MODEL)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_11)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_12, OPTION_LOAD_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_CHANGE_BRANCH)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_22)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_23, OPTION_LOAD_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_USERNAME)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_13)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_14, OPTION_CLONE_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_PASSFILE)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_15)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_16, OPTION_CLONE_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_SSH_IDENTITY_FILE)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_19)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_20, OPTION_CLONE_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_OVERWRITE_MODEL_FILE)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_26)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_27, OPTION_CLONE_MODEL))
                .build();
        options.addOption(option);

        return options;
    }
    
    private boolean hasCorrectOptions(CommandLine commandLine) {
        return commandLine.hasOption(OPTION_CLONE_MODEL) || commandLine.hasOption(OPTION_LOAD_MODEL);
    }
    
    @Override
    public int getPriority() {
        return PRIORITY_LOAD_OR_CREATE_MODEL;
    }
    
    @Override
    protected String getLogPrefix() {
        return PREFIX;
    }
}
