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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator;
import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator.SSHIdentityProvider;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.process.IRepositoryProcessListener;
import org.archicontribs.modelrepository.process.RepositoryModelProcess;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.osgi.util.NLS;
import com.archimatetool.commandline.AbstractCommandLineProvider;
import com.archimatetool.commandline.CommandLineState;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.ModelChecker;
import com.archimatetool.editor.preferences.IPreferenceConstants;
import com.archimatetool.editor.preferences.Preferences;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.ModelVersion;

/**
 * Command Line interface for updating an online Repository model from Archi
 * 
 * Usage - (should be all on one line):
 * 
 * Archi -consoleLog -nosplash -application com.archimatetool.commandline.app
   --updatemodelrepository.commitModel "cloneFolder"
   --updatemodelrepository.publishModel "true/false"
   --updatemodelrepository.commitMessage "commit message"
   --updatemodelrepository.userName "userName"
   --updatemodelrepository.passFile "/pathtoPasswordFile"
   --updatemodelrepository.identityFile "/pathtoIdentityFile"
 * 
 * This will update an online Archi model repository from the loaded model.
 * 
 * @author Michael Ansley
 */
public class UpdateModelToRepositoryProvider extends AbstractCommandLineProvider implements IRepositoryProcessListener {

    static final String PREFIX = Messages.UpdateModelToRepositoryProvider_0;
    
    static final String OPTION_COMMIT_MODEL = "updatemodelrepository.commitModel"; //$NON-NLS-1$
    static final String OPTION_PUBLISH_MODEL = "updatemodelrepository.publishModel"; //$NON-NLS-1$
    static final String OPTION_REFRESH_MODEL = "updatemodelrepository.refreshModel"; //$NON-NLS-1$
    static final String OPTION_USERNAME = "updatemodelrepository.userName"; //$NON-NLS-1$
    static final String OPTION_PASSFILE = "updatemodelrepository.passFile"; //$NON-NLS-1$
    static final String OPTION_SSH_IDENTITY_FILE = "updatemodelrepository.identityFile"; //$NON-NLS-1$

    protected static final int PULL_STATUS_ERROR = -1;
    protected static final int PULL_STATUS_OK = 0;
    protected static final int PULL_STATUS_UP_TO_DATE = 1;
    protected static final int PULL_STATUS_MERGE_CANCEL = 2;
    
    protected static final int USER_OK = 0;
    protected static final int USER_CANCEL = 1;
    
    private RepositoryModelProcess fActionHandler;

    public UpdateModelToRepositoryProvider() {
    }
    
    @Override
    public void run(CommandLine commandLine) throws Exception {
    	if (!hasAnyOptions(commandLine)) {
    		logMessage(NLS.bind(Messages.UpdateModelToRepositoryProvider_20, Messages.UpdateModelToRepositoryProvider_0));
    		return;
    	}
    	// Check command line options
    	// The minimum requirement is for the commit option plus a commit message
    	if(!hasCorrectOptions(commandLine)) {
        	logError(Messages.UpdateModelToRepositoryProvider_12);
            return;
        }

        // Check that the command line processor has a model loaded
        IArchimateModel model = CommandLineState.getModel();
        if(model == null) {
            throw new IOException(Messages.UpdateModelToRepositoryProvider_10);
        }

        // Check that the loaded model has a save file
        if(model.getFile() == null) {
            throw new IOException(Messages.UpdateModelToRepositoryProvider_19);
        }
        
        // Check that the loaded model is associated with repository
        if (!GraficoUtils.isModelInLocalRepository(model)) {
        	logError(Messages.UpdateModelToRepositoryProvider_18);
            return;
        }
        IArchiRepository repo = new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model));        
        
        // Save the model, just in case it hasn't been saved yet
        saveModel(model);
        
        boolean publish = StringUtils.safeString(commandLine.getOptionValue(OPTION_PUBLISH_MODEL)).equals("true");
        boolean refresh = StringUtils.safeString(commandLine.getOptionValue(OPTION_REFRESH_MODEL)).equals("true");
        String sCommitMessage = commandLine.getOptionValue(OPTION_COMMIT_MODEL);
        
        if (!publish && !refresh) {
        	fActionHandler = new RepositoryModelProcess(RepositoryModelProcess.PROCESS_COMMIT, model, this, null, null, sCommitMessage, false);
        } else {
        	
        	String url = repo.getOnlineRepositoryURL();
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

            // UsernamePassword is will be null if using SSH
            UsernamePassword npw = new UsernamePassword(username, password.toCharArray());

            if (refresh && !publish) {
            	fActionHandler = new RepositoryModelProcess(RepositoryModelProcess.PROCESS_REFRESH, model, this, null, npw, "", false);
            } else if (publish) {
            	fActionHandler = new RepositoryModelProcess(RepositoryModelProcess.PROCESS_PUBLISH, model, this, null, npw, sCommitMessage, false);
            } else {
            	logError(Messages.UpdateModelToRepositoryProvider_12);
            }
        }
        fActionHandler.run();
        
        logMessage(Messages.UpdateModelToRepositoryProvider_5);
    }

/*
    private IArchimateModel loadModel(File folder) throws IOException {
        GraficoModelImporter importer = new GraficoModelImporter(folder);
        IArchimateModel model = importer.importAsModel();
        
        if(model == null) {
            throw new IOException(NLS.bind(Messages.LoadModelFromRepositoryProvider_21, folder));
        }
        
        if(importer.getUnresolvedObjects() != null) {
            throw new IOException(Messages.LoadModelFromRepositoryProvider_8);
        }
        
        CommandLineState.setModel(model);
        
        return model;
    }
*/    
    
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
                .longOpt(OPTION_COMMIT_MODEL)
                .hasArg()
                .argName(Messages.UpdateModelToRepositoryProvider_1)
                .desc(NLS.bind(Messages.UpdateModelToRepositoryProvider_2, OPTION_COMMIT_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_PUBLISH_MODEL)
                .hasArg()
                .argName(Messages.UpdateModelToRepositoryProvider_3)
                .desc(NLS.bind(Messages.UpdateModelToRepositoryProvider_4, OPTION_PUBLISH_MODEL))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_USERNAME)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_13)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_14, OPTION_USERNAME))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_PASSFILE)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_15)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_16, OPTION_PASSFILE))
                .build();
        options.addOption(option);
        
        option = Option.builder()
                .longOpt(OPTION_SSH_IDENTITY_FILE)
                .hasArg()
                .argName(Messages.LoadModelFromRepositoryProvider_19)
                .desc(NLS.bind(Messages.LoadModelFromRepositoryProvider_20, OPTION_SSH_IDENTITY_FILE))
                .build();
        options.addOption(option);

        
        return options;
    }

    private boolean hasAnyOptions(CommandLine commandLine) {
        return (commandLine.hasOption(OPTION_COMMIT_MODEL) || 
        		commandLine.hasOption(OPTION_REFRESH_MODEL) ||
        		commandLine.hasOption(OPTION_PUBLISH_MODEL) ||
        		commandLine.hasOption(OPTION_USERNAME) ||
        		commandLine.hasOption(OPTION_PASSFILE) ||
        		commandLine.hasOption(OPTION_SSH_IDENTITY_FILE));
    }

    private boolean hasCorrectOptions(CommandLine commandLine) {
        return (commandLine.hasOption(OPTION_COMMIT_MODEL) || (StringUtils.safeString(commandLine.getOptionValue(OPTION_COMMIT_MODEL)).length() > 0));
    }
    
    @Override
    public int getPriority() {
        return PRIORITY_SAVE_MODEL;
    }
    
    @Override
    protected String getLogPrefix() {
        return PREFIX;
    }

    public boolean saveModel(IArchimateModel model) throws IOException {
    	
    	// Check integrity
        ModelChecker checker = new ModelChecker(model);
        if(!checker.checkAll()) {
        	logError(Messages.UpdateModelToRepositoryProvider_17);
        	return false;
        }
        
        File file = model.getFile();
        
        // Save backup (if set in Preferences)
        if(Preferences.STORE.getBoolean(IPreferenceConstants.BACKUP_ON_SAVE) && file.exists()) {
            FileUtils.copyFile(file, new File(model.getFile().getAbsolutePath() + ".bak"), false); //$NON-NLS-1$
        }
        
        // Set model version
        model.setVersion(ModelVersion.VERSION);
        
        // Use Archive Manager to save contents
        IArchiveManager archiveManager = (IArchiveManager)model.getAdapter(IArchiveManager.class);
        archiveManager.saveModel();
        
        // Set CommandStack Save point
        CommandStack stack = (CommandStack)model.getAdapter(CommandStack.class);
        if(stack != null) {
            stack.markSaveLocation();
        }
        
        return true;
    }
    
    public void actionSimpleEvent(String eventType, String object, String summary, String detail) {
    	logMessage(eventType + " from " + object + ": " + summary + " - " + detail);
    }
    
    public boolean actionComplexEvent(String eventType, String object, RepositoryModelProcess actionHandler) {
    	// For now, just continue; needs handlers when refresh/publish is coded
    	return true;
    }
    
    public RepositoryModelProcess getActionHandler() {
    	return fActionHandler;
    }
}
