/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.wala.util.io.FileUtil;

import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

/**
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class CollectData {
	
	private static final String SDG_SUFFIX = ".pdg";
	private static final String SDG_REGEX = ".*\\" + SDG_SUFFIX;
	private static final String SLICING_SUFFIX = "-heavyslicing.pdg";
	private static final String SDG_STATS_SUFFIX = ".log";
	private static final String SDG_STATS_NEWSUM_SUFFIX = "-sumnew.log";
	private static final String SDG_STATS_OLDSUM_SUFFIX = "-sumold.log";

	public static void main(String[] args) throws IOException {
		final CollectData cd = new CollectData();
		cd.scanDir("../../example/output");
		cd.scanDir("../../deprecated/jSDG/out");
		for (final ProgramData pd : cd.name2data.values()) {
			System.out.println(pd.name + ": " + pd.runs.size());
			pd.readData();
		}
	}

	public Map<String, ProgramData> name2data = new HashMap<String, CollectData.ProgramData>();
	
	public void scanDir(final String dirName) {
		final Collection<File> result = FileUtil.listFiles(dirName, SDG_REGEX, true);
		for (final File f : result) {
			final String name = extractName(f.getAbsolutePath());
			ProgramData pd = name2data.get(name);
			if (pd == null) {
				pd = new ProgramData(name);
				name2data.put(name, pd);
			}
			final SDGData sd = new SDGData(f.getAbsolutePath());
			pd.runs.add(sd);
		}
	}
	
	public static boolean checkExists(final String fileName) {
		final File f = new File(fileName);
		return f.exists() && f.isAbsolute() && f.canRead() && f.length() > 0;
	}
	
	public static String extractName(final String pdgFile) {
		String name  = null;
		if (pdgFile.contains("test_")) {
			// junit-graph file
			// typical name: C:\Users\Juergen\git\joana\example\output\test_JRE14_HSQLDB_PtsInst_Graph-noopt.pdg
			name = pdgFile.substring(pdgFile.indexOf("test_") + "test_".length());
			if (name.startsWith("J2ME_Safe")) {
				// special treatment for bogus name
				name = "J2MESafe";
			} else if (name.startsWith("JC_Safe")) {
				// special treatment for bogus name
				name = "SafeApplet"; 
			} else {
				name = name.substring(name.indexOf("_") + 1);
				if (name.startsWith("JavaGrande")) {
					name = name.substring("JavaGrande".length());
				}
				name = name.substring(0, name.indexOf("_"));
			}
		} else {
			// tree or unstructured pdg file
			// typical name: C:\Users\Juergen\git\joana\deprecated\jSDG\out\tree-0-1-cfa\j2me-Barcode.pdg
			name = pdgFile.substring(pdgFile.lastIndexOf("-") + 1);
			name = name.substring(0, name.length() - SDG_SUFFIX.length());
		}
		
		return name.toLowerCase();
	}
	
	public static String statsFile(final String pdgFile) {
		return pdgFile.substring(0, pdgFile.length() - SDG_SUFFIX.length()) + SDG_STATS_SUFFIX;
	}
	
	public static String statsNewSumFile(final String pdgFile) {
		return pdgFile + SDG_STATS_NEWSUM_SUFFIX;
	}
	
	public static String statsOldSumFile(final String pdgFile) {
		return pdgFile + SDG_STATS_NEWSUM_SUFFIX;
	}
	
	public static class ProgramData {
		final String name;
		final List<SDGData> runs = new LinkedList<SDGData>();
		
		public ProgramData(final String name) {
			this.name = name;
		}
		
		public void readData() throws IOException {
			for (final SDGData sd : runs) {
				sd.readData();
			}
		}
		
	}
	
	public static enum ParamModel {
		UNSTRUCTURED, OBJ_TREE, OBJ_GRAPH_FIXP, OBJ_GRAPH_SIMPLE
	}
	
	public static class SDGData {
		final String sdgFile;
		ExceptionAnalysis exc;
		PointsToPrecision pts;
		ParamModel model;
		boolean noOptimizations;
		int numberOfNormalNodes;
		int numberOfParameterNodes;
		int numberOfNormalEdges;
		int numberOfSummaryEdges;		
		long computationTime;
		long summaryTime;
		
		public SDGData(final String fileName) {
			this.sdgFile = fileName;
		}
		
		public void readData() throws IOException {
			extractParamModel();
			extractExceptionAnalysis();
			extractPointsToPrecision();
			extractNoOpt();
			
			if (checkExists(statsFile(sdgFile))) {
				final BufferedReader bIn = new BufferedReader(new FileReader(statsFile(sdgFile)));
				final String line = bIn.readLine();
				// string for obj-tree and unstructured:
				// "114558 nodes and 1027491 edges in 12208 ms"
				// string for obj-graph stats:
				// "test_JC_Safe_PtsInst_Graph_StdNoOpt-noopt-noexc: 201 methods (201 unpruned) total of 10482 nodes and 57046 edges. Computation time was 366 ms"
				if (line.contains(" nodes")) { 
					String nodes = line.substring(0, line.indexOf(" nodes"));
					if (nodes.contains("total of ")) {
						nodes = nodes.substring(nodes.indexOf("total of ") + "total of ".length());
					}
					numberOfNormalNodes = Integer.parseInt(nodes);
				} else {
					System.err.println("illegal line found in " + sdgFile);
					System.err.println(line);
				}
				
				if (line.contains(" edges")) {
					String edges = line.substring(0, line.indexOf(" edges"));
					edges = edges.substring(edges.indexOf("nodes and ") + "nodes and ".length());
					numberOfNormalEdges = Integer.parseInt(edges);
				} else {
					System.err.println("illegal line found in " + sdgFile);
					System.err.println(line);
				}
				
				if (line.contains(" ms")) {
					String time = line.substring(0, line.indexOf(" ms"));
					if (time.contains("time was ")) {
						time = time.substring(time.indexOf("time was ") + "time was ".length());
					} else {
						time = time.substring(time.indexOf("edges in ") + "edges in ".length());
					}
					computationTime = Long.parseLong(time);
				} else {
					System.err.println("illegal line found in " + sdgFile);
					System.err.println(line);
				}
			}
			
			if (checkExists(statsNewSumFile(sdgFile))) {
				// TODO
				
			}
		}
		
		private void extractNoOpt() {
			noOptimizations = true;
			if (model == ParamModel.OBJ_GRAPH_FIXP || model == ParamModel.OBJ_GRAPH_SIMPLE) {
				noOptimizations = false;
				if (sdgFile.contains("NoOpt") || sdgFile.contains("-noopt")) {
					noOptimizations = true;
				}
			}
		}
		
		private void extractPointsToPrecision() {
			if (sdgFile.contains("PtsType") || sdgFile.contains("-0-cfa")) {
				pts = PointsToPrecision.TYPE_BASED;
			} else if (sdgFile.contains("PtsInst") || sdgFile.contains("-0-1-cfa")) {
				pts = PointsToPrecision.INSTANCE_BASED;
			} else if (sdgFile.contains("PtsObj") || sdgFile.contains("-objsens")) {
				pts = PointsToPrecision.OBJECT_SENSITIVE;
			} else {
				pts = PointsToPrecision.RTA;
				System.err.println("unknown points-to for filename: " + sdgFile);
			}
		}
		
		private void extractExceptionAnalysis() {
			exc = ExceptionAnalysis.INTRAPROC;
			if (sdgFile.contains("-allexc")) {
				exc = ExceptionAnalysis.ALL_NO_ANALYSIS;
			} else if (sdgFile.contains("-noexc")) {
				exc = ExceptionAnalysis.IGNORE_ALL;
			}
		}
		
		private void extractParamModel() {
			if (sdgFile.contains("Graph_Fast")) {
				model = ParamModel.OBJ_GRAPH_SIMPLE;
			} else if (sdgFile.contains("Graph_Std") || sdgFile.contains("Graph-") || sdgFile.contains("Graph.")) {
				model = ParamModel.OBJ_GRAPH_FIXP;
			} else if (sdgFile.contains("tree-")) {
				model = ParamModel.OBJ_TREE;
			} else if (sdgFile.contains("wala-")) {
				model = ParamModel.UNSTRUCTURED;
			} else {
				System.err.println("unknown parameter model for filename: " + sdgFile);
			}
		}
		
		public int totalNumberOfNodes() {
			return numberOfNormalNodes + numberOfParameterNodes;
		}
		
		public int totalNumberOfEdges() {
			return numberOfNormalEdges + numberOfSummaryEdges;
		}
		
		public long totalTime() {
			return computationTime + summaryTime;
		}
	}
}
