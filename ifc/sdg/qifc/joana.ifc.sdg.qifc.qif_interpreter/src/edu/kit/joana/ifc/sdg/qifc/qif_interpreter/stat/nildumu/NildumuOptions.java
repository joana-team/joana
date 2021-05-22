package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat.nildumu;

import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Type;
import nildumu.Lattices;

public class NildumuOptions {

	public static final NildumuOptions DEFAULT = new NildumuOptions(Type.INTEGER.bitwidth(),
			Lattices.BasicSecLattice.get());

	public int intWidth;
	public Lattices.SecurityLattice<Lattices.BasicSecLattice> secLattice;

	public NildumuOptions(int intWIdth, Lattices.SecurityLattice<Lattices.BasicSecLattice> secLattice) {
		this.intWidth = intWIdth;
		this.secLattice = secLattice;
	}
}
