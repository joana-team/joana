/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.api.test.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import javax.xml.stream.XMLStreamException;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.ifc.sdg.io.graphml.SDG2GraphML;
import edu.kit.joana.ifc.sdg.irlsod.DomTree;
import edu.kit.joana.ifc.sdg.util.graph.io.dot.MiscGraph2Dot;

/**
 * TODO: Simon Bischof <simon.bischof@kit.edu>
 */
public class DumpTestSDG {
	
	private static final String outputDir = "out";
	private static final String outputDirPrefix = outputDir + File.separator;

	static {
		File fOutDir = new File(outputDir);
		if (!fOutDir.exists()) {
			fOutDir.mkdir();
		}
	}
	
	public static void dumpSDG(SDG sdg, String filename) throws FileNotFoundException {
		BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outputDirPrefix + filename));
		SDGSerializer.toPDGFormat(sdg, bOut);
	}
	
	public static void dumpGraphML(SDG sdg, String filename) throws FileNotFoundException {
		final BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(outputDirPrefix + filename + ".graphml"));
		final BufferedOutputStream bOutHierachical = new BufferedOutputStream(new FileOutputStream(outputDirPrefix + filename + ".hierarchical.graphml"));
		try {
			SDG2GraphML.convert(sdg, bOut);
			SDG2GraphML.convertHierachical(sdg, bOutHierachical);
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void dumpDotCDomTree(DomTree tree, String filename)
			throws FileNotFoundException {
		MiscGraph2Dot.export(tree.getTree(), MiscGraph2Dot.cdomTreeExporter(), outputDirPrefix + filename);
	}
	
	public static void dumpDotTCT(DirectedGraph<ThreadInstance, DefaultEdge> tct,
									String filename) throws FileNotFoundException {
		MiscGraph2Dot.export(tct, MiscGraph2Dot.tctExporter(), outputDirPrefix + filename);
	}
	
	public static void dumpDotCFG(CFG cfg, String filename) throws FileNotFoundException {
		MiscGraph2Dot.export(cfg, MiscGraph2Dot.joanaGraphExporter(), outputDirPrefix + filename);
	}
}
