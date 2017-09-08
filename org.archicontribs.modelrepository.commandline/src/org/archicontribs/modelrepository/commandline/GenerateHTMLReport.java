package org.archicontribs.modelrepository.commandline;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.archicontribs.modelrepository.grafico.GraficoModelImporter;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.archimatetool.commandline.ICommandLineProvider;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.utils.FileUtils;
import com.archimatetool.editor.utils.StringUtils;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.reports.html.HTMLReportExporter;

/**
 * Command Line interface for generating a HTML Report from Model Repository
 * 
 * @author Phillip Beauvoir
 */
@SuppressWarnings("nls")
public class GenerateHTMLReport implements ICommandLineProvider {

    public static final String HTMLREPORT = "[GenerateHTMLReport]"; //$NON-NLS-1$
    
    public static final String ARGS_PREFIX = "-GenerateHTMLReport_"; //$NON-NLS-1$
    public static final String ARGS_CLONEFOLDER = ARGS_PREFIX + "CloneFolder"; //$NON-NLS-1$
    public static final String ARGS_REPORTFOLDER = ARGS_PREFIX + "ReportFolder"; //$NON-NLS-1$
    public static final String ARGS_REPO_URL = ARGS_PREFIX + "URL"; //$NON-NLS-1$
    public static final String ARGS_REPO_USERNAME = ARGS_PREFIX + "UserName"; //$NON-NLS-1$
    public static final String ARGS_REPO_PASSWORD = ARGS_PREFIX + "Password"; //$NON-NLS-1$
    
    public GenerateHTMLReport() {
    }
    
    @Override
    public void run(String[] args) throws Exception {
        Map<String, String> argsMap = parseArgs(args);
        
        if(!hasCorrectArgs(argsMap)) {
            return;
        }
        
        File cloneFolder = null;
        File reportFolder = null;

        String s_cloneFolder = argsMap.get(ARGS_CLONEFOLDER);
        if(!StringUtils.isSet(s_cloneFolder)) {
            logError("No folder input.");
            return;
        }
        else {
            cloneFolder = new File(s_cloneFolder);
        }
        
        String s_reportFolder = argsMap.get(ARGS_REPORTFOLDER);
        if(!StringUtils.isSet(s_reportFolder)) {
            logError("No output folder.");
            return;
        }
        else {
            reportFolder = new File(s_reportFolder);
        }

        String url = argsMap.get(ARGS_REPO_URL);
        String userName = argsMap.get(ARGS_REPO_USERNAME);
        String password = argsMap.get(ARGS_REPO_PASSWORD);
        if(StringUtils.isSet(url) && StringUtils.isSet(userName)) {
            cloneModel(url, cloneFolder, userName, password);
        }
        
        IArchimateModel model = loadModel(cloneFolder);
        generateReport(model, reportFolder);
        
        logMessage("Report generated!");
    }
    
    private void generateReport(IArchimateModel model, File reportFolder) throws IOException {
        logMessage("Generating report to '" + reportFolder + "'");
        
        FileUtils.deleteFolder(reportFolder);

        HTMLReportExporter ex = new HTMLReportExporter(model);
        ex.createReport(reportFolder, "index.html"); //$NON-NLS-1$
    }

    private void cloneModel(String url, File cloneFolder, String userName, String password) throws GitAPIException, IOException {
        logMessage("Cloning from '" + url + "'");
        
        FileUtils.deleteFolder(cloneFolder);
        
        CloneCommand cloneCommand = Git.cloneRepository();
        cloneCommand.setDirectory(cloneFolder);
        cloneCommand.setURI(url);
        cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, password));
            
        try(Git git = cloneCommand.call()) {
        }
    }

    private IArchimateModel loadModel(File cloneFolder) throws IOException {
        logMessage("Importing model at '" + cloneFolder + "'");

        GraficoModelImporter importer = new GraficoModelImporter(cloneFolder);
        IArchimateModel model = importer.importAsModel();
        
        if(importer.getUnresolvedObjects() != null) {
            throw new IOException("Model had unresolved objects!");
        }
        
        // Add an Archive Manager and load images
        IArchiveManager archiveManager = IArchiveManager.FACTORY.createArchiveManager(model);
        model.setAdapter(IArchiveManager.class, archiveManager);
        archiveManager.loadImages();
        
        return model;
    }
    
    private Map<String, String> parseArgs(String[] args) {
        Map<String, String> argsMap = new HashMap<String, String>();
        
        for(int i = 0; i < args.length; i++) {
            String s = args[i];
            switch(s) {
                case ARGS_CLONEFOLDER:
                case ARGS_REPORTFOLDER:
                case ARGS_REPO_URL:
                case ARGS_REPO_USERNAME:
                case ARGS_REPO_PASSWORD:
                    if(i < args.length - 1) {
                        argsMap.put(s, args[i + 1]);
                    }
                    break;

                default:
                    break;
            }
        }
        
        return argsMap;
    }
    
    private boolean hasCorrectArgs(Map<String, String> args) {
        return args.containsKey(ARGS_CLONEFOLDER) && args.containsKey(ARGS_REPORTFOLDER);
    }
    
    private void logMessage(String message) {
        System.out.println(HTMLREPORT + " " + message); //$NON-NLS-1$
    }
    
    private void logError(String message) {
        System.err.println(HTMLREPORT + " " + message); //$NON-NLS-1$
    }
}
