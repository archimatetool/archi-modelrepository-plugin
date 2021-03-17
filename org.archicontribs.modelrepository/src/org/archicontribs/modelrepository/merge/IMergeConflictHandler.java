package org.archicontribs.modelrepository.merge;

import java.util.List;

import org.archicontribs.modelrepository.grafico.IArchiRepository;

import com.archimatetool.model.IArchimateModel;

public interface IMergeConflictHandler {

    String getLocalRef();
    String getTheirRef();
    IArchiRepository getArchiRepository();
    IArchimateModel getOurModel();
    IArchimateModel getTheirModel();
    List<MergeObjectInfo> getMergeObjectInfos();
}
