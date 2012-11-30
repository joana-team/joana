/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 02.12.2004
 *
 */
package edu.kit.joana.ui.ifc.sdg.gui.marker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;


import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;

/**
 * @author naxan, giffhorn
 *
 */
public class NJSecMarkerFactory {

	/* Marker factories */

	/* The first three factories are for annotations where the nodes belonging to the annotations are known. */

	public IMarker createOutputMarker(IResource resource, String required, String message,
			int line, int offset, int length, String nodes, int sc, int ec)
    throws CoreException {
        return createMarker(resource, NJSecMarkerConstants.MARKER_TYPE_OUTPUT,
        		required, SecurityNode.UNDEFINED,
        		message, line, offset, length, nodes, sc, ec);
    }

    public IMarker createInputMarker(IResource resource, String provided, String message,
    		int line, int offset, int length, String nodes, int sc, int ec)
    throws CoreException {
        return createMarker(resource, NJSecMarkerConstants.MARKER_TYPE_INPUT,
        		SecurityNode.UNDEFINED, provided,
        		message, line, offset, length, nodes, sc, ec);
    }

    public IMarker createRedefiningMarker(IResource resource, String required, String provided,
    		String message, int line, int offset, int length, String nodes, int sc, int ec)
    throws CoreException {
        return createMarker(resource, NJSecMarkerConstants.MARKER_TYPE_REDEFINE,
        		required, provided,
        		message, line, offset, length, nodes, sc, ec);
    }


	/* The next three factories are for annotations where the nodes are not known. */

	public IMarker createOutputMarker(IResource resource, String required, String message,
			int line, int offset, int length, int sc, int ec)
    throws CoreException {
        return createMarker(resource, NJSecMarkerConstants.MARKER_TYPE_OUTPUT,
        		required, SecurityNode.UNDEFINED,
        		message, line, offset, length, "", sc, ec);
    }

    public IMarker createInputMarker(IResource resource, String provided, String message,
    		int line, int offset, int length, int sc, int ec)
    throws CoreException {
        return createMarker(resource, NJSecMarkerConstants.MARKER_TYPE_INPUT,
        		SecurityNode.UNDEFINED, provided,
        		message, line, offset, length, "", sc, ec);
    }

    public IMarker createRedefiningMarker(IResource resource, String required, String provided,
    		String message, int line, int offset, int length, int sc, int ec)
    throws CoreException {
        return createMarker(resource, NJSecMarkerConstants.MARKER_TYPE_REDEFINE,
        		required, provided,
        		message, line, offset, length, "", sc, ec);
    }



//    public IMarker createIFlowMarker(IResource resource, String securityClassIn,
//            String securityClassOut, String message, int line, int offset, int length)
//    throws CoreException {
//        return createMarker(resource, NJSecMarkerConstants.MARKER_TYPE_IFLOW,
//        		securityClassIn, securityClassOut,
//        		message, line, offset, length, "");
//    }


    /** This method does the real work.
     */
    private IMarker createMarker(IResource resource, String markerType, String required, String provided,
    		String message, int line, int offset, int length, String nodes, int sc, int ec)
    throws CoreException {
        IMarker marker = resource.createMarker(markerType);

        marker.setAttribute(IMarker.MESSAGE, message);
        marker.setAttribute(IMarker.LINE_NUMBER, line);
        marker.setAttribute(IMarker.CHAR_START, offset);
        marker.setAttribute(IMarker.CHAR_END, offset + length);
        marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_START_COLUMN, sc);
        marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_END_COLUMN, ec);
        marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, required);
        marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, provided);

//        Declassification
//        if (markerType.equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
//        	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_NUMBER_OF_PAIRS, 1);
//        }

		marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, nodes);
        marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_ACTIVE, true);

        return marker;
    }


    /* Marker access methods */

    public void deleteNJSecMarker(IMarker marker)
    throws CoreException {
        marker.delete();
    }

    /**
     *
     * @param project
     * @param markerType
     * @return
     * @throws CoreException - if finding markers fails
     * <br> possible reasons: project doesn't exist or project isn't opened
     */
    public IMarker[] findNJSecMarkers(IResource resource, String markerType)
    throws CoreException {
        IMarker[] ims = resource.findMarkers(markerType, true, IResource.DEPTH_INFINITE);
        return ims;
    }

    /**
     * Adds a new pair of security classes to an redefining marker
     * @param marker The marker to add.
     * @param securityClassIn new Class In
     * @param securityClassOut new Class Out
     * @throws CoreException - if marker does not exist
     */
    public void addPairToRedefiningMarker(IMarker marker, String required, String provided)
    throws CoreException {
    	//check if redefining marker
    	if (!marker.getType().equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
			return;
		}

    	String requiredTmp = marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "");

    	if (requiredTmp.isEmpty()) {
    		// empty
    		marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, provided);
        	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, required);

    	} else {
    		String providedTmp = marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "");
        	String[] requiredArray = requiredTmp.split(";");

	    	// each required label should be unique
	    	for (int i = 0; i < requiredArray.length; ++i) {
	    		if (required.equals(requiredArray[i])) {
	    			return;
	    		}
	    	}

	    	providedTmp += ";" + provided;
	    	requiredTmp += ";" + required;

	    	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, providedTmp);
	    	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, requiredTmp);
    	}
    }

    /**
     * Removes a pair from a redefining marker
     * @param marker the marker to modify
     * @param pair the number of the pair to remove (starts with 1)
     * @throws CoreException - if marker does not exist
     */
    public void removePairFromRedefiningMarker(IMarker marker, String required, String provided)
    throws CoreException {
    	//check if redefining marker
    	if (!marker.getType().equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
			return;
		}

    	String[] requiredTmp = marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "").split(";");

    	if (requiredTmp.length < 1) {
    		return;
    	}

    	String[] providedTmp = marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "").split(";");
    	StringBuffer providedNew = new StringBuffer();
    	StringBuffer requiredNew = new StringBuffer();

    	for (int i = 0; i < providedTmp.length; ++i) {
    		if (providedTmp[i].equals(provided) && requiredTmp[i].equals(required)) {
    			continue;
    		}

    		providedNew.append(";").append(providedTmp[i]);
    		requiredNew.append(";").append(requiredTmp[i]);
    	}

    	if (providedNew.length() > 0) {
	    	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, providedNew.substring(1));
	    	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, requiredNew.substring(1));

    	} else {
    		// letzte Annotation wurde geloescht
	    	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "");
	    	marker.setAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "");
    	}
    }

	// getText() returns the contents of the file
	private static String getText(IFile file)
	throws CoreException, IOException {
		InputStream in = file.getContents();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int read = in.read(buf);
		while (read > 0) {
			out.write(buf, 0, read);
			read = in.read(buf);
		}
		return out.toString();
	}

	public int getCharStartOfRow(IFile file, int line)
	throws CoreException, IOException {
		String text = null;
		if ("class".equals(file.getProjectRelativePath().getFileExtension())) {
			IClassFile cfile = JavaCore.createClassFileFrom(file);
			text = cfile.getSource();
		} else {
			text = getText(file);
		}
		text = text == null ?  "" : text;

		IDocument doc = new Document(text);

		try {
			return doc.getLineOffset(line);

		} catch (BadLocationException e) {
			return -1; //TODO moeglicherweise doch eine exception schmeissen?
		}
	}

	public int getNumberOfDeclassRules(IMarker marker)
	throws CoreException {
		if (!marker.getType().equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
			return 0;
		} else {
			String[] providedTmp = marker.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "").split(";");
			return providedTmp.length;
		}
	}

    /* Load and save markers from/to hard drive */

	public void saveMarker(File dest, IMarker[] ims) {
		try {
			FileWriter fw = new FileWriter(dest);
			BufferedWriter bf = new BufferedWriter(fw);
			IMarker[] imarkers = ims;

			for (IMarker im : imarkers) {
				bf.write("+++\n");
				bf.write(im.getAttribute(IMarker.MESSAGE, "") + "\n");
				bf.write(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_PROVIDED, "") + "\n");
				bf.write(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_REQUIRED, "") + "\n");
				bf.write(im.getResource().getProjectRelativePath().toPortableString() + "\n");
				bf.write(im.getAttribute(IMarker.LINE_NUMBER, -1) + "\n");
				bf.write(im.getAttribute(IMarker.CHAR_START, -1) + "\n");
				bf.write(im.getAttribute(IMarker.CHAR_END, -1) + "\n");
				bf.write(im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, "") + "\n");
				bf.write(im.getType() + "\n");
				bf.write("---\n");
			}

			bf.close();

		} catch (IOException e) {
			NJSecPlugin.singleton().showError(e.getMessage(), null, e);

		} catch (CoreException e) {
			NJSecPlugin.singleton().showError(e.getMessage(), null, e);
		}

	}

	public void loadMarkerIntoActiveProject(File dest) {
		try {
			FileReader fr = new FileReader(dest);
			BufferedReader br = new BufferedReader(fr);

			while (br.ready()) {
				String message = br.readLine();
				String provided = br.readLine();
				String required = br.readLine();
				String resourcefullpath = br.readLine();
				int linenumber = Integer.parseInt(br.readLine());
				int char_start = Integer.parseInt(br.readLine());
				int char_end = Integer.parseInt(br.readLine());
				String sdgnodeid = br.readLine();
				String markerType = br.readLine();

				IResource ir = NJSecPlugin.singleton().getActiveProject().findMember(resourcefullpath);
				int rowcharstart = getCharStartOfRow((IFile) ir, linenumber-1);

				IMarker im = null;

				if (markerType.equals(NJSecMarkerConstants.MARKER_TYPE_INPUT)) {
					im = createInputMarker(ir, provided, message, linenumber, char_start, char_end - char_start, char_start - rowcharstart, char_end - rowcharstart);

				} else if (markerType.equals(NJSecMarkerConstants.MARKER_TYPE_OUTPUT)) {
					im = createOutputMarker(ir, required, message, linenumber, char_start, char_end - char_start, char_start - rowcharstart, char_end - rowcharstart);

				} else if (markerType.equals(NJSecMarkerConstants.MARKER_TYPE_REDEFINE)) {
					im = createRedefiningMarker(ir, required, provided, message, linenumber, char_start, char_end - char_start, char_start - rowcharstart, char_end - rowcharstart);
				}

				if (im != null) {
					im.setAttribute(IMarker.LINE_NUMBER, linenumber);
					im.setAttribute(IMarker.CHAR_START, char_start);
					im.setAttribute(IMarker.CHAR_END, char_end);
					im.setAttribute(NJSecMarkerConstants.MARKER_ATTR_MATCHING_SDGNODES, sdgnodeid);
				}
			}

			br.close();

		} catch (IOException e) {
			NJSecPlugin.singleton().showError(e.getMessage(), null, e);

		} catch (NumberFormatException e) {
			NJSecPlugin.singleton().showError(e.getMessage(), null, e);

		} catch (CoreException e) {
			NJSecPlugin.singleton().showError(e.getMessage(), null, e);
		}
	}
}
