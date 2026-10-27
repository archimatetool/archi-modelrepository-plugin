/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.commandline;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoModelExporter;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.osgi.util.NLS;

import com.archimatetool.commandline.AbstractCommandLineProvider;
import com.archimatetool.commandline.CommandLineState;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.ModelVersion;

/**
 * Command Line interface for saving the current active model to a repository model
 * This means another action has to be taken first to have an active model by loading it or populating via a script.
 * 
 * Usage - (should be all on one line):
 * 
 * Archi -consoleLog -nosplash -application com.archimatetool.commandline.app
   --modelrepository.saveModel "cloneFolder"
 * 
 * This will save the active archimatemodel into a local Archi model repository to enable changes in the model to be committed.
 * Note: The commit itself is not done as part of this action.
 * 
 * @author Raimond Brookman
 */
public class SaveModelToRepositoryProvider extends AbstractCommandLineProvider {

    static final String PREFIX = Messages.SaveModelToRepositoryProvider_0;
    
    static final String OPTION_SAVE_MODEL = "modelrepository.saveModel"; //$NON-NLS-1$
       
    public SaveModelToRepositoryProvider() {
	}

	@Override
    public void run(CommandLine commandLine) throws Exception {
        if(!hasCorrectOptions(commandLine)) {
            return;
        }
               
        String sFolder = commandLine.getOptionValue(OPTION_SAVE_MODEL);
        if(!StringUtils.isSet(sFolder)) {
            logError(NLS.bind(Messages.SaveModelToRepositoryProvider_1, OPTION_SAVE_MODEL));
            return;
        }
        
        File cloneFolder = new File(sFolder);

    	IArchimateModel model = CommandLineState.getModel();
    	
        // Save
        logMessage(NLS.bind(Messages.SaveModelToRepositoryProvider_2, model.getName()));
        saveModel(model, cloneFolder);
        logMessage(NLS.bind(Messages.SaveModelToRepositoryProvider_3, cloneFolder));
        
    }
    
    private void saveModel(IArchimateModel model, File folder) throws IOException {
        // This command will only write the changes to the repo, commit must be done as a normal Git action

    	try
    	{ 
            GraficoModelExporter exporter = new GraficoModelExporter(model, folder);
            exporter.exportModel();
    		
            logMessage(NLS.bind(Messages.SaveModelToRepositoryProvider_4, folder));
        }
        catch(Exception ex) {
        	logError(NLS.bind(Messages.SaveModelToRepositoryProvider_5, ex));
            return;
        }
    }
       
    @Override
    public Options getOptions() {
        Options options = new Options();
        
        Option option = Option.builder()
                .longOpt(OPTION_SAVE_MODEL)
                .hasArg()
                .argName(Messages.SaveModelToRepositoryProvider_6)
                .desc(NLS.bind(Messages.SaveModelToRepositoryProvider_7, OPTION_SAVE_MODEL))
                .build();
        options.addOption(option);
        
        return options;
    }
    
    private boolean hasCorrectOptions(CommandLine commandLine) {
        return commandLine.hasOption(OPTION_SAVE_MODEL);
    }
    
    @Override
    public int getPriority() {
        return PRIORITY_SAVE_MODEL;
    }
    
    @Override
    protected String getLogPrefix() {
        return PREFIX;
    }
}
