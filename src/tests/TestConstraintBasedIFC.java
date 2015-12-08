package tests;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.wala.constraints.ConstraintBasedIFC;


public class TestConstraintBasedIFC {

	public static void main(String[] args) throws IOException {
		SDG sdg = SDG.readFrom("example.pdg");
		SDGNode source1 = sdg.getNode(5); // read SECRET
		SDGNode source2 = sdg.getNode(13); // read MID_A
		SDGNode source3 = sdg.getNode(21); // read MID_A
		SDGNode sink1 = sdg.getNode(19); // write MID_B
		SDGNode sink2 = sdg.getNode(27); // write MID_B
		SDGNode sink3 = sdg.getNode(35); // write PUBLIC
		Map<SDGNode, String> sourceAnnotations = new HashMap<SDGNode, String>();
		sourceAnnotations.put(source1, BuiltinLattices.STD_SECLEVEL_HIGH);
		sourceAnnotations.put(source2, BuiltinLattices.STD_SECLEVEL_DIAMOND_A);
		sourceAnnotations.put(source3, BuiltinLattices.STD_SECLEVEL_DIAMOND_A);
		Map<SDGNode, String> sinkAnnotations = new HashMap<SDGNode, String>();
		sinkAnnotations.put(sink1, BuiltinLattices.STD_SECLEVEL_DIAMOND_A);
		sinkAnnotations.put(sink2, BuiltinLattices.STD_SECLEVEL_DIAMOND_B);
		sinkAnnotations.put(sink3, BuiltinLattices.STD_SECLEVEL_LOW);

		ConstraintBasedIFC<String> ifc = new ConstraintBasedIFC<String>(sdg, sourceAnnotations, sinkAnnotations, BuiltinLattices.getDiamondLattice());
		ifc.computeClassification();
		if (ifc.isForwardClassificationCompatibleWithSinkAnnotation()) {
			System.out.println("Forward classification is compatible with sink annotation.");
		} else {
			System.out.println("Forward classification is incompatible with sink annotation. Forward violations:");
			for (SDGNode n : ifc.getNodesWithForwardViolation()) {
				System.out.println(n + " --> propagated from sources: " + ifc.getForwardClassification(n) + " / annotated as sink: " + sinkAnnotations.get(n));
			}
		}
		if (ifc.isBackwardClassificationCompatibleWithSourceAnnotation()) {
			System.out.println("Backward classification is compatible with source annotation.");
		} else {
			System.out.println("Backward classification is incompatible with sink annotation. Backward violations:");
			for (SDGNode n : ifc.getNodesWithBackwardViolation()) {
				System.out.println(n + " --> propagated from sinks: " + ifc.getBackwardClassification(n) + " / annotated as source: " + sourceAnnotations.get(n));
			}
		}

		for (SDGNode n : sdg.vertexSet()) {
			System.out.println(n + ": " + ifc.getForwardClassification(n) + "/" + ifc.getBackwardClassification(n));
		}
	}

}
