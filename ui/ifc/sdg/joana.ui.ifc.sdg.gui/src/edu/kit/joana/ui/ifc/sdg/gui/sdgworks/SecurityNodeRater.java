/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.sdgworks;

import java.util.HashMap;
import java.util.List;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;

import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ui.ifc.sdg.gui.NJSecPlugin;
import edu.kit.joana.ui.ifc.sdg.gui.marker.NJSecMarkerConstants;

public class SecurityNodeRater implements ISecurityNodeRater {
	public static enum Case {
        RETURN,
        CALL,
        DEFAULT;
    }

//	private HashMap3d<IFile, Integer, Integer> linecache = new HashMap3d<IFile, Integer, Integer>();
//
//	/***
//	 * Returns a rating that identifies how good SDGNode m probably matches to IMarker im
//	 * @param im
//	 * @param m
//	 * @return
//	 */
//	public int getRating(IMarker im, SecurityNode m) {
//
//		int ret = 0;
//
//		String label = m.getLabel();
//
//		//basic selection
//		if (m.getOperation().equals(SecurityNode.Operation.ASSIGN)) ret += 10;
//		if (m.getOperation().equals(SecurityNode.Operation.ACTUAL_IN)) ret += 10;
//		if (m.getLabel() != null && label.indexOf("lv") >= 0) ret += 11;
//		if (m.getLabel() != null && label.indexOf("un") >= 0) ret += 10;
//		if (m.getLabel() != null && label.indexOf("exc") >= 0) ret-= 10;
//		if (m.getLabel() != null && label.indexOf("OPER") >= 0) ret-= 5;
//		if (m.getLabel() != null && label.indexOf("java.lang.") >= 0) ret-= 5;
//		if (m.getKind().equals(SecurityNode.Kind.ACTUAL_IN)) ret += 10;
//		if (m.getKind().equals(SecurityNode.Kind.EXPRESSION)) ret += 10;
//
//		//marker-matching selection
//		int mcharstart = im.getAttribute(IMarker.CHAR_START, -1);
//		int mcharend = im.getAttribute(IMarker.CHAR_END, -1);
//
//		int linestart = 0;
//		boolean gotLine = true;
//		if (linecache.containsKey((IFile) im.getResource(), m.getSr())) {
//			linestart = linecache.get((IFile) im.getResource(), m.getSr());
//		} else {
//			try {
//				linestart = NJSecPlugin.getDefault().getMarkerFactory().getCharStartOfRow((IFile) im.getResource(), m.getSr());
//				linecache.put((IFile) im.getResource(), m.getSr(), linestart);
//			} catch (CoreException e) {
//				gotLine = false;
//			} catch (IOException e) {
//				gotLine = false;
//			}
//		}
//		if (gotLine) {
//			int mstartColumn = mcharstart - linestart;
//			int mendColumn = mcharend - linestart;
//			int startColumn = m.getSc();
//			int endColumn = m.getEc();
//
//			int diff = Math.abs(startColumn - mstartColumn) + Math.abs(endColumn - mendColumn);
//
//			if (diff < 20) ret += (20 - diff)*5;
//		}
//
//
//
//		/**
//		 * different lines
//		 */
//		if (m.getSr() != im.getAttribute(IMarker.LINE_NUMBER, -1)) ret-=50;
//
//		/**
//		 * Differentiate through AnnotationType
//		 */
//		try {
//			if (im.getType().equals(NJSecMarkerConstants.MARKER_TYPE_ANNOTATION)) {
//				if (m.getKind().equals(SecurityNode.Kind.EXPRESSION) ||
//					m.getKind().equals(SecurityNode.Kind.FORMAL_IN))
//					ret+=20;
//
//			} else if (im.getType().equals(NJSecMarkerConstants.MARKER_TYPE_OUTGOING)) {
//				if (m.getKind().equals(SecurityNode.Kind.ACTUAL_IN))
//					ret+=20;
//			}
//		} catch (CoreException e) {
//			//show nothing... may be called often and doesnt hurt THAT much if failes
//		}
//		return ret;
//	}

//	public int getRating(IMarker im, SecurityNode m, List<SecurityNode> ms) {
//		int ret = 0;
//
//		if (m.getSr() <= im.getAttribute(IMarker.LINE_NUMBER, -1) && m.getEr() >= im.getAttribute(IMarker.LINE_NUMBER, -1)) {
//			ret += 50;
//		} else {
//			ret -= 50;
//		}
//
//		LinkedList<SecurityNode> nodes = new LinkedList<SecurityNode>();
//
//		for (SecurityNode n : ms) {
//			if (n.getSr() <= im.getAttribute(IMarker.LINE_NUMBER, -1) && n.getEr() >= im.getAttribute(IMarker.LINE_NUMBER, -1)) {
//				nodes.add(n);
//			}
//		}
//
//		for (SecurityNode n : nodes) {
//			if (n.getOperation().equals(SecurityNode.Operation.EXIT)) {
//				//Return Knoten
//				if (m.getOperation().equals(SecurityNode.Operation.EXIT)) {
//					ret -= 0;
//				} else if (m.getOperation().equals(SecurityNode.Operation.FORMAL_OUT)) {
//					ret -= 20;
//				} else {
//					ret -= 50;
//				}
//				break;
//			} else if (n.getOperation().equals(SecurityNode.Operation.ENTRY)) {
//				//Entry Knoten
//				if (m.getOperation().equals(SecurityNode.Operation.FORMAL_IN)) {
//					ret -= 0;
//				} else if (m.getOperation().equals(SecurityNode.Operation.FORMAL_OUT)) {
//					ret -= 50;
//				} else if (m.getOperation().equals(SecurityNode.Operation.ENTRY)) {
//					ret -= 20;
//				} else {
//					ret -= 50;
//				}
//				break;
//			} else if (n.getOperation().equals(SecurityNode.Operation.CALL)) {
//				//Call Knoten
//				if (m.getOperation().equals(SecurityNode.Operation.ACTUAL_IN)) {
//					ret -= 0;
//				} else if (m.getOperation().equals(SecurityNode.Operation.ACTUAL_OUT)) {
//					ret -= 20;
//				} else if (m.getOperation().equals(SecurityNode.Operation.CALL)) {
//					ret -= 20;
//				} else {
//					ret -= 50;
//				}
//				break;
//			}
//		}
//
//		return ret;
//	}

	public HashMap<SecurityNode, Integer> getRating(IMarker im, List<SecurityNode> candidates) {
		HashMap<SecurityNode, Integer> rating = new HashMap<SecurityNode, Integer>(candidates.size());
		Case _case = getCase(im);

		for (SecurityNode m : candidates) {
			int rate = 0;

			if (NJSecPlugin.singleton().getSDGFactory().getCachedSDG(NJSecPlugin.singleton().getActiveProject()).getJoanaCompiler()) {
				if (m.getSr() <= im.getAttribute(IMarker.LINE_NUMBER, -1) && m.getEr() >= im.getAttribute(IMarker.LINE_NUMBER, Integer.MAX_VALUE)
						&& m.getSc() >= im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_START_COLUMN, Integer.MAX_VALUE)
						&& m.getEc() <= im.getAttribute(NJSecMarkerConstants.MARKER_ATTR_END_COLUMN, -1)) {
					rate += 50;
				} else {
					rate -= 50;
				}
			} else {
				if (m.getSr() <= im.getAttribute(IMarker.LINE_NUMBER, -1) && m.getEr() >= im.getAttribute(IMarker.LINE_NUMBER, Integer.MAX_VALUE)) {
					rate += 50;
				} else {
					rate -= 50;
				}
			}

			switch(_case) {
			case RETURN:
				if (m.getOperation().equals(SecurityNode.Operation.EXIT)) {
					rate -= 0;
				} else if (m.getOperation().equals(SecurityNode.Operation.FORMAL_OUT)) {
					rate -= 20;
				} else {
					rate -= 50;
				}
				break;

			case CALL:
				if (m.getOperation().equals(SecurityNode.Operation.FORMAL_IN)) {
					rate -= 0;
				} else if (m.getOperation().equals(SecurityNode.Operation.FORMAL_OUT)) {
					rate -= 50;
				} else if (m.getOperation().equals(SecurityNode.Operation.ENTRY)) {
					rate -= 20;
				} else {
					rate -= 50;
				}
				break;

			case DEFAULT:
				if (m.getOperation().equals(SecurityNode.Operation.ACTUAL_IN)) {
					rate -= 0;
				} else if (m.getOperation().equals(SecurityNode.Operation.ACTUAL_OUT)) {
					rate -= 20;
				} else if (m.getOperation().equals(SecurityNode.Operation.CALL)) {
					rate -= 20;
				} else {
					rate -= 50;
				}
				break;
			}

			rating.put(m, rate);
		}

		return rating;
	}

	private Case getCase(IMarker im) {
		String marker = getText(im);

		if (marker.matches(".*return.*")) {
			return Case.RETURN;

		} else if (marker.matches(".*(public|private|protected)[^=]*\\(.*\\).*")) {
			return Case.CALL;

		} else {
			return Case.DEFAULT;
		}
	}

	private String getText(IMarker im) {
		try {
			String str = (String) im.getAttributes().get("message");
			int start = str.indexOf("Value: ") + 7;
			String text = str.substring(start);
			return text;

		} catch(CoreException e) { }

		return "";
	}
}
