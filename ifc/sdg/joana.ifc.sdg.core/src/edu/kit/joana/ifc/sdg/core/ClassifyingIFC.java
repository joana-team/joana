/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.core;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.core.violations.ViolationTranslator;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;

/**
 * @author Martin Mohr
 */
public class ClassifyingIFC extends IFC {

	private final IFC ifc;

	public ClassifyingIFC(IFC ifc) {
		super(ifc.getSDG(), ifc.getLattice());
		this.ifc = ifc;
	}

	
	/**
	 * @param pl
	 * @see edu.kit.joana.ifc.sdg.core.IFC#addProgressListener(edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener)
	 */
	public void addProgressListener(ProgressListener pl) {
		ifc.addProgressListener(pl);
	}

	/**
	 * @return
	 * @see edu.kit.joana.ifc.sdg.core.IFC#getSDG()
	 */
	public SDG getSDG() {
		return ifc.getSDG();
	}

	/**
	 * @return
	 * @see edu.kit.joana.ifc.sdg.core.IFC#getLattice()
	 */
	public IStaticLattice<String> getLattice() {
		return ifc.getLattice();
	}

	/**
	 * @param lattice
	 * @throws WrongLatticeDefinitionException
	 * @throws IOException
	 * @see edu.kit.joana.ifc.sdg.core.IFC#setLattice(java.io.File)
	 */
	public void setLattice(File lattice) throws WrongLatticeDefinitionException, IOException {
		ifc.setLattice(lattice);
	}

	/**
	 * @param lattice
	 * @see edu.kit.joana.ifc.sdg.core.IFC#setLattice(edu.kit.joana.ifc.sdg.lattice.IStaticLattice)
	 */
	public void setLattice(IStaticLattice<String> lattice) {
		ifc.setLattice(lattice);
	}

	/**
	 * @param sdg
	 * @throws IOException
	 * @see edu.kit.joana.ifc.sdg.core.IFC#setSDG(java.lang.String)
	 */
	public void setSDG(String sdg) throws IOException {
		ifc.setSDG(sdg);
	}

	/**
	 * @param sdg
	 * @see edu.kit.joana.ifc.sdg.core.IFC#setSDG(edu.kit.joana.ifc.sdg.graph.SDG)
	 */
	public void setSDG(SDG sdg) {
		ifc.setSDG(sdg);
	}

	/**
	 * @param sdgfile
	 * @throws IOException
	 * @see edu.kit.joana.ifc.sdg.core.IFC#setSDG(java.io.File)
	 */
	public void setSDG(File sdgfile) throws IOException {
		ifc.setSDG(sdgfile);
	}

	/**
	 * @param pl
	 * @see edu.kit.joana.ifc.sdg.core.IFC#removeProgressListener(edu.kit.joana.ifc.sdg.core.interfaces.ProgressListener)
	 */
	public void removeProgressListener(ProgressListener pl) {
		ifc.removeProgressListener(pl);
	}

	/**
	 * @param progressTitle
	 * @param progress
	 * @param maxProgress
	 * @see edu.kit.joana.ifc.sdg.core.IFC#notifyProgressListeners(java.lang.String, int, int)
	 */
	public void notifyProgressListeners(String progressTitle, int progress, int maxProgress) {
		ifc.notifyProgressListeners(progressTitle, progress, maxProgress);
	}

	/**
	 * @param progressTitle
	 * @param progress
	 * @param progressmax
	 * @see edu.kit.joana.ifc.sdg.core.IFC#progressChanged(java.lang.String, int, int)
	 */
	public void progressChanged(String progressTitle, int progress, int progressmax) {
		ifc.progressChanged(progressTitle, progress, progressmax);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return ifc.toString();
	}


	/* (non-Javadoc)
	 * @see edu.kit.joana.ifc.sdg.core.IFC#checkIFlow()
	 */
	@Override
	public Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException {
		Collection<? extends IViolation<SecurityNode>> vios = ifc.checkIFlow();
		return new ViolationTranslator().map(vios);
	}
	
	
}
