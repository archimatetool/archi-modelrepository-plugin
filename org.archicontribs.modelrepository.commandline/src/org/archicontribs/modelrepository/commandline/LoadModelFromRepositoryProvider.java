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
import org.archicontribs.modelrepository.authentication.CryptoUtils;
import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator;
import org.archicontribs.modelrepository.authentication.CredentialsAuthenticator.SSHIdentityProvider;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.commandline.AbstractCommandLineProvider;
import com.archimatetool.commandline.CommandLineState;
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
 * 
 * This will clone an online Archi model repository into clonefolder.
 * 
 * @author Phillip Beauvoir
 */
public class LoadModelFromRepositoryProvider extends AbstractCommandLineProvider {

    static final String PREFIX = Messages.LoadModelFromRepositoryProvider_0;
    
    static final String OPTION_CLONE_MODEL = "modelrepository.cloneModel"; //$NON-NLS-1$
    static final String OPTION_LOAD_MODEL = "modelrepository.loadModel"; //$NON-NLS-1$
    static final String OPTION_USERNAME = "modelrepository.userName"; //$NON-NLS-1$
    static final String OPTION_PASSFILE = "modelrepository.passFile"; //$NON-NLS-1$
    static final String OPTION_SSH_IDENTITY_FILE = "modelrepository.identityFile"; //$NON-NLS-1$
    
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
            char[] password = getPasswordFromFile(commandLine);
            File identityFile = getSSHIdentityFile(commandLine);
            
            boolean isSSH = GraficoUtils.isSSH(url);
            boolean isHTTP = !isSSH;
            
            if(!StringUtils.isSet(url)) {
                logError(Messages.LoadModelFromRepositoryProvider_2);
                return;
            }
            
            // HTTP requires user name
            if(isHTTP && !StringUtils.isSet(username)) {
                logError(Messages.LoadModelFromRepositoryProvider_3);
                return;
            }
            
            // If using HTTP then password is needed for connection
            // If using SSH then password is optional for the identity file
            if(isHTTP && (password == null || password.length == 0)) {
                logError(Messages.LoadModelFromRepositoryProvider_17);
                return;
            }
            
            // SSH needs identity file
            if(isSSH && identityFile == null) {
                logError(Messages.LoadModelFromRepositoryProvider_18);
                return;
            }
            
            logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_4, url, cloneFolder));
            
            // Delete clone folder
            FileUtils.deleteFolder(cloneFolder);
            
            IArchiRepository repo = new ArchiRepository(cloneFolder);

            // SSH
            if(isSSH) {
                // Set this to return our details rather than using the defaults from App prefs
                CredentialsAuthenticator.setSSHIdentityProvider(new SSHIdentityProvider() {
                    @Override
                    public File getIdentityFile() {
                        return identityFile;
                    }

                    @Override
                    public char[] getIdentityPassword() {
                        return password;
                    }
                });
                
                repo.cloneModel(url, null, null);
            }
            // HTTP
            else {
                UsernamePassword npw = new UsernamePassword(username, password);
                try {
                    repo.cloneModel(url, npw, null);
                }
                finally {
                    npw.clear(); // Clear this
                }
            }
            
            logMessage(Messages.LoadModelFromRepositoryProvider_5);
        }
        
        // Load
        logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_6, cloneFolder));
        IArchimateModel model = loadModel(cloneFolder);
        logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_7, model.getName()));
    }
    
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

    private char[] getPasswordFromFile(CommandLine commandLine) throws IOException {
        char[] password = null;
        
        String path = commandLine.getOptionValue(OPTION_PASSFILE);
        if(StringUtils.isSet(path)) {
            File file = new File(path);
            if(file.exists() && file.canRead()) {
                byte[] bytes = Files.readAllBytes(Paths.get(file.getPath()));
                return CryptoUtils.convertBytesToChars(bytes);
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
