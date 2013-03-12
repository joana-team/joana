/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import edu.kit.joana.ifc.sdg.graph.PDGs;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.I2PBackward;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.Nanda;
import edu.kit.joana.ifc.sdg.graph.slicer.conc.nanda.NandaFactory;


public class HotSpots {
	static double[] i2pSize = new double[100];
	static double[] i2pTime = new double[100];
	static double[] nSize = new double[100];
	static double[] nTime = new double[100];
	static double[] sizeRatio = new double[100];

	public static void main(String[] args) throws IOException, IllegalArgumentException {

		for (int nr = 12; nr < 23; nr++) {
			String file = PDGs.pdgs[nr];
	        System.out.println("***********************");
	        System.out.println(file);
			SDG g = SDG.readFrom(file);

	        System.out.println("initializing the slicers");
	        I2PBackward i2p = new I2PBackward(g);
	        Nanda n = NandaFactory.createNandaBackward(g);

	        System.out.println("collecting slicing criteria");
			List<SDGNode> criteria = g.getNNodes(100, 15);

			System.out.println("slicing");
			int ctr = 0;

			for (SDGNode node : criteria) {
				ctr++;
				try {
					long time = System.currentTimeMillis();
					Collection<SDGNode> slice = i2p.slice(node);
					time = System.currentTimeMillis() - time;
					i2pSize[ctr-1] = slice.size();
					i2pTime[ctr-1] = time;

					time = System.currentTimeMillis();
					slice = n.slice(node);
					time = System.currentTimeMillis() - time;
					nSize[ctr-1] = slice.size();
					nTime[ctr-1] = time;

				} catch(RuntimeException ex) { }

			    System.out.print(".");
			    
				if (ctr % 10 == 0) {
					System.out.print(ctr);
				}
				if (ctr % 100 == 0) {
					System.out.println();
				}
			}

			System.out.println("results");
			// compute time ratio
			timeRatio(i2pTime);
			timeRatio(nTime);
			// compute gain of precision
			for (int i = 0; i < 100; i++) {
				BigDecimal myDec = new BigDecimal((nSize[i] / i2pSize[i]) * 100.0);
				myDec = myDec.setScale( 2, BigDecimal.ROUND_HALF_UP );
				sizeRatio[i] = myDec.doubleValue();
			}
			System.out.println("I2P time behaviour:");
			for (int i = 0; i < 100; i++) {
				System.out.println(i2pTime[i]);
			}
			System.out.println("-------------------");
			System.out.println("N time behaviour:");
			for (int i = 0; i < 100; i++) {
				System.out.println(nTime[i]);
			}
			System.out.println("-------------------");
			System.out.println("size ratio");
			for (int i = 0; i < 100; i++) {
				System.out.println(sizeRatio[i]);
			}
		}
	}

	private static void timeRatio(double[] raw) {
		double totalTime = 0.0;

		for (int i = 0; i < 100; i++) {
			totalTime += raw[i];
		}

		for (int i = 0; i < 100; i++) {
			BigDecimal myDec = new BigDecimal((raw[i] / totalTime) * 100.0);
			myDec = myDec.setScale( 2, BigDecimal.ROUND_HALF_UP );
			raw[i] = myDec.doubleValue();
		}
	}
}
