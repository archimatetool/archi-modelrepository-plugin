package org.archicontribs.modelrepository.merge;

import java.io.IOException;
import java.util.List;

import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.archimatetool.model.IArchimateModel;

public interface IMergeConflictHandler {

    String getLocalRef();
    String getTheirRef();
    IArchiRepository getArchiRepository();
    IArchimateModel getOurModel();
    IArchimateModel getTheirModel();
    List<MergeObjectInfo> getMergeObjectInfos();
    void resetToLocalState() throws IOException, GitAPIException;
    void merge() throws IOException, GitAPIException;
	void init(IProgressMonitor progressMonitor) throws IOException, GitAPIException;
}
