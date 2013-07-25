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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.core.sdgtools.SDGTools;
import edu.kit.joana.ifc.sdg.core.violations.ClassifiedViolation;
import edu.kit.joana.ifc.sdg.core.violations.IIllegalFlow;
import edu.kit.joana.ifc.sdg.core.violations.paths.PathGenerator;
import edu.kit.joana.ifc.sdg.core.violations.paths.ViolationPathes;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;
import edu.kit.joana.ifc.sdg.lattice.WrongLatticeDefinitionException;


/**
 *
 * @author nodes
 *
 * There are 4 types of nodes:
 * Outgoing: means node has only incoming class, no outgoing -> starting points for slices
 * Annotated: means node has only outgoing class, no incoming
 * Declassifing (~Redefining): means node allows incoming information of incoming class and outgoing information of outgoing class
 * UnAnnotated: means node allows every incoming class as long as its smaller or equal to outoing class
 *
 * Incoming Class: No Information from a higher class is allowed to flow in
 * Outoing Class: Information of this class is flowing out of that node
 *
 * Security SimpleViolation NodeA ---> NodeB:
 * NodeB's Incoming Class is lower than NodeA's Outgoing Class
 *
 */
public class InterFlowChecker7 extends IFC {
	private static final boolean TIME = false;
	private static final boolean TIME_VIO = false;

	public InterFlowChecker7(SDG sdg, IStaticLattice<String> lattice) {
		super(sdg, lattice);
	}

	public InterFlowChecker7(File sdg, File lattice) throws WrongLatticeDefinitionException, IOException {
		super(sdg, lattice);
	}


	/**
	 * Determines security violations by applying slicing to all 'outgoing' and
	 * 'declassification' nodes in this InterFlowCheckers Graph(=sdgParser) g
	 * Basically calls <i>checkIFlow(false)</i>
	 * @return List of violations: List&lt;Violation&gt;
	 * @throws InterSlicePluginException
	 * @throws NotInLatticeException
	 */
	public Collection<ClassifiedViolation> checkIFlow() throws NotInLatticeException {
		return checkIFlow(false);
	}


	/**
	 * Determines security violations by applying slicing to all 'outgoing' and
	 * 'declassification' nodes in this InterFlowCheckers Graph(=sdgParser) g
	 * @return List of violations: List<Violation>
	 * @throws InterSlicePluginException
	 * @throws NotInLatticeException
	 */

	@SuppressWarnings("deprecation")
	public Collection<ClassifiedViolation> checkIFlow(boolean generateVioPathes) throws NotInLatticeException {
		Collection<ClassifiedViolation> ret = new LinkedList<ClassifiedViolation>(); //list to be returned

		// compute declassification information
		DeclassificationSummaryNodes dec = new DeclassificationSummaryNodes(g, l);
		dec.slice();

		// get all outgoing nodes...
		Collection<SecurityNode> outgoingNodes = SDGTools.getInformationSinks(g);
		// ...add all declassification nodes...
		outgoingNodes.addAll(SDGTools.getDeclassificationNodes(g));

        IFCSlicer is = new IFCSlicer(l, g);

		//...and do slicing for each of these nodes
		for (SecurityNode temp : outgoingNodes) {
			long slicestart = System.currentTimeMillis();
			if (TIME) System.out.println("Started slicing at " + slicestart);

			// NODES-KLEINOD: nicht loeschen :D
			//((ProgressAnnouncer) is).addProgressListener(this);

			// do slicing
			Collection<ClassifiedViolation> violations = is.checkIFC(temp);

			long sliceend = System.currentTimeMillis();
			if (TIME) System.out.println("Finished slicing at " + sliceend + " | slice duration: " + (sliceend-slicestart));

			//Transform SimpleViolations into Violations
			for (ClassifiedViolation vp : violations) {
				ClassifiedViolation vio = ClassifiedViolation.createViolation(temp, vp.getSink(), temp.getRequired());
				ret.add(vio);
			}

			// add ViolationPathes if wished
			if (generateVioPathes) {
				ret = addViolationPathesChop(ret);
			}
		}

		//this.progressChanged("Done", 100, 100);
		return ret;
	}

	/**
	 * @param ret
	 * @param temp
	 * @param violations
	 * @throws NotInLatticeException
	 */
	public Collection<ClassifiedViolation> addViolationPathesChop(Collection<ClassifiedViolation> violations)
	throws NotInLatticeException {
		long viostart = System.currentTimeMillis();
		if (TIME_VIO) System.out.println("Started viopathgen at " + viostart + " for " + violations.size() + " violations");

		LinkedList<ClassifiedViolation> ret = new LinkedList<ClassifiedViolation>();
		PathGenerator vpg = new PathGenerator(g);
		vpg.addProgressListener(this);

		//merge all violations into return list
		for (ClassifiedViolation sViolation : violations) {

			// Generate ViolationPathes and attach them to violation nodes
			ViolationPathes vps = vpg.computePaths(sViolation);
            ClassifiedViolation vio = ClassifiedViolation.createViolation(sViolation.getSink(), sViolation.getSource(), vps, sViolation.getSink().getRequired());
			ret.add(vio);
		}

		long vioend = System.currentTimeMillis();
		if (TIME_VIO) System.out.println("Ended viopathgen at " + vioend + " duration: " + (vioend - viostart));
		return ret;
	}

	/**
	 * Accepts and interprets parameters from command line and
	 * displays results
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("Start " + System.currentTimeMillis());
		File latticefile = null;
		File pdgfile = null;


		/*** comment that block out for debugging and testing ****
		if (args.length == 0) {
			System.out.println("use 'java NJSec.parsing.IFlowCheck -h' for some help");
			System.exit(0);
		}
		/********************************************************/



		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-lattice")) {
				latticefile = new File(args[i+1]);

			} else if (args[i].equals("-pdg")) {
				pdgfile = new File(args[i+1]);
			} else if (args[i].equals("-h") || args[i].equals("--help")) {
				System.out.println("use: java NJSec.parsing.IFlowCheck -lattice <latticefile> -pdg <pdgfile>");
				System.out.println("example: java NJSec.parsing.IFlowCheck -lattice IPIFlow.lat -pdg IPIFlow.pdg");

				System.exit(0);
				//    		} else if (args[i].equals("-debug")) {
				//    			debug = true;                          //not used at the moment
			}
		}

		/*** some code for debugging and testing **********/
		//    	latticefile = new File ("Kalibrierung.lat");
		//    	stanclass = "standard";
		//    	outstanclass = "standard";
		//    	pdgfile = new File("NJSec\\pdgs\\KalibrierungOK.pdg");

		latticefile = new File ("bin\\de\\naxan\\NJSec\\iflowchecking\\test\\IPIFlow.lat");
		pdgfile = new File("bin\\de\\naxan\\NJSec\\iflowchecking\\test\\IPIFlow.pdg");
		//    	System.out.println(latticefile.getAbsolutePath());
		/*******/


		if (!latticefile.exists()) System.err.println("Error: lattice-file doesn't exist");
		if (!pdgfile.exists()) System.err.println("Error: SDG-file doesn't exist");
		if (!latticefile.exists() || !pdgfile.exists()) System.exit(1);

		IStaticLattice<String> l = null;

		try {
			l = LatticeUtil.compileBitsetLattice(new FileInputStream(latticefile));

		} catch (IOException e) {
			System.out.println("Couldn't read given file " + latticefile + "\n" + e.toString());

		} catch (WrongLatticeDefinitionException e) {
			System.out.println("Invalid lattice defintion in file: " + latticefile + "\n" + e.toString());
		}

		SDG g = null;
		try {
			g = SDG.readFrom(new FileReader(pdgfile));

		} catch (FileNotFoundException e1) {
			System.out.println("Couldn't find given file " + pdgfile + "\n" + e1.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}

		InterFlowChecker7 ifc = new InterFlowChecker7(g, l);
		System.out.println("Starte ViolationCheck " + System.currentTimeMillis());
		Collection<ClassifiedViolation> violations = ifc.checkIFlow(true);
		System.out.println("Start Output " + System.currentTimeMillis());
		System.out.println(toText(violations));

	}

	/**
	 * Generates textual, human-readable representation for given violations
	 * @param violations<Violation>
	 * @return String containing textual representation for violations
	 */
	public static String toText(Collection<ClassifiedViolation> violations) {
		StringBuffer ret = new StringBuffer();
		if (violations.size() == 0) {
			ret.append("No iflow violations found.");
		} else {
			int i = 0;
			for (ClassifiedViolation violation : violations) {
				i++;
				SecurityNode outgoing = violation.getSink();
				SecurityNode from = violation.getSource();
				ViolationPathes violationPathes = violation.getViolationPathes();

				ret.append(i + ": There's Information flowing out at \n" +
						"   IFlow Allowed  : " + outgoing.getRequired() + " - Node " + outgoing + " - " +
						outgoing.getOperation() + " - " + outgoing.getLabel() +
						" - " + outgoing.getSource() + " - Row:" + outgoing.getSr() + "\n");
				ret.append("from \n" +
						"   IFlow Annotated: " + from.getProvided() + " - Node " + from + " - " +
						from.getOperation() + " - " + from.getLabel() +
						" - " + from.getSource() + " - Row:" + from.getSr() + "\n");


				if (violationPathes == null) {
					ret.append("no pathes available");
				} else {
					ret.append(violationPathes.toString());
				}
				ret.append("---\n");

			}
		}
		return ret.toString();
	}
}
