/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.swt.graphics.Image;

import edu.kit.joana.ui.wala.easyifc.Activator;
import edu.kit.joana.ui.wala.easyifc.model.FileSourcePositions;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SLeak;
import edu.kit.joana.ui.wala.easyifc.model.SourcePosition;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class EasyIFCMarkerAndImageManager {

	public enum Marker {
		SECRET_INPUT("joana.ui.easyifc.marker.secret_input", "icons/secret_input_big.png"),
		PUBLIC_OUTPUT("joana.ui.easyifc.marker.public_output", "icons/public_output_big.png"),
		CRITICAL_INTERFERENCE("joana.ui.easyifc.marker.critical_interference", "icons/critical_interference_big.png");
		
		public final String id;
		public final String bigImg;
		
		private Marker(final String id, final String bigImg) {
			this.id = id;
			this.bigImg = bigImg;
		}
		
	}
	
	public static final String NO_LEAK_IMG					= "icons/no_leak_big.png";
    public static final String ILLEGAL_FLOW_EXC_IMG 		= "icons/illegal_flow_exc_big.png";
	public static final String ILLEGAL_FLOW_THREAD_IMG 		= "icons/illegal_flow_thread_big.png";
	public static final String ILLEGAL_FLOW_THREAD_EXC_IMG 	= "icons/illegal_flow_thread_exc_big.png";
	public static final String ILLEGAL_FLOW_IMG 			= "icons/illegal_flow_big.png";

	public static final String CRITICAL_MARKER = "joana.ui.easyifc.highlight.critical";
	public static final String IFC_MARKER = "joana.ui.easyifc.marker";

    private final List<IMarker> markers = new LinkedList<IMarker>();

    private static EasyIFCMarkerAndImageManager instance = null;

    private final JavaElementLabelProvider jLables = new JavaElementLabelProvider();

    public static EasyIFCMarkerAndImageManager getInstance() {
    	if (instance == null) {
    		instance = new EasyIFCMarkerAndImageManager();
    	}

    	return instance;
    }

    public Image getImage(final SourceMethod m) {
		return jLables.getImage(m);
    }

    public Image getImage(final IFCResult ifcres) {
		return (ifcres.hasLeaks() 
				? Activator.getImageDescriptor(ILLEGAL_FLOW_IMG).createImage()
				: Activator.getImageDescriptor(NO_LEAK_IMG).createImage());
    }

    public Image getImage(final SLeak leak) {
    	switch (leak.getReason()) {
    	case BOTH_FLOW:
    	case DIRECT_FLOW:
    	case INDIRECT_FLOW:
        	return Activator.getImageDescriptor(ILLEGAL_FLOW_IMG).createImage();
    	case EXCEPTION:
        	return Activator.getImageDescriptor(ILLEGAL_FLOW_EXC_IMG).createImage();
    	case THREAD:
        	return Activator.getImageDescriptor(ILLEGAL_FLOW_THREAD_IMG).createImage();
    	case THREAD_EXCEPTION:
        	return Activator.getImageDescriptor(ILLEGAL_FLOW_THREAD_EXC_IMG).createImage();
    	}
    	
    	return Activator.getImageDescriptor(ILLEGAL_FLOW_IMG).createImage();
    }
    
    private synchronized IMarker create(final IResource res, final String msg, final int lineNr, final String kind) {
    	try {
			final IMarker m = res.createMarker(kind);

			m.setAttribute(IMarker.MESSAGE, msg);
			m.setAttribute(IMarker.LINE_NUMBER, lineNr);
			m.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
			m.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

			markers.add(m);

			return m;
		} catch (CoreException e) {
		}

    	return null;
    }

    public IMarker createMarker(final IResource res, final String msg, final int lineNr, final Marker marker) {
    	return create(res, msg, lineNr, marker.id);
    }

    public synchronized void clearAll(final IProject p) {
    	for (final IMarker m : markers) {
    		try {
    			if (m.exists()) {
    				m.delete();
    			}
			} catch (CoreException e) {
			}
    	}

    	markers.clear();

    	// search rest of textmarkers (left over from crashed runs)
    	if (p != null) {
    		try {
				final IMarker[] found = p.findMarkers(IFC_MARKER, true, IResource.DEPTH_INFINITE);
				if (found != null) {
					for (final IMarker m : found) {
						if (m.exists()) {
							m.delete();
						}
					}
				}
			} catch (CoreException e) {}
    	}
    }

    private final List<IMarker> sliceMarkers = new LinkedList<IMarker>();

    public synchronized void clearAllSliceMarkers() {
    	for (final IMarker m : sliceMarkers) {
    		try {
    			if (m.exists()) {
    				m.delete();
    			}
			} catch (CoreException e) {
			}
    	}

    	sliceMarkers.clear();
    }

	public synchronized void createChopMarkers(final IFile file, final FileSourcePositions f) {
		try {
			final TIntObjectMap<LinePos> line2char = countCharsToLine(file);

			for (final SourcePosition spos : f.getPositions()) {
				if (spos.getFirstLine() == 0 || !line2char.containsKey(spos.getFirstLine())) {
					continue;
				}

				for (int line = spos.getFirstLine(); line <= spos.getLastLine(); line++) {
					try {
						final LinePos pos = line2char.get(line);
						final Map<String, Integer> m = new HashMap<String, Integer>();

						int startChar = pos.firstReadableChar;
						if (spos.getFirstLine() == line && spos.getFirstCol() > 0) {
							startChar = pos.firstChar + spos.getFirstCol();
						}

						int endChar = pos.lastReadableChar;
						if (spos.getLastLine() == line && spos.getLastCol() > 0) {
							endChar = pos.firstChar + spos.getLastCol();
							if (endChar > pos.lastChar) {
								endChar = pos.lastChar;
							}
						}

						m.put(IMarker.CHAR_START, startChar);
						m.put(IMarker.CHAR_END, endChar);
						m.put(IMarker.LINE_NUMBER, line);
						m.put(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
						m.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);

						final IMarker marker = file.createMarker(CRITICAL_MARKER);
						marker.setAttributes(m);
						sliceMarkers.add(marker);
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class LinePos {
		public int firstChar;
		public int firstReadableChar;
		public int lastReadableChar;
		public int lastChar;
	}

	private static TIntObjectMap<LinePos> countCharsToLine(final IFile f) throws CoreException, IOException {
		final TIntObjectMap<LinePos> line2char = new TIntObjectHashMap<LinePos>();
		final InputStream in = f.getContents();
		final InputStreamReader reader = new InputStreamReader(in);
		final BufferedReader br = new BufferedReader(reader);

		int currentLine = 1;
		int currentChar = 0;

		while (br.ready()) {
			final String line = br.readLine();

			final LinePos pos = new LinePos();
			pos.firstChar = currentChar;
			pos.lastChar = currentChar + line.length();
			pos.firstReadableChar = currentChar + findFirstReadbleChar(line);
			pos.lastReadableChar = currentChar + findLastReadbleChar(line);

			line2char.put(currentLine, pos);

			currentChar += line.length() + 1;
			currentLine++;
//			System.out.print("line: " + currentLine + "\tchar: " + currentChar);
//			System.out.println("\t - " + line);
//			line2char.put(currentLine, currentChar);
		}

		return line2char;
	}

	private static int findFirstReadbleChar(final String line) {
		int pos = -1;

		for (int i = 0; pos < 0 && i < line.length(); i++) {
			switch (line.charAt(i)) {
			case '\t':
			case '\r':
			case ' ':
				break;
			default:
				pos = i;
				break;
			}
		}

		return (pos < 0 ? 0 : pos);
	}

	private static int findLastReadbleChar(final String line) {
		int pos = -1;

		for (int i = line.length() - 1; pos < 0 && i >= 0; i--) {
			switch (line.charAt(i)) {
			case '\t':
			case '\r':
			case ' ':
				break;
			default:
				pos = i + 1;
				break;
			}
		}

		return (pos < 0 ? line.length() : pos);
	}

}
