/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.eval;

import java.util.Collection;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.chopper.FixedPointChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.InsensitiveIntersectionChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.IntersectionChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.NonSameLevelChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.Opt1Chopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.RepsRosayChopperUnopt;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.AlmostTimeSensitiveThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ContextSensitiveThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.SimpleThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.ThreadChopper;
import edu.kit.joana.ifc.sdg.graph.chopper.conc.VerySimpleThreadChopper;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextInsensitiveBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.ContextSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.IPDGSlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.SummarySlicerBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaFactory;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.simple.SimpleConcurrentSlicer;


public interface Algorithm {
	public enum Kind {
        SLICER("S"),
        /** An edge connecting a folded node with its fold node. */
        CHOPPER("C");

        private final String value;

        private Kind(String s) {
        	value = s;
        }

        public String toString() {
            return value;
        }
	}

	public enum Algo {
		/* chopper */
		INTERSECTION_CHOPPER("IC", Kind.CHOPPER, false) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new InsensitiveIntersectionChopper(null));
			}
		},
		OPT_0("Opt-0", Kind.CHOPPER, false) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new IntersectionChopper(null));
			}
		},
		OPT_1("Opt-1", Kind.CHOPPER, false) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new Opt1Chopper(null));
			}
		},
		FIXPOINT_CHOPPER("FC", Kind.CHOPPER, false) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new FixedPointChopper(null));
			}
		},
		RRC("RRC", Kind.CHOPPER, false) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new RepsRosayChopper(null));
			}
		},
		RRC_SMC("RRC_SMC", Kind.CHOPPER, false) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new NonSameLevelChopper(null));
			}
		},
		RRC_UNOPT("RRC_UNOPT", Kind.CHOPPER, false) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new RepsRosayChopperUnopt(null));
			}
		},


		CONC_INTERSECTION_CHOPPER("CIC", Kind.CHOPPER, true) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new VerySimpleThreadChopper(null));
			}
		},
		ITERATED_TWO_PHASE_CHOPPER("I2PC", Kind.CHOPPER, true) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new SimpleThreadChopper(null));
			}
		},
		CONC_FIXPOINT_CHOPPER("CFC", Kind.CHOPPER, true) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new edu.kit.joana.ifc.sdg.graph.chopper.conc.FixedPointChopper(null));
			}
		},
		CONTEXT_SENSITIVE_CHOPPER("CSC", Kind.CHOPPER, true) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new ContextSensitiveThreadChopper(null));
			}
		},
		ALMOST_TMIE_SENSITIVE_CHOPPER("ATSC", Kind.CHOPPER, true) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new AlmostTimeSensitiveThreadChopper(null));
			}
		},
		TIME_SENSITIVE_CHOPPER("TSC", Kind.CHOPPER, true) {
			public Algorithm instantiate() {
				return new ChoppingAlgorithm(new ThreadChopper(null));
			}
		},


		/* slicer */
		INTERSECTION_SLICER("IS", Kind.SLICER, false) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(new ContextInsensitiveBackward(null));
			}
		},
		TWO_PHASE_SLICER("2P", Kind.SLICER, false) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(new SummarySlicerBackward(null));
			}
		},
		CONTEXT_SLICER("CS", Kind.SLICER, false) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(new ContextSlicerBackward(null, true));
			}
		},
		IPDG_SLICER("IPDG", Kind.SLICER, false) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(IPDGSlicerBackward.newIPDGSlicerBackward(null, true));
			}
		},
		IDYN_SLICER("IDYN", Kind.SLICER, false) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(IPDGSlicerBackward.newIPDGSlicerBackward(null, false));
			}
		},


		ITERATED_TWO_PHASE_SLICER("I2P", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(new I2PBackward(null));
			}
		},
		SIMPLE_CONCURRENT_SLICER("S", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(new SimpleConcurrentSlicer());
			}
		},
		KRINKE_SLICER("K", Kind.SLICER, true) {
			@SuppressWarnings("deprecation")
			public Algorithm instantiate() {
				return new SlicingAlgorithm(new edu.kit.joana.ifc.sdg.graph.slicer.conc.krinke.OptimizedKrinke());
			}
		},
		GIFFHORN_KRINKE("GK", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(new edu.kit.joana.ifc.sdg.graph.slicer.conc.dynamic.krinke.Slicer());
			}
		},
		NANDA("N", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions.NandaFactory.createNandaOriginalBackward(null));
			}
		},
		GIFFHORN_NANDA("GN", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(NandaFactory.createNandaBackward(null));
			}
		},
		GIFFHORN_NANDA_EXPERIMENTAL("GNE", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.experimental.NandaFactory.createNandaBackward(null));
			}
		},
		GIFFHORN_NANDA_NO_MHP("GNF", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(NandaFactory.createNandaFlatBackward(null));
			}
		},
		GIFFHORN_NANDA_REACH("GNR", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(NandaFactory.createNandaReachBackward(null));
			}
		},
		GIFFHORN_NANDA_THREAD_REGIONS("GNT", Kind.SLICER, true) {
			public Algorithm instantiate() {
				return new SlicingAlgorithm(edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.regions.NandaFactory.createNandaBackward(null));
			}
		};


	    private final String value;
	    private final Kind kind;
	    private final boolean isConcurrent;

	    Algo(String s, Kind k, boolean conc) {
	    	value = s;
	    	kind = k;
	    	isConcurrent = conc;
	    }

	    public String getValue() {
	    	return value;
	    }

	    public Kind getKind() {
	    	return kind;
	    }

	    public boolean isConcurrent() {
	    	return isConcurrent;
	    }

	    public abstract Algorithm instantiate();
	}


	Collection<SDGNode> run(Criterion crit);

	String getName();

	void setSDG(SDG g);
}
