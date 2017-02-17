/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.util.graph.io.dot;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.VirtualNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadRegion;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;


/**
 * Convenience class which provides the possibility to export a number of different kinds of Graphs to the dot format
 */
public final class MiscGraph2Dot {

	private MiscGraph2Dot() {}

	public static <V, E> void export(final Graph<V, E> graph, final DOTExporter<V, E> exporter, final String filename)
			throws FileNotFoundException {
		final PrintWriter pw = new PrintWriter(filename);
		exporter.export(pw, graph);
	}

	public static <V, E> DOTExporter<V, E> genericExporter() {
		return new DOTExporter<V, E>(new VertexNameProvider<V>() {
			private final TObjectIntHashMap<V> id = new TObjectIntHashMap<V>();
			private int maxID = 0;

			private int getID(final V vn) {
				int ret;
				if (id.containsKey(vn)) {
					ret = id.get(vn);
				} else {
					id.put(vn, maxID);
					ret = maxID;
					maxID++;
				}
				return ret;
			}

			@Override
			public String getVertexName(final V ti) {
				return Integer.toString(getID(ti));
			}

		}, new VertexNameProvider<V>() {

			@Override
			public String getVertexName(final V ti) {
				return ti.toString();
			}

		}, new EdgeNameProvider<E>() {

			@Override
			public String getEdgeName(final E e) {
				return "";
			}

		});
	}

	public static DOTExporter<ThreadInstance, DefaultEdge> tctExporter() {
		return new DOTExporter<ThreadInstance, DefaultEdge>(new VertexNameProvider<ThreadInstance>() {

			@Override
			public String getVertexName(final ThreadInstance ti) {
				return Integer.toString(ti.getId());
			}

		}, new VertexNameProvider<ThreadInstance>() {

			@Override
			public String getVertexName(final ThreadInstance ti) {
				return Integer.toString(ti.getId());
			}

		}, new EdgeNameProvider<DefaultEdge>() {

			@Override
			public String getEdgeName(final DefaultEdge e) {
				return "";
			}

		});
	}

	public static DOTExporter<VirtualNode, SDGEdge> threadGraphExporter() {
		return new DOTExporter<VirtualNode, SDGEdge>(new VertexNameProvider<VirtualNode>() {

			private final TObjectIntHashMap<VirtualNode> id = new TObjectIntHashMap<VirtualNode>();
			private int maxID = 0;

			private int getID(final VirtualNode vn) {
				int ret;
				if (id.containsKey(vn)) {
					ret = id.get(vn);
				} else {
					id.put(vn, maxID);
					ret = maxID;
					maxID++;
				}
				return ret;
			}

			@Override
			public String getVertexName(final VirtualNode vn) {
				return Integer.toString(getID(vn));
			}

		}, new VertexNameProvider<VirtualNode>() {

			@Override
			public String getVertexName(final VirtualNode ti) {
				return String.format("(%d, %d)", ti.getNode().getId(), ti.getNumber());
			}

		}, new EdgeNameProvider<SDGEdge>() {

			@Override
			public String getEdgeName(final SDGEdge e) {
				return e.getKind().toString();
			}

		});
	}

	public static DOTExporter<Integer, DefaultEdge> standardExporter() {
		return new DOTExporter<Integer, DefaultEdge>(new VertexNameProvider<Integer>() {

			@Override
			public String getVertexName(final Integer tr) {
				return Integer.toString(tr);
			}

		}, new VertexNameProvider<Integer>() {

			@Override
			public String getVertexName(final Integer tr) {
				return Integer.toString(tr);
			}

		}, new EdgeNameProvider<DefaultEdge>() {

			@Override
			public String getEdgeName(final DefaultEdge e) {
				return "";
			}

		});
	}

	public static DOTExporter<SDGNode, SDGEdge> joanaGraphExporter() {
		return new DOTExporter<SDGNode, SDGEdge>(new VertexNameProvider<SDGNode>() {

			@Override
			public String getVertexName(final SDGNode tr) {
				return Integer.toString(tr.getId());
			}

		}, new VertexNameProvider<SDGNode>() {

			@Override
			public String getVertexName(final SDGNode tr) {
				return Integer.toString(tr.getId()) + " " + tr.getKind();
			}

		}, new EdgeNameProvider<SDGEdge>() {

			@Override
			public String getEdgeName(final SDGEdge e) {
				return "";
			}

		});
	}
	
	public static DOTExporter<VirtualNode, DefaultEdge> cdomTreeExporter() {
		return new DOTExporter<VirtualNode, DefaultEdge>(new VertexNameProvider<VirtualNode>() {

			@Override
			public String getVertexName(final VirtualNode tr) {
				return "id"+tr.getNode()+"nr"+tr.getNumber();
			}

		}, new VertexNameProvider<VirtualNode>() {

			@Override
			public String getVertexName(final VirtualNode tr) {
				return tr.toString();
			}

		}, new EdgeNameProvider<DefaultEdge>() {

			@Override
			public String getEdgeName(final DefaultEdge e) {
				return "";
			}

		});
	}

	public static <E> DOTExporter<Set<ThreadRegion>, E> regionClusterGraphExporter() {
		return new DOTExporter<Set<ThreadRegion>, E>(new VertexNameProvider<Set<ThreadRegion>>() {
			private final TObjectIntMap<Set<ThreadRegion>> id = new TObjectIntHashMap<Set<ThreadRegion>>();
			private int maxID = 0;

			int getID(final Set<ThreadRegion> x) {
				if (id.containsKey(x)) {
					return id.get(x);
				}

				id.put(x, maxID);
				final int ret = maxID;
				maxID++;
				return ret;
			}

			@Override
			public String getVertexName(final Set<ThreadRegion> tr) {
				return Integer.toString(getID(tr));
			}
		}, new VertexNameProvider<Set<ThreadRegion>>() {

			@Override
			public String getVertexName(final Set<ThreadRegion> tr) {
				return tr.toString();
			}

		}, new EdgeNameProvider<E>() {

			@Override
			public String getEdgeName(final E e) {
				return "";
			}

		});
	}

	public static <E> DOTExporter<ThreadRegion, E> regionGraphExporter() {
		return new DOTExporter<ThreadRegion, E>(new VertexNameProvider<ThreadRegion>() {
			@Override
			public String getVertexName(final ThreadRegion tr) {
				return Integer.toString(tr.getID());
			}
		}, new VertexNameProvider<ThreadRegion>() {

			@Override
			public String getVertexName(final ThreadRegion tr) {
				return tr.toString();
			}

		}, new EdgeNameProvider<E>() {

			@Override
			public String getEdgeName(final E e) {
				return "";
			}

		});
	}

}
