/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.mhpoptimization;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;





public class MHPOptimization {

	public void runOn(String fileName){
		SDG sdg;
		try {
			sdg = readSDGFromFile(fileName);
		} catch (IOException e) {
			System.out.println("I/O error while reading sdg from file.");
			e.printStackTrace();
			return;
		}

		optimize(sdg);
		saveSDG(sdg, fileName+".opt");
	}

	public void optimize(SDG sdg)  {
		MHPAnalysis mhpAnalysis = CSDGPreprocessor.runMHP(sdg);
		//sdg.getThreadsInfo();
		if (mhpAnalysis != null) {
			cleanCSDG(sdg, mhpAnalysis);
		}
	}

	public static SDG readSDGFromFile(String sdgFile) throws IOException {
		Reader reader = new FileReader(sdgFile);
		return SDG.readFrom(reader);
	}

	public static void saveSDG(SDG sdg, String sdgFile) {
		try {
			SDGSerializer.toPDGFormat(sdg, new FileOutputStream(sdgFile));
		} catch (FileNotFoundException fnfe) {

		}
	}

	private void cleanCSDG(SDG graph, MHPAnalysis mhp) {
		LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
		int all = 0;
		int x = 0;

		for (SDGEdge e : graph.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.INTERFERENCE
					|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE) {
				all++;
				// not parallel
				if (!mhp.isParallel(e.getSource(), e.getTarget())) {
					remove.add(e);
					x++;
				}
			}
		}

		System.out.println("Will remove the following edges: " + remove);

		for (SDGEdge e : remove) {
			graph.removeEdge(e);
		}
	}

	public static void main(String[] args) {
		MHPOptimization mhpOpt = new MHPOptimization();
		//mhpOpt.runOn("/afs/info.uni-karlsruhe.de/user/mmohr/git/mojo/joana.ifcconsole/edu.kit.pp.joana.ifcconsole.toy.conc.ThreadTest1a.main.pdg");
		//mhpOpt.runOn("/afs/info.uni-karlsruhe.de/user/mmohr/git/mojo/joana.ifcconsole/edu.kit.pp.joana.ifcconsole.toy.conc.ThreadTest2.main.pdg");
		//mhpOpt.runOn("/afs/info.uni-karlsruhe.de/user/mmohr/git/mojo/joana.ifcconsole/edu.kit.pp.joana.ifcconsole.toy.conc.ThreadFool.main.pdg");
		mhpOpt.runOn("/afs/info.uni-karlsruhe.de/user/mmohr/git/mojo/joana.ifcconsole/edu.kit.pp.joana.ifcconsole.toy.conc.ThreadHierarchy.main.pdg");
		//mhpOpt.runOn("/afs/info.uni-karlsruhe.de/user/mmohr/git/mojo/joana.ifcconsole/edu.kit.pp.joana.ifcconsole.toy.conc.InterproceduralThreadTest.main.pdg");
	}

}
