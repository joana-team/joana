/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;


/**
 * 1. Copy the SDGs into a directory.
 * 2. Remove all threads from the thread information block, except for the main thread.
 * 3. Add the names of the SDG files to the pdgs-array.
 * 4. Run main().
 *
 * @author giffhorn
 *
 */
public class Sequentialize {
	static String[] pdgs = {
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Barcode.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Cellsafe.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.ac.AlarmClock.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.auto.EnvDriver.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.bb.ProducerConsumer.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.cliser.dt.Main.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.cliser.kk.Main.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.daisy.DaisyTest.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.dp.DiningPhilosophers.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.ds.DiskSchedulerDriver.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.lg.LaplaceGrid.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.sq.SharedQueue.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/conc.TimeTravel.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/GoldenSMS_KeyManagement.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/GoldenSMS_Message.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/GoldenSMS_Reception.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Guitar.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/HyperM.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/J2MESafe.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Logger.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Maza.pdg",
		"/afs/info.uni-karlsruhe.de/user/giffhorn/Desktop/pdgs/sequential/Podcast.pdg"
	};

	public static void main(String[] args) throws IOException {
		for (String file : pdgs) {
			SDG g = SDG.readFrom(file);
			propagateThreadIDs(g.getThreadsInfo(), g);
			convertConcurrencyEdges(g);

			String content = SDGSerializer.toPDGFormat(g);
	        File f = new File(file);
	        FileWriter w = new FileWriter(f);

	        w.write(content);
	        w.flush();
	        w.close();
		}
	}

	private static void propagateThreadIDs(ThreadsInformation ti, SDG graph) {
		if (ti.getNumberOfThreads() > 1) throw new IllegalArgumentException();

		int[] ids = new int[] {0};

		for (SDGNode n : graph.vertexSet()) {
			n.setThreadNumbers(ids);
		}
    }

	private static void convertConcurrencyEdges(SDG g) {
		LinkedList<SDGEdge> remove = new LinkedList<SDGEdge>();
		LinkedList<SDGEdge> add = new LinkedList<SDGEdge>();

		for (SDGEdge e : g.edgeSet()) {
			if (e.getKind() == SDGEdge.Kind.INTERFERENCE
					|| e.getKind() == SDGEdge.Kind.INTERFERENCE_WRITE
					|| e.getKind() == SDGEdge.Kind.JOIN
					|| e.getKind() == SDGEdge.Kind.SYNCHRONIZATION
					|| e.getKind() == SDGEdge.Kind.READY_DEP) {
				remove.add(e);

			} else if (e.getKind() == SDGEdge.Kind.FORK) {
				remove.add(e);
				add.add( SDGEdge.Kind.CALL.newEdge(e.getSource(), e.getTarget()));

			} else if (e.getKind() == SDGEdge.Kind.FORK_IN) {
				remove.add(e);
				add.add( SDGEdge.Kind.PARAMETER_IN.newEdge(e.getSource(), e.getTarget()));

			} else if (e.getKind() == SDGEdge.Kind.FORK_OUT) {
				remove.add(e);
				add.add( SDGEdge.Kind.PARAMETER_OUT.newEdge(e.getSource(), e.getTarget()));
			}
		}

		g.removeAllEdges(remove);
		g.addAllEdges(add);
	}
}
