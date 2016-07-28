package edu.kit.joana.ifc.sdg.irlsod;

import java.util.HashMap;
import java.util.Map;

import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;

/**
 * Abstract base class for IFC checker that require  user annotation represented as Map from SDGNodes to L,
 * rather than corresponding field in SecurityNodes.
 * 
 * Use inferUserAnnotationsOnDemand() to infer such a map from SecurityNodes.
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public abstract class AnnotationMapChecker<L> extends IFC<L> {
	/** user-provided annotations */
	protected Map<SDGNode, L> userAnn;
	/**
	 * 
	 * @param sdg An SDG
	 * @param secLattice A security lattice
	 * @param userAnn user Annotations. May be null, in which case subclasses are expected to call
	 *        inferUserAnnotationsOnDemand().
	 */
	AnnotationMapChecker(final SDG sdg, final IStaticLattice<L> secLattice, final Map<SDGNode, L> userAnn) {
		super(sdg, secLattice);
		this.userAnn = userAnn;
	}
	
	/**
	 * Infer userAnn from a SDG equipped with SecurityNodes.
	 */
	protected void inferUserAnnotationsOnDemand() {
		if (userAnn != null) return;
		
		final SDG sdg = this.getSDG();
		final IStaticLattice<L> secLattice = this.getLattice();
		
		userAnn = new HashMap<>();
		for (final SDGNode n : sdg.vertexSet()) {
			if (n instanceof SecurityNode) {
				final SecurityNode sn = (SecurityNode) n;
				final String req = sn.getRequired();
				if (req != null && !req.equals(SecurityNode.UNDEFINED)) {
					for (final L elem : secLattice.getElements()) {
						if (req.equals(elem.toString())) {
							userAnn.put(n, elem);
						}
					}
				}
				final String prov = sn.getProvided();
				if (prov != null && !prov.equals(SecurityNode.UNDEFINED)) {
					for (final L elem : secLattice.getElements()) {
						if (prov.equals(elem.toString())) {
							userAnn.put(n, elem);
						}
					}
				}
			}
		}
	}

}
