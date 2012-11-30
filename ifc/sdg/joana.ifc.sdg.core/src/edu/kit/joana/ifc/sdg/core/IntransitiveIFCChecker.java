/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
 * Created on 28.06.2004
 *
 */
package edu.kit.joana.ifc.sdg.core;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.sdgtools.SDGTools;
import edu.kit.joana.ifc.sdg.core.violations.Violation;
import edu.kit.joana.ifc.sdg.core.violations.paths.PathGenerator;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPathes;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.BarrierChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.barrier.NonSameLevelBarrierChopper;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;


/**
 * IFC checker with declassification that guarantees monotonicity of release. 
 * No transitive declassification allowed, sec labels know their original label.
 * 
 * E.g. lattice: secure < normal < public 
 * 
 * This approach:
 * secure[secure] --(secure to normal)--> normal[secure] --(normal to public)--> ERROR
 * Fails because required level of declassification checks for original label.
 * 
 * Other approaches:
 * secure --(secure to normal)--> normal --(normal to public)--> public
 */
public class IntransitiveIFCChecker extends IFC {
	private static final boolean TIME = false;
	private static final boolean TIME_VIO = false;

	private BarrierChopper chopper;
	private Collection<SecurityNode> declass;

	public IntransitiveIFCChecker(SDG sdg, IStaticLattice<String> lattice) {
		super(sdg, lattice);
		initBarrierChopper();
	}

	public IntransitiveIFCChecker(File sdg, File lattice) throws WrongLatticeDefinitionException, IOException {
		super(sdg, lattice);
		initBarrierChopper();
	}

	protected void initBarrierChopper() {
		chopper = new NonSameLevelBarrierChopper(this.g);
	}

	/**
	 * Determines security violations by applying slicing to all 'outgoing' and
	 * 'declassification' nodes in this InterFlowCheckers Graph(=sdgParser) g
	 * Basically calls <i>checkIFlow(false)</i>
	 * @return List of violations: List&lt;Violation&gt;
	 * @throws InterSlicePluginException
	 * @throws NotInLatticeException
	 */
	public Collection<Violation> checkIFlow() throws NotInLatticeException {
		return checkIFlow(false);
	}


	/**
	 * Determines security violations by applying slicing to all 'outgoing' and
	 * 'declassification' nodes in this InterFlowCheckers Graph(=sdgParser) g
	 * @return List of violations: List<Violation>
	 * @throws InterSlicePluginException
	 * @throws NotInLatticeException
	 */

	public Collection<Violation> checkIFlow(boolean generateVioPathes)
	throws NotInLatticeException {
		Collection<Violation> ret = new LinkedList<Violation>(); //list to be returned
		//get all input/{input, output} pairs which may not interfere
		Collection<SecurityNodeTuple> mustNotInterfere = computeCriteria();

		// cache all declassifications
		declass = SDGTools.getDeclassificationNodes(g);
//		System.out.println("declass: "+declass);
		// security check
		for (SecurityNodeTuple tup : mustNotInterfere) {
			long slicestart = System.currentTimeMillis();
			if (TIME) System.out.println("Started slicing at " + slicestart);

			Violation vio = checkIFC(tup);

			long sliceend = System.currentTimeMillis();
			if (TIME) System.out.println("Finished slicing at " + sliceend + " | slice duration: " + (sliceend-slicestart));

			if (vio != null) {
				ret.add(vio);
			}

			//add ViolationPathes if wished
			if (generateVioPathes) {
				ret = addViolationPathesChop(ret);
			}
		}

		//this.progressChanged("Done", 100, 100);
		return ret;
	}

	private Violation checkIFC(SecurityNodeTuple tup) {
		Violation vio = null;

		Collection<SDGNode> barrier = computeBarrier(tup);
		chopper.setBarrier(barrier);

		Collection<SDGNode> chop = chopper.chop(tup.getFirstNode(), tup.getSecondNode());

		// if the chop is not empty, noninterference is violated
		if (!chop.isEmpty()) {
			SecurityNode leak = tup.getSecondNode();
			String attacker = (tup.getSecondNode().isInformationSink() ? leak.getRequired() : leak.getProvided());
			vio = Violation.createViolation(leak, tup.getFirstNode(), attacker);
		}

		return vio;
	}

	/**
	 * @param ret
	 * @param temp
	 * @param violations
	 * @throws NotInLatticeException
	 */
	public Collection<Violation> addViolationPathesChop(Collection<Violation> violations)
	throws NotInLatticeException {
		long viostart = System.currentTimeMillis();
		if (TIME_VIO) System.out.println("Started viopathgen at " + viostart + " for " + violations.size() + " violations");

		LinkedList<Violation> ret = new LinkedList<Violation>();
		PathGenerator vpg = new PathGenerator(g);
		vpg.addProgressListener(this);

		//merge all violations into return list
		for (Violation sViolation : violations) {
			// Generate ViolationPathes and attach them to violation nodes
			ViolationPathes vps = vpg.computePaths(sViolation);
            Violation vio = Violation.createViolation(sViolation.getSink(), sViolation.getSource(), vps, sViolation.getSink().getRequired());
			ret.add(vio);
		}

		long vioend = System.currentTimeMillis();
		if (TIME_VIO) System.out.println("Ended viopathgen at " + vioend + " duration: " + (vioend - viostart));

		return ret;
	}

	private Collection<SecurityNodeTuple> computeCriteria() {
		LinkedList<SecurityNodeTuple> result = new LinkedList<SecurityNodeTuple>();
		Collection<SecurityNode> sources = SDGTools.getInformationSources(g);
		Collection<SecurityNode> sinks = SDGTools.getInformationSinks(g);

		// source -> source tuples
		for (SecurityNode from : sources) {
			for (SecurityNode to : sources) {
				if (!l.leastUpperBound(from.getProvided(), to.getProvided()).equals(to.getProvided())) {
					result.add(new SecurityNodeTuple(from, to));
				}
			}
		}

		// source -> sink tuples
		for (SecurityNode from : sources) {
			for (SecurityNode to : sinks) {
				if (!l.leastUpperBound(from.getProvided(), to.getRequired()).equals(to.getRequired())) {
					result.add(new SecurityNodeTuple(from, to));
				}
			}
		}

		return result;
	}

	private Collection<SDGNode> computeBarrier(SecurityNodeTuple tup) {
		LinkedList<SDGNode> result = new LinkedList<SDGNode>();
		String from = tup.getFirstNode().getProvided();
		String to = (tup.getSecondNode().isInformationSource() ? tup.getSecondNode().getProvided() : tup.getSecondNode().getRequired());

		for (SecurityNode d : declass) {
			if (l.leastUpperBound(d.getRequired(), from).equals(d.getRequired()) // d applies to `from'
					&& l.leastUpperBound(to, d.getProvided()).equals(to)) {      // d declassifies to `to'

				result.add(d);

			} else if (d.getAdditionalDeclass() != null) {
				// if we have multiple declassifications here, check them too
				for (String[] rule : d.getAdditionalDeclass()) {
					if (l.leastUpperBound(rule[0], from).equals(rule[0])         // d applies to `from'
							&& l.leastUpperBound(to, rule[1]).equals(to)) {      // d declassifies to `to'

						result.add(d);
						break; // adding d once suffices
					}
				}
			}
		}

		return result;
	}
}
