/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.graph.EfficientGraph;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jgrapht.DirectedGraph;

import java.io.InputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.*;

/**
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class WorkPackage<G extends DirectedGraph<SDGNode, SDGEdge> & EfficientGraph<SDGNode, SDGEdge>> {

	public final static boolean SORT_SUMMARY_EDGES = false;

	public static class EntryPoint implements Comparable<EntryPoint> {
		private final int entryId;
		private final TIntCollection formalIns;
		private final TIntCollection formalOuts;
		private final TIntObjectHashMap<TIntList> formIn2out;
		private long summaryEdges = 0;

		public EntryPoint(int entryId, TIntCollection formIn, TIntCollection formOut) {
			this.entryId = entryId;
			this.formalIns = formIn;
			this.formalOuts = formOut;
			this.formIn2out = new TIntObjectHashMap<TIntList>(formIn.size());
		}

		public String toString() {
			long maxConnections = formalIns.size() * formalOuts.size();
			return "ENTRY(" + entryId + ")[in:" + formalIns.size() + "][out:" + formalOuts.size() +"][sum:" + summaryEdges
				+ (maxConnections > 0 ? " " + ((100 * summaryEdges) / maxConnections) + "%]" : " --%]" );
		}

		private void addSummaryDep(int formIn, int formOut) {
			if (formalIns.contains(formIn) && formalOuts.contains(formOut)) {
				TIntList outs = formIn2out.get(formIn);
				if (outs == null) {
					outs = new TIntArrayList();
					formIn2out.put(formIn, outs);
				}

				summaryEdges++;
				outs.add(formOut);
			}
		}

		public TIntList getInfluencedFormOuts(int formIn) {
			if (!formalIns.contains(formIn)) {
				throw new IllegalArgumentException("Node with id " + formIn + " not part of formal-ins of this entry point.");
			}

			return formIn2out.get(formIn);
		}

		private void sortSummaries() {
			if (!SORT_SUMMARY_EDGES) {
				throw new IllegalStateException();
			}

			final TIntObjectIterator<TIntList> it = formIn2out.iterator();
			while (it.hasNext()) {
				it.advance();
				TIntList list = it.value();
				list.sort();
			}
		}

		public static void writeOut(PrintWriter out, EntryPoint ep) {
			out.println("entry: " + ep.entryId);

			TIntList sortedIns = new TIntArrayList(ep.formalIns);
			sortedIns.sort();
			{
				out.print("formal-ins:");
				TIntIterator it = sortedIns.iterator();
				while (it.hasNext()) {
					out.print(" " + it.next());
				}
			}

			out.println();

			{
				TIntList sortedOuts = new TIntArrayList(ep.formalOuts);
				sortedOuts.sort();
				out.print("formal-outs:");
				TIntIterator it = sortedOuts.iterator();
				while (it.hasNext()) {
					out.print(" " + it.next());
				}
			}

			out.println();

			TIntIterator it = sortedIns.iterator();
			while (it.hasNext()) {
				int fIn = it.next();
				out.print(fIn + ":");

				TIntList summary = ep.formIn2out.get(fIn);
				if (summary != null) {
					TIntIterator it2 = summary.iterator();
					while (it2.hasNext()) {
						out.print(" " + it2.next());
					}
				}

				out.println();
			}

			out.flush();
		}

		public static EntryPoint readIn(InputStream in) throws ParseException {
			if (in == null) {
				throw new IllegalArgumentException();
			}

			EntryPoint ep = null;

			try {
				Scanner scan = new Scanner(in);

				int currentLine = 1;

				String token = scan.next().toLowerCase();
				if (!"entry:".equals(token)) {
					scan.close();
					throw new ParseException("\"entry:\" expected", currentLine);
				}

				final int entryId = scan.nextInt();

				currentLine++;

				token = scan.next();
				if (!"formal-ins:".equals(token)) {
					scan.close();
					throw new ParseException("\"formal-ins:\" expected", currentLine);
				}

				final TIntSet formIns = readIntSet(scan);

				currentLine++;

				token = scan.next();
				if (!"formal-outs:".equals(token)) {
					throw new ParseException("\"formal-outs:\" expected", currentLine);
				}

				final TIntSet formOuts = readIntSet(scan);

				ep = new EntryPoint(entryId, formIns, formOuts);

				while (scan.hasNext()) {
					currentLine++;

					token = scan.next();
					if (!token.endsWith(":")) {
						throw new ParseException("Expected format \"<number>:(' '<number>)*\". No ':' found.", currentLine);
					}
					token = token.substring(0, token.length() - 1);
					int formInId = 0;
					try {
						formInId = Integer.parseInt(token);
					} catch (NumberFormatException exc) {
						throw new ParseException("\"" + token + "\" is no number", currentLine);
					}

					final TIntList summaryOuts = readIntList(scan);

					for (TIntIterator it = summaryOuts.iterator(); it.hasNext(); ) {
						int formOutId = it.next();
						ep.addSummaryDep(formInId, formOutId);
					}
				}

				if (SORT_SUMMARY_EDGES) {
					ep.sortSummaries();
				}
			} catch (NullPointerException exc) {
				throw new ParseException(exc.getMessage(), 0);
			}

			return ep;
		}

		private static TIntSet readIntSet(Scanner scan) {
			TIntSet set = new TIntHashSet();

			while (scan.hasNextInt()) {
				int i = scan.nextInt();
				set.add(i);
			}

			return set;
		}

		private static TIntList readIntList(Scanner scan) {
			TIntList set = new TIntArrayList();

			while (scan.hasNextInt()) {
				int i = scan.nextInt();
				set.add(i);
			}

			return set;
		}

		public TIntIterator iterateFormalIns() {
			return formalIns.iterator();
		}

		public int getEntryId() {
			return entryId;
		}

		public void reset() {
			formIn2out.clear();
			summaryEdges = 0;
		}

		@Override
		public int compareTo(EntryPoint o) {
			return this.hashCode() - o.hashCode();
		}
	}

	private final G subgraph;
	private final Set<EntryPoint> entries;
	private final String name;
	private final TIntSet relevantProcs;
	private final TIntSet fullyConnected;
	private final TIntObjectMap<List<SDGNode>> out2in;
	private boolean immutable = false;
	private final boolean rememberReached;
	/** initial entries of the worklist or none (if the default worklist contents should be used */
	private Optional<TIntSet> initialWorklistEntries = Optional.empty();

	private WorkPackage(G subgraph, Set<EntryPoint> entries, String name,
			TIntSet relevantProcs, TIntSet fullyConnected, TIntObjectMap<List<SDGNode>> out2in,
			boolean rememberReached) {
		this.subgraph = subgraph;
		this.entries = entries;
		this.name = name;
		this.relevantProcs = relevantProcs;
		this.fullyConnected = fullyConnected;
		this.out2in = out2in;
		this.rememberReached = rememberReached;
	}

	public static WorkPackage<SDG> create(SDG subgraph, Set<EntryPoint> entryPoints, String name) {
		WorkPackage<SDG> pack = new WorkPackage<>(subgraph, entryPoints, name, null, null, null, false);

		return pack;
	}

	public static WorkPackage<SDG> create(SDG subgraph, Set<EntryPoint> entryPoints, String name, TIntSet relevantProcs) {
		WorkPackage<SDG> pack = new WorkPackage<>(subgraph, entryPoints, name, relevantProcs, null, null, false);

		return pack;
	}

	public static WorkPackage<SDG> create(SDG subgraph, Set<EntryPoint> entryPoints, String name,
			TIntSet relevantProcs, TIntSet fullyConnected, TIntObjectMap<List<SDGNode>> out2in) {
		WorkPackage<SDG> pack = new WorkPackage<>(subgraph, entryPoints, name, relevantProcs, fullyConnected, out2in, false);

		return pack;
	}

	public static WorkPackage<SDG> create(SDG subgraph, Set<EntryPoint> entryPoints, String name,
			TIntSet relevantProcs, TIntSet fullyConnected, TIntObjectMap<List<SDGNode>> out2in,	boolean rememberReached) {
		WorkPackage<SDG> pack = new WorkPackage<>(subgraph, entryPoints, name, relevantProcs, fullyConnected, out2in, rememberReached);

		return pack;
	}

	public G getGraph() {
		return subgraph;
	}

	public void addSummaryDep(int formIn, int formOut) {
		if (immutable) {
			throw new IllegalStateException("WorkPackage has entered immutable state.");
		}

		for (EntryPoint ep : entries) {
			ep.addSummaryDep(formIn, formOut);
		}
	}

	public TIntSet getRelevantProcIds() {
		return relevantProcs;
	}

	public TIntSet getFullyConnected() {
		return fullyConnected;
	}

	public TIntObjectMap<List<SDGNode>> getOut2In() {
		return out2in;
	}

	public boolean getRememberReached() {
		return rememberReached;
	}

	public void workIsDone() {
		if (!immutable) {
			immutable = true;

			if (SORT_SUMMARY_EDGES) {
				for (EntryPoint entry : entries) {
					entry.sortSummaries();
				}
			}
		}
	}

	public void reset() {
		for (EntryPoint ep : entries) {
			ep.reset();
		}

		immutable = false;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer("WP: ");
		for (EntryPoint ep : entries) {
			sb.append(ep.toString());
			sb.append(' ');
		}
		sb.append("- Subgraph with ");
		sb.append(subgraph.vertexSet().size());
		sb.append(" nodes and ");
		sb.append(subgraph.edgeSet().size());
		sb.append(" edges.");

		return sb.toString();
	}

	public TIntSet getAllFormalInIds() {
		TIntSet fIns = new TIntHashSet();

		for (EntryPoint entry : entries) {
			fIns.addAll(entry.formalIns);
		}

		return fIns;
	}

	public Set<EntryPoint> getEntryPoints() {
		return Collections.unmodifiableSet(entries);
	}

	public String getName() {
		String shorter = (name.length() > 128 ? name.substring(0, 128) + "..." : name);
		return "WP[" + shorter + "]";
	}

	public boolean isFinished() {
		return immutable;
	}

	public WorkPackage<G> setInitialWorklistEntries(Optional<TIntSet> initialWorklistEntries) {
		this.initialWorklistEntries = initialWorklistEntries;
		return this;
	}

	public Optional<TIntSet> getInitialWorklistEntries() {
		return initialWorklistEntries;
	}

}