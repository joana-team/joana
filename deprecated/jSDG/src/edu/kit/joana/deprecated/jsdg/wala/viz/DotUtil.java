/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package edu.kit.joana.deprecated.jsdg.wala.viz;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.labeled.LabeledEdgeManager;
import com.ibm.wala.util.graph.labeled.LabeledGraph;
import com.ibm.wala.viz.NodeDecorator;

import edu.kit.joana.deprecated.jsdg.util.ExtendedNodeDecorator;
import edu.kit.joana.deprecated.jsdg.util.Log;

/**
 * utilities for interfacing with DOT
 *
 * @author sfink
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class DotUtil {


	/**
	 * @param g
	 * @param labels
	 * @throws WalaException
	 * @throws CancelException
	 * @throws IllegalArgumentException  if g is null
	 */
	public static <T,M> void dotify(Graph<T> g, EdgeManager<T> edges, ExtendedNodeDecorator labels, String dotFile,
			String psFile, String dotExe, IProgressMonitor monitor) throws WalaException, CancelException {
		if (g == null) {
			throw new IllegalArgumentException("g is null");
		}

		File f = writeDotFile(g, edges, labels, dotFile, monitor);
		writePsFile(dotExe, psFile, f, monitor);
	}

	public static <T,M> void dotify(LabeledGraph<T, M> g, LabeledEdgeManager<T,M> edges, ExtendedNodeDecorator nDecor,
			EdgeDecorator eDecor, String dotFile, String psFile, String dotExe, IProgressMonitor monitor)
	throws WalaException, CancelException {
		dotify(g, edges, nDecor, eDecor, dotFile, psFile, dotExe, monitor, false);
	}

	/**
	 * @param g
	 * @param nDecor
	 * @throws WalaException
	 * @throws CancelException
	 * @throws IllegalArgumentException  if g is null
	 */
	public static <T,M> void dotify(LabeledGraph<T, M> g, LabeledEdgeManager<T,M> edges, ExtendedNodeDecorator nDecor,
			EdgeDecorator eDecor, String dotFile, String psFile, String dotExe, IProgressMonitor monitor,
			boolean ignoreDefaultLabel)
	throws WalaException, CancelException {
		if (g == null) {
			throw new IllegalArgumentException("g is null");
		}

		File f = writeDotFile(g, edges, nDecor, eDecor, dotFile, monitor, ignoreDefaultLabel);
		writePsFile(dotExe, psFile, f, monitor);
	}



	private static void writePsFile(String dotExe, String psFile, File dotFile, IProgressMonitor monitor)
	throws WalaException, CancelException {
		if (dotFile == null) {
			throw new IllegalArgumentException("dotFile is null");
		}

		if (psFile == null) {
			return;
		}

		String[] cmdarray = { dotExe, "-Tps", "-o", psFile, "-v", dotFile.getAbsolutePath() };
		monitor.beginTask("Running " + dotExe + " on " + dotFile, -1);
		try {
			Process p = Runtime.getRuntime().exec(cmdarray);
			BufferedInputStream output = new BufferedInputStream(p.getInputStream());
			BufferedInputStream error = new BufferedInputStream(p.getErrorStream());

			boolean repeat = true;
			while (repeat) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					// just ignore and continue
				}

				if (output.available() > 0) {
					byte[] data = new byte[output.available()];
					int nRead = output.read(data);
					monitor.worked(nRead);
					if (monitor.isCanceled()) {
						throw CancelException.make("Operation aborted.");
					}

					Log.debug("read " + nRead + " bytes from output stream");
				}

				if (error.available() > 0) {
					byte[] data = new byte[error.available()];
					int nRead = error.read(data);
					monitor.worked(nRead);
					if (monitor.isCanceled()) {
						throw CancelException.make("Operation aborted.");
					}

					Log.debug("read " + nRead + " bytes from error stream");
				}

				try {
					p.exitValue();
					// if we get here, the process has terminated
					repeat = false;
					Log.debug("process terminated with exit code " + p.exitValue());
					monitor.done();
				} catch (IllegalThreadStateException e) {
					// this means the process has not yet terminated.
					repeat = true;
				}
			}
		} catch (IOException e) {
			monitor.cancel();
			Log.error("IOException in " + DotUtil.class + ": " + e.getMessage());
			throw new WalaException("IOException in " + DotUtil.class + ": "
					+ e.getMessage());
		}
	}

	private static <T,M> File writeDotFile(Graph<T> g, EdgeManager<T> edges, ExtendedNodeDecorator labels,
			String dotfile, IProgressMonitor monitor)
	throws WalaException {
		if (g == null) {
			throw new IllegalArgumentException("g is null");
		} else if (dotfile == null) {
			throw new IllegalArgumentException("dotfile is null");
		}

		try {
			File f = new File(dotfile);
			PrintStream pwOut = new PrintStream(f);
			dotOutput(g, edges, labels, pwOut);
			pwOut.close();

			return f;
		} catch (IOException e) {
			if (monitor != null) monitor.cancel();
			Log.error("IOException in " + DotUtil.class + ": " + e.getMessage());
			throw new WalaException("IOException in " + DotUtil.class + ": "
					+ e.getMessage());
		}
	}

	private static <T,M> File writeDotFile(LabeledGraph<T,M> g, LabeledEdgeManager<T,M> edges, ExtendedNodeDecorator nDecor,
			EdgeDecorator eDecor, String dotfile, IProgressMonitor monitor, boolean ignoreDefaultLabels)
	throws WalaException {
		if (g == null) {
			throw new IllegalArgumentException("g is null");
		} else if (dotfile == null) {
			throw new IllegalArgumentException("dotfile is null");
		}

	    try {
	    	File f = new File(dotfile);
	    	PrintStream fw = new PrintStream(f);
	    	dotOutputLabeld(g, edges, nDecor, eDecor, fw, ignoreDefaultLabels);
	    	fw.close();
	    	if (monitor != null) monitor.worked(1);
			if (monitor.isCanceled()) {
				throw CancelException.make("Operation aborted.");
			}

	    	return f;
	    } catch (Exception e) {
	    	if (monitor != null) monitor.cancel();
			Log.error("IOException in " + DotUtil.class + ": " + e.getMessage());
			throw new WalaException("IOException in " + DotUtil.class + ": "
					+ e.getMessage());
	    }
	  }


	/**
	 * @return StringBuffer holding dot output representing G
	 * @throws WalaException
	 */
	private static <T, M> void dotOutput(Graph<T> g, EdgeManager<T> edges, ExtendedNodeDecorator labels, PrintStream out)
	throws WalaException {
		out.print("digraph \"DirectedGraph\" {\n");

		String rankdir = getRankDir();
		if (rankdir != null) {
			out.print("rankdir=" + rankdir + ";");
		}
		out.print("center=true;fontsize=12;node [fontsize=12];edge [fontsize=12]; \n");

		Collection<T> dotNodes = computeDotNodes(g);
		if (g instanceof NumberedGraph<?>) {
			outputNodes(labels, out, dotNodes, (NumberedGraph<T>) g);
		} else {
			outputNodes(labels, out, dotNodes);
		}

		for (T n : g) {
			for (Iterator<? extends T> it2 = g.getSuccNodes(n); it2.hasNext();) {
				T s = it2.next();
				out.print(" \"");
				out.print(getPort(n, labels));
				out.print("\" -> \"");
				out.print(getPort(s, labels));
				out.print("\"");
				out.print(" \n");
			}
		}

		out.print("\n}");
	}

  /**
	 * @return StringBuffer holding dot output representing G
	 * @throws WalaException
	 */
	private static <T, M> void dotOutputLabeld(LabeledGraph<T, M> g, LabeledEdgeManager<T, M> edges, ExtendedNodeDecorator nDecor,
			EdgeDecorator eDecor, PrintStream out, boolean ignoreDefaultLabel)
	throws WalaException {
		out.print("digraph \"DirectedGraph\" {\n");

		String rankdir = getRankDir();
		if (rankdir != null) {
			out.print("rankdir=" + rankdir + ";");
		}
		out.print("center=true;fontsize=12;node [fontsize=12];edge [fontsize=12]; \n");

		Collection<T> dotNodes = computeDotNodes(g);
		outputNodes(nDecor, out, dotNodes);

		for (T n : g) {
			for (Iterator<? extends M> itLabels = g.getSuccLabels(n);
					itLabels.hasNext();) {
				M label = itLabels.next();
				if (ignoreDefaultLabel && label == g.getDefaultLabel()) {
					continue;
				}

				for (Iterator<? extends T> it2 = g.getSuccNodes(n, label);
						it2.hasNext();) {
					T s = it2.next();
					out.print(" \"");
					out.print(getPort(n, nDecor));
					out.print("\" -> \"");
					out.print(getPort(s, nDecor));
					out.print("\"");

					out.print(decorateEdge(label, eDecor));

					out.print(" \n");
				}
			}
		}

		out.print("\n}");
	}

	private static <T extends Object> void outputNodes(ExtendedNodeDecorator labels, PrintStream out,
			Collection<T> dotNodes, NumberedGraph<T> g)
	throws WalaException {
		for (T node : dotNodes) {
			outputNode(labels, out, node, g.getNumber(node));
		}
	}

	private static void outputNode(ExtendedNodeDecorator labels, PrintStream out, Object n, int number)
	throws WalaException {
		out.print("   \"");
		out.print(System.identityHashCode(n));
		out.print("\" ");
		out.print(decorateNode(n, labels, number));
	}

	private static <T extends Object> void outputNodes(ExtendedNodeDecorator labels, PrintStream out, Collection<T> dotNodes)
	throws WalaException {
		for (T node : dotNodes) {
			outputNode(labels, out, node);
		}
	}

	private static void outputNode(ExtendedNodeDecorator labels, PrintStream out, Object n) throws WalaException {
		out.print("   \"");
		out.print(System.identityHashCode(n));
		out.print("\" ");
		out.print(decorateNode(n, labels));
	}

	/**
	 * Compute the nodes to visualize .. these may be clusters
	 *
	 */
	private static <T> Collection<T> computeDotNodes(Graph<T> g) throws WalaException {
		return Iterator2Collection.toList(g.iterator());
	}

	private static String getRankDir() throws WalaException {
		return null;
	}

	private static String decorateEdge(Object e, EdgeDecorator d) throws WalaException {
		StringBuffer result = new StringBuffer();

		if (d != null) {
			result.append("[label=\"");
			result.append(d.getLabel(e));
			result.append("\" style=\"");
			result.append(d.getStyle(e));
			result.append("\" color=\"");
			result.append(d.getColor(e));
			result.append("\"");
		} else {
			result.append(" [style=\"solid\" color=\"black\"");
		}

		result.append(" ] \n");

		return result.toString();
	}

	private static String decorateNode(Object n, ExtendedNodeDecorator d, int number) throws WalaException {
		StringBuffer result = new StringBuffer();

		if (d != null) {
			result.append("[label=\"");
			result.append("(" + number + ") " + d.getLabel(n));
			result.append("\" shape=\"");
			result.append(d.getShape(n));
			result.append("\" color=\"");
			result.append(d.getColor(n));
			result.append("\"");
		} else {
			result.append(" [shape=\"box\" color=\"blue\"");
		}

		result.append(" ] \n");

		return result.toString();
	}

	private static String decorateNode(Object n, ExtendedNodeDecorator d) throws WalaException {
		StringBuffer result = new StringBuffer();

		if (d != null) {
			result.append("[label=\"");
			result.append(d.getLabel(n));
			result.append("\" shape=\"");
			result.append(d.getShape(n));
			result.append("\" color=\"");
			result.append(d.getColor(n));
			result.append("\"");
		} else {
			result.append(" [shape=\"box\" color=\"blue\"");
		}

		result.append(" ] \n");

		return result.toString();
	}

	private static String getPort(Object o, NodeDecorator d) throws WalaException {
		return "" + System.identityHashCode(o);
	}

}
