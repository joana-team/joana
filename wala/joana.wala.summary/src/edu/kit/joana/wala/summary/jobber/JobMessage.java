/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.summary.jobber;

import java.nio.CharBuffer;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import gnu.trove.TIntCollection;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;


public class JobMessage {

	private static final String SEPARATOR = "|";
	private static final String ENTRIES = "ENTRIES";
	private static final String EXIT_POINTS = "EXITS";

	private final TIntList entries;
	private final String subgraphFile;
	private final TIntList exitPoints;

	// info for the job manager. Stores ID of the created job and node of the folded call graph
	// that corresponds to this job.
	private int jobberId;
	private SDGNode node;
	private int size; // number of nodes of the subgraph - used to approximate the computation time of this job.

	public JobMessage(String subgraphFile) {
		if (subgraphFile.contains(SEPARATOR)) {
			throw new IllegalArgumentException("Filename '" + subgraphFile + "' contains seperator char '" + SEPARATOR + "'");
		}

		this.subgraphFile = subgraphFile;
		this.entries = new TIntArrayList();
		this.exitPoints = new TIntArrayList();
	}

	public void addEntry(int id) {
		entries.add(id);
	}

	public TIntCollection getEntries() {
		return entries;
	}

	public void addExitPoint(int id) {
		exitPoints.add(id);
	}

	public TIntCollection getExitPoints() {
		return exitPoints;
	}

	public String getSubgraphFile() {
		return subgraphFile;
	}

	public static CharBuffer toCharBuffer(JobMessage msg) {
		String str = msg.subgraphFile;
		str += SEPARATOR;
		str += ENTRIES;
		str += SEPARATOR;

		TIntIterator it = msg.entries.iterator();
		while (it.hasNext()) {
			int id = it.next();
			str += id + SEPARATOR;
		}

		str += EXIT_POINTS;

		it = msg.exitPoints.iterator();
		while (it.hasNext()) {
			int id = it.next();
			str += SEPARATOR + id;
		}

		return CharBuffer.wrap(str);
	}

	public static JobMessage fromCharBuffer(CharBuffer data) throws JobMessageFormatException {
		JobMessage msg = null;
		final String str = data.toString();
		final StringTokenizer tok = new StringTokenizer(str, SEPARATOR);

		try {
			String subgraphFile = tok.nextToken();
			msg = new JobMessage(subgraphFile);
			if (!ENTRIES.equals(tok.nextToken())) {
				throw new JobMessageFormatException("Excpected " + ENTRIES + " in " + msg);
			}

			String next = tok.nextToken();
			while (!EXIT_POINTS.equals(next)) {
				try {
					int entryId = Integer.parseInt(next);
					msg.addEntry(entryId);
				} catch (NumberFormatException exc) {
					throw new JobMessageFormatException("'" + next + "' is not a number in msg: " + str, exc);
				}

				next = tok.nextToken();
			}

			while (tok.hasMoreElements()) {
				next = tok.nextToken();
				try {
					int exitPointId = Integer.parseInt(next);
					msg.addExitPoint(exitPointId);
				} catch (NumberFormatException exc) {
					throw new JobMessageFormatException("'" + next + "' is not a number in msg: " + str, exc);
				}
			}
		} catch (NoSuchElementException exc) {
			throw new JobMessageFormatException("Unexpected end of msg: " + str, exc);
		}

		return msg;
	}

	public void setJobberId(int jobberId) {
		this.jobberId = jobberId;
	}

	public int getJobberId() {
		return jobberId;
	}

	public void setNode(SDGNode node) {
		this.node = node;
	}

	public SDGNode getNode() {
		return node;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getSize() {
		return size;
	}

	public String toString() {
		return JobMessage.class.getCanonicalName() + "(" + jobberId + "): " + subgraphFile
			+ "[entries: " + entries.size() + "][exits: " + exitPoints.size() + "]";
	}

	public static class JobMessageFormatException extends Exception {

		private static final long serialVersionUID = -5239253732323452625L;

		private JobMessageFormatException() {
			super();
		}

		private JobMessageFormatException(String msg) {
			super(msg);
		}

		private JobMessageFormatException(String msg, Throwable cause) {
			super(msg, cause);
		}

	}

}
