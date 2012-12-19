/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.graphviewer.view;

public class EdgeViewSettings {

	/** show control dependencies */
	private boolean showCD = true;
	/** show control flow edges */
	private boolean showCF = true;
	/** show data dep edges */
	private boolean showDD = true;
	/** show heap data dep edges */
	private boolean showDH = true;
	/** show parameter structure edges */
	private boolean showPS = true;
	/** show interference edges */
	private boolean showIF = true;

	public EdgeViewSettings(boolean showDF, boolean showCF, boolean showDD, boolean showDH, boolean showPS, boolean showIF) {
		super();
		this.showCD = showDF;
		this.showCF = showCF;
		this.showDD = showDD;
		this.showDH = showDH;
		this.showPS = showPS;
		this.showIF = showIF;
	}


	public boolean isShowDD() {
		return showDD;
	}

	public void setShowDD(boolean showDD) {
		this.showDD = showDD;
	}

	public boolean isShowDH() {
		return showDH;
	}

	public void setShowDH(boolean showDH) {
		this.showDH = showDH;
	}

	public boolean isShowPS() {
		return showPS;
	}

	public void setShowPS(boolean showPS) {
		this.showPS = showPS;
	}


	public boolean isShowIF() {
		return showIF;
	}

	public void setShowIF(boolean showIF) {
		this.showIF = showIF;
	}



	public boolean isShowCD() {
		return showCD;
	}

	public void setShowCD(boolean showCD) {
		this.showCD = showCD;
	}



	public boolean isShowCF() {
		return showCF;
	}

	public void setShowCF(boolean showCF) {
		this.showCF = showCF;
	}
}
