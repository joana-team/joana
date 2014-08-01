/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.flowless.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.labeled.AbstractNumberedLabeledGraph;
import com.ibm.wala.util.graph.labeled.NumberedLabeledEdgeManager;
import com.ibm.wala.util.graph.labeled.SparseNumberedLabeledEdgeManager;

import edu.kit.joana.wala.flowless.pointsto.PtsParameter;
import edu.kit.joana.wala.flowless.pointsto.AliasGraph.MayAliasGraph;
import edu.kit.joana.wala.flowless.pointsto.PtsParameter.RootParameter;
import edu.kit.joana.wala.util.ParamNum;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

public final class AliasGraphIO {

	private AliasGraphIO() {}

	public static void writeToFile(final MayAliasGraph g, final String file) throws FileNotFoundException {
		final PrintWriter pw = new PrintWriter(file);

		writeOut(g, pw);

		pw.close();
	}

	public static void writeOut(final MayAliasGraph g, final OutputStream out) {
		final PrintWriter pw = new PrintWriter(out);

		writeOut(g, pw);

		pw.close();
	}

	public static void writeOut(final MayAliasGraph g, final PrintWriter pw) {
		pw.println(g.isStaticMethod());
		pw.println(g.getRoots().size());
		for (RootParameter p : g.getRoots()) {
			writeOut(p, pw);
		}

		List<Edge> edges = new LinkedList<Edge>();

		for (PtsParameter p : g) {
			for (Iterator<PtsParameter> it = g.getSuccNodes(p); it.hasNext();) {
				final PtsParameter succ = it.next();
				edges.add(new Edge(p.id, succ.id));
			}
		}

		pw.println(edges.size());

		for (Edge e : edges) {
			pw.print(e.from);
			pw.print(DELIM);
			pw.println(e.to);
		}

		pw.flush();
	}

	public final static int ROOT_PARAMETER = -2;
	public final static int OBJECT_FIELD = -4;
	public final static int ARRAY_FIELD = -5;

	public static String DELIM = "&";

	private static void writeOut(final RootParameter p, final PrintWriter pw) {
		pw.print(ROOT_PARAMETER);
		pw.print(DELIM);
		pw.print(p.getParamNum().getNum());
		pw.print(DELIM);
		writeOutParam(p, pw);
	}

	private static void writeOutParam(final PtsParameter p, final PrintWriter pw) {
		pw.print(p.getName());
		pw.print(DELIM);
		pw.print(p.id);
		pw.print(DELIM);
		pw.print(p.getType().getName().toString());
		pw.print(DELIM);
		final int numCh = p.getChildren().size();
		pw.println(numCh);
		for (PtsParameter ch : p.getChildren()) {
			if (ch instanceof PtsParameter.NormalFieldParameter) {
				pw.print(OBJECT_FIELD);
				pw.print(DELIM);
			} else if (ch instanceof PtsParameter.ArrayFieldParameter) {
				pw.print(ARRAY_FIELD);
				pw.print(DELIM);
			} else {
				throw new IllegalStateException();
			}

			writeOutParam(ch, pw);
		}
	}

	public static MayAliasGraph readIn(final String file) throws FileNotFoundException {
		final Scanner in = new Scanner(new File(file));

		return readIn(in);
	}

	public static MayAliasGraph readIn(final InputStream is) {
		final Scanner in = new Scanner(is);

		return readIn(in);
	}

	private static String DELIM_PATTERN = "[\\s&]";

	public static MayAliasGraph readIn(Scanner in) {
		in = in.useDelimiter(DELIM_PATTERN);

		final boolean isStaticMethod = in.nextBoolean();
		
		final MayAliasGraph g = new MayAliasGraph(isStaticMethod);

		final int numOfNodes = in.nextInt();
		for (int i = 0; i < numOfNodes; i++) {
			readInRootParam(g, in);
		}

		final int numOfEdges = in.nextInt();

		List<Edge> edges = new LinkedList<Edge>();
		for (int i = 0; i < numOfEdges; i++) {
			final int from = in.nextInt();
			final int to = in.nextInt();
			final Edge e = new Edge(from, to);
			edges.add(e);
		}

		final TIntIntMap id2graph = new TIntIntHashMap(numOfNodes);
		for (final PtsParameter p : g) {
			id2graph.put(p.id, p.getGraphNodeId());
		}

		for (final Edge e : edges) {
			final int fromId = id2graph.get(e.from);
			final int toId = id2graph.get(e.to);
			final PtsParameter from = g.getNode(fromId);
			final PtsParameter to = g.getNode(toId);

			g.addEdge(from, to);
		}

		return g;
	}

	private static RootParameter readInRootParam(final MayAliasGraph g, final Scanner in) {
		final int type = in.nextInt();
		if (type != ROOT_PARAMETER) {
			throw new IllegalStateException("expected a root parameter (" + ROOT_PARAMETER + ") but found " + type);
		}

		final RootParameter p = (RootParameter) readInParam(g, in, type, null);

		return p;
	}

	private static PtsParameter readInParam(final MayAliasGraph g, final Scanner in, final int paramType, final PtsParameter parent) {
		final ParamNum paramNum;

		if (paramType == ROOT_PARAMETER) {
			final int pNum = in.nextInt();
			paramNum = ParamNum.fromParamNum(g.isStaticMethod(), pNum);
		} else {
			paramNum = null;
		}

		final String name = in.next();
		final int nodeId = in.nextInt();
		final String typeName = in.next();
		final int numChildren = in.nextInt();

		PtsParameter current;
		switch (paramType) {
		case ROOT_PARAMETER:
			current = PtsParameter.RootParameter.create(nodeId, name, typeName, paramNum);
			break;
		case ARRAY_FIELD:
			current = PtsParameter.ArrayFieldParameter.create(nodeId, parent, typeName);
			break;
		case OBJECT_FIELD:
			current = PtsParameter.NormalFieldParameter.create(nodeId, parent, name, typeName);
			break;
			//break;
		default:
			throw new IllegalArgumentException("Expected object (" + OBJECT_FIELD + "), array ("
				+ ARRAY_FIELD + "), root (" + ROOT_PARAMETER + ") field parameter, but found: " + paramType);
		}

		g.addNode(current);

		for (int i = 0; i < numChildren; i++) {
			final int childType = in.nextInt();
			switch (childType) {
			case OBJECT_FIELD:
			case ARRAY_FIELD:
				// all ok
				break;
			default:
				throw new IllegalStateException("Expected object (" + OBJECT_FIELD + ") or array ("
						+ ARRAY_FIELD + ") field parameter, but found: " + childType);
			}

			// new child is added automagically to parent, so we do not need the return value here.
			readInParam(g, in, childType, current);
		}

		return current;
	}

	private static class Edge {
		public final int from;
		public final int to;

		public Edge(int from, int to) {
			this.from = from;
			this.to = to;
		}
	}

	private final static PtsNodeDecorator PTS_NODE_DEC = new PtsNodeDecorator();
	private static class PtsNodeDecorator extends ExtendedNodeDecorator.DefaultImpl<PtsParameter> {
		public String getLabel(PtsParameter pts) {
			return pts.getName();
		}
	}

	private static final PtsEdgeDecorator PTS_EDGE_DEC = new PtsEdgeDecorator();
	private static class PtsEdgeDecorator implements EdgeDecorator {

		@Override
		public String getLabel(Object o) throws WalaException {
			if (o instanceof EdgeType) {
				switch ((EdgeType) o) {
				case ALIAS:
					return "";
				case STRUCTURE:
					return "";
				}
			}

			return EdgeDecorator.DEFAULT.getLabel(o);
		}

		@Override
		public String getStyle(Object o) throws WalaException {
			if (o instanceof EdgeType) {
				switch ((EdgeType) o) {
				case ALIAS:
					return "solid";
				case STRUCTURE:
					return "dotted";
				}
			}

			return EdgeDecorator.DEFAULT.getStyle(o);
		}

		@Override
		public String getColor(Object o) throws WalaException {
			if (o instanceof EdgeType) {
				switch ((EdgeType) o) {
				case ALIAS:
					return "black";
				case STRUCTURE:
					return "blue";
				}
			}

			return EdgeDecorator.DEFAULT.getColor(o);
		}

	}

	private static enum EdgeType { ALIAS, STRUCTURE }
	private static class LabeledAliasGraph extends AbstractNumberedLabeledGraph<PtsParameter, EdgeType> {

		private final MayAliasGraph delegate;
		private final NumberedLabeledEdgeManager<PtsParameter, EdgeType> edges;

		private LabeledAliasGraph(final MayAliasGraph delegate) {
			this.delegate = delegate;
			this.edges = new SparseNumberedLabeledEdgeManager<PtsParameter, AliasGraphIO.EdgeType>(delegate, EdgeType.ALIAS);

			for (final PtsParameter p : delegate) {
				for (final Iterator<PtsParameter> it = delegate.getSuccNodes(p); it.hasNext();) {
					PtsParameter next = it.next();
					edges.addEdge(p, next, EdgeType.ALIAS);
				}

				if (p.hasChildren()) {
					for (final PtsParameter child : p.getChildren()) {
						edges.addEdge(p, child, EdgeType.STRUCTURE);
					}
				}
			}
		}

		@Override
		protected NumberedLabeledEdgeManager<PtsParameter, EdgeType> getEdgeManager() {
			return edges;
		}

		@Override
		protected NumberedNodeManager<PtsParameter> getNodeManager() {
			return delegate;
		}

	}

	public static void dumpToDot(final MayAliasGraph g, final String filename) {
		LabeledAliasGraph lg = new LabeledAliasGraph(g);

		System.out.println("Writing to file '" + filename + "'");

		try {
			DotUtil.dotify(lg, lg, PTS_NODE_DEC, PTS_EDGE_DEC, filename, NullProgressMonitor.INSTANCE, false);
		} catch (WalaException e) {
			e.printStackTrace();
		} catch (CancelException e) {
			e.printStackTrace();
		}
	}
}
