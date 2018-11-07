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
import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.commandline.AbstractCommandLineProvider;
import com.archimatetool.commandline.CommandLineState;
import com.archimatetool.editor.model.IArchiveManager;
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
            String userName = commandLine.getOptionValue(OPTION_USERNAME);
            String password = getPasswordFromFile(commandLine);
            
            if(!StringUtils.isSet(url)) {
                logError(Messages.LoadModelFromRepositoryProvider_2);
                return;
            }
            
            if(!StringUtils.isSet(userName)) {
                logError(Messages.LoadModelFromRepositoryProvider_3);
                return;
            }
            
            if(!StringUtils.isSet(password)) {
                logError(Messages.LoadModelFromRepositoryProvider_17);
                return;
            }

            logMessage(NLS.bind(Messages.LoadModelFromRepositoryProvider_4, url, cloneFolder));
            cloneModel(url, cloneFolder, userName, password);
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
        
        if(importer.getUnresolvedObjects() != null) {
            throw new IOException(Messages.LoadModelFromRepositoryProvider_8);
        }
        
        // Add an Archive Manager and load images
        IArchiveManager archiveManager = IArchiveManager.FACTORY.createArchiveManager(model);
        model.setAdapter(IArchiveManager.class, archiveManager);
        archiveManager.loadImages();
        
        CommandLineState.setModel(model);
        
        return model;
    }

    private void cloneModel(String url, File cloneFolder, String userName, String password) throws GitAPIException, IOException {
        FileUtils.deleteFolder(cloneFolder);
        cloneFolder.mkdirs(); // Make dir
        
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setDirectory(cloneFolder);
        cloneCommand.setURI(url);
        cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, password));
            
        try(Git git = cloneCommand.call()) {
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
