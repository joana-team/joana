/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.flowless.util;

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

import edu.kit.joana.ui.wala.flowless.Activator;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResult;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.wala.dictionary.util.FileSourcePositions;
import edu.kit.joana.wala.dictionary.util.SourcePosition;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public class CheckFlowMarkerAndImageManager {

	public static final String FLOW_OK = "edu.kit.joana.ui.checkflow_ok";
	public static final String FLOW_OK_IMG = "icons/flow_ok_big.png";

    public static final String FLOW_NO_EXC_OK = "edu.kit.joana.ui.checkflow_no_exc_ok";
    public static final String FLOW_NO_EXC_OK_IMG = "icons/flow_no_exc_ok_big.png";

	public static final String FLOW_INFERRED_OK = "edu.kit.joana.ui.checkflow_inferred_ok";
	public static final String FLOW_INFERRED_OK_IMG = "icons/flow_inferred_ok_big.png";

	public static final String FLOW_INFERRED_NO_EXC_OK = "edu.kit.joana.ui.checkflow_inferred_no_exc_ok";
	public static final String FLOW_INFERRED_NO_EXC_OK_IMG = "icons/flow_inferred_no_exc_ok_big.png";

	public static final String FLOW_ILLEGAL = "edu.kit.joana.ui.checkflow_illegal";
	public static final String FLOW_ILLEGAL_IMG = "icons/flow_illegal_big.png";

	public static final String SLICE_MARKER = "edu.kit.joana.ui.checkflow.highlight.level0";
	public static final String CHECK_FLOW_MARKER = "edu.kit.joana.ui.checkflow_marker";

    private final List<IMarker> markers = new LinkedList<IMarker>();

    private static CheckFlowMarkerAndImageManager instance = null;

    private final JavaElementLabelProvider jLables = new JavaElementLabelProvider();

    public static CheckFlowMarkerAndImageManager getInstance() {
    	if (instance == null) {
    		instance = new CheckFlowMarkerAndImageManager();
    	}

    	return instance;
    }

    public Image getImage(final SourceMethod m) {
		return jLables.getImage(m);
    }

    public Image getImage(final FlowError ferr) {
		return Activator.getImageDescriptor(FLOW_ILLEGAL_IMG).createImage();
    }

    public Image getImage(final FlowStmtResult fsr) {
    	if (fsr.isAlwaysSatisfied()) {
    		return Activator.getImageDescriptor(FLOW_OK_IMG).createImage();
    	} else if (fsr.isNeverSatisfied()) {
    		return Activator.getImageDescriptor(FLOW_ILLEGAL_IMG).createImage();
    	} else if (fsr.isNoExceptionSatisfied()) {
    		return Activator.getImageDescriptor(FLOW_NO_EXC_OK_IMG).createImage();
    	} else if (fsr.isInferredSatisfied()) {
    		return Activator.getImageDescriptor(FLOW_INFERRED_OK_IMG).createImage();
    	} else if (fsr.isInferredNoExcSatisfied()) {
    		return Activator.getImageDescriptor(FLOW_INFERRED_NO_EXC_OK_IMG).createImage();
    	} else {
    		return Activator.getImageDescriptor(FLOW_ILLEGAL_IMG).createImage();
    	}
    }

    public Image getImage(final FlowStmtResultPart fsr) {
    	if (fsr.isSatisfied()) {
    		return Activator.getImageDescriptor(FLOW_OK_IMG).createImage();
    	} else {
    		return Activator.getImageDescriptor(FLOW_ILLEGAL_IMG).createImage();
    	}
    }

    public IMarker createMarker(final IResource res, final String msg, final int lineNr, final String kind) {
    	if (FLOW_OK.equals(kind)) {
    		return createMarkerOk(res, msg, lineNr);
    	} else if (FLOW_NO_EXC_OK.equals(kind)) {
    		return createMarkerNoExcOk(res, msg, lineNr);
    	} else if (FLOW_INFERRED_OK.equals(kind)) {
    		return createMarkerInferredOk(res, msg, lineNr);
    	} else if (FLOW_INFERRED_NO_EXC_OK.equals(kind)) {
    		return createMarkerInferredNoExcOk(res, msg, lineNr);
    	} else if (FLOW_ILLEGAL.equals(kind)) {
    		return createMarkerIllegal(res, msg, lineNr);
    	} else {
    		throw new IllegalArgumentException("Unknown marker kind: " + kind);
    	}
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

    public IMarker createMarkerOk(final IResource res, final String msg, final int lineNr) {
    	return create(res, msg, lineNr, FLOW_OK);
    }

    public IMarker createMarkerNoExcOk(final IResource res, final String msg, final int lineNr) {
    	return create(res, msg, lineNr, FLOW_NO_EXC_OK);
    }

    public IMarker createMarkerInferredOk(final IResource res, final String msg, final int lineNr) {
    	return create(res, msg, lineNr, FLOW_INFERRED_OK);
    }

    public IMarker createMarkerError(final IResource res, final String msg, final int lineNr) {
    	return create(res, msg, lineNr, FLOW_ILLEGAL);
    }

    public IMarker createMarkerInferredNoExcOk(final IResource res, final String msg, final int lineNr) {
    	return create(res, msg, lineNr, FLOW_INFERRED_NO_EXC_OK);
    }

    public IMarker createMarkerIllegal(final IResource res, final String msg, final int lineNr) {
    	return create(res, msg, lineNr, FLOW_ILLEGAL);
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
				final IMarker[] found = p.findMarkers(CHECK_FLOW_MARKER, true, IResource.DEPTH_INFINITE);
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

	public synchronized void createSliceMarkers(final IFile file, final FileSourcePositions f) {
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

						final IMarker marker = file.createMarker(SLICE_MARKER);
						marker.setAttributes(m);
//						MarkerUtilities.createMarker(file, m, SLICE_MARKER);

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
