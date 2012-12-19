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

/**
 * @author naxan
 *
 */
public interface NJSecMarkerConstants {
	public final String MARKER_ATTR_REQUIRED = "edu.kit.joana.ifc.sdg.gui.securityclass.allowed";
	public final String MARKER_ATTR_PROVIDED = "edu.kit.joana.ifc.sdg.gui.securityclass.defining";
	public final String MARKER_ATTR_MATCHING_SDGNODES = "edu.kit.joana.ifc.sdg.gui.sdgnode.matched";
//	public final String MARKER_ATTR_NUMBER_OF_PAIRS = "edu.kit.joana.ifc.sdg.gui.number.pairs";
	public final String MARKER_ATTR_ACTIVE = "edu.kit.joana.ifc.sdg.gui.marker.active";
	public final String MARKER_ATTR_START_COLUMN = "edu.kit.joana.ifc.sdg.gui.start.column";
	public final String MARKER_ATTR_END_COLUMN = "edu.kit.joana.ifc.sdg.gui.end.column";

	public final String MARKER_TYPE_INPUT = "edu.kit.joana.ifc.sdg.gui.annotation";
	public final String MARKER_TYPE_REDEFINE = "edu.kit.joana.ifc.sdg.gui.redefining";
	public final String MARKER_TYPE_OUTPUT = "edu.kit.joana.ifc.sdg.gui.outgoing";
	public final String MARKER_TYPE_IFLOW = "edu.kit.joana.ifc.sdg.gui.iflow";
	public final String MARKER_TYPE_NJSEC = "edu.kit.joana.ifc.sdg.gui.njsecmarker";
}

