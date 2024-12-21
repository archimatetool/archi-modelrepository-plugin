package org.archicontribs.modelrepository.grafico;

import java.io.File;

import org.eclipse.emf.ecore.EObject;

import com.archimatetool.model.FolderType;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.IIdentifier;

public final class GraficoFileConventions {

	private GraficoFileConventions() {
		// utility class
	}

	public static File forFolder(File parentFolder, IFolder iFolder) {
		return new File(parentFolder, GraficoFileConventions.getNameFor(iFolder));
	}

	public static File forElement(File modelFolder, IFolder iFolder, EObject elem) {
		File elemFolder = new File(modelFolder, GraficoFileConventions.getNameFor(iFolder));
		return GraficoFileConventions.forElement(elemFolder, elem);
	}

	public static File forElement(File elemFolder, EObject elem) {
		return new File(elemFolder, elem.getClass().getSimpleName() + "_" + ((IIdentifier) elem).getId() + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Generate a proper name for directory creation
	 *
	 * @param folder
	 * @return
	 */
	public static String getNameFor(IFolder folder) {
		return folder.getType() == FolderType.USER ? folder.getId().toString() : folder.getType().toString();
	}

}
