/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import edu.kit.joana.ifc.sdg.graph.SDGNode.NodeFactory;
import edu.kit.joana.ifc.sdg.graph.SDGThreadInstance_Parser.ThreadInstanceStub;
import edu.kit.joana.ifc.sdg.graph.SDGVertex_Parser.SDGNodeStub;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.ThreadsInformation.ThreadInstance;
import edu.kit.joana.util.Log;
import edu.kit.joana.util.Logger;

/**
 * Due to excessive memory consumption of the "whole SDG" ANTLR parser, we are forced to parse parts of the
 * SDG manually and only use ANTLR separate for each node and the ThreadInformation part. You will still need around
 * 10*filesize of RAM to parse the SDG.
 * 
 * WARNING: In order for this parser to work the SDG format has to fulfill an additional constraints
 * (that all generated SDGs currently do). The header of a new node e.g. "ENTR 42 {" as well as the end token of a node
 * "}" have to be in a separate line. If newlines are artificially removed from the SDG this parser will not work. 
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class SDGManualParser {

    private NodeFactory nodeFact = new SDGNode.SDGNodeFactory();
    
	private SDGManualParser() {}

    public void setNodeFactory(final NodeFactory nodeFact) {
    	this.nodeFact = nodeFact;
    }
	
	public static SDG parse(final InputStream in) throws IOException, RecognitionException {
		return parse(in, null);
	}
	
	public static SDG parse(final InputStream in, final NodeFactory nodeFact)
			throws IOException, RecognitionException {
		final SDGManualParser parser = new SDGManualParser();
		if (nodeFact != null) {
			parser.setNodeFactory(nodeFact);
		}
		
		return parser.run(in);
	}
	
	public SDG run(final InputStream in) throws IOException, RecognitionException {
		final Logger log = Log.getLogger(Log.L_SDG_GRAPH_PARSE_INFO);
		final long startTime = System.currentTimeMillis();
		final BufferedReader br = new BufferedReader(new InputStreamReader(in));

		log.out("parsing sdg ");
		final SDGHeader header = parseHeader(br);
		final LinkedList<SDGNodeStub> nodeStubs = new LinkedList<SDGNodeStub>();
		final LinkedList<ThreadInstanceStub> threadStubs = new LinkedList<ThreadInstanceStub>();
		boolean joanaCompiler = false;
		
		boolean endOfSdg = false;
		while (!endOfSdg) {
			final String line = skipEmpty(br);
			if (line == null) {
				error("unexpected end of file.");
			}
			
			if (line.equals("}")) {
				// end of sdg
				endOfSdg = true;
			} else if (line.startsWith("JComp")) {
				joanaCompiler = true;
			} else if (line.startsWith("Thread")) {
				// thread information
				final StringBuffer node = readUntil(br, "}");
				final ThreadInstanceStub stub = parseThreadInstance(line + node.toString());
				threadStubs.add(stub);
			} else if (line.endsWith("{")) {
				// new node
				final StringBuffer node = readUntil(br, "}");
				final SDGNodeStub stub = parseVertex(line + node.toString());
				nodeStubs.add(stub);
				if (nodeStubs.size() % 10000 == 0) {
					log.out(".");
				}
			}
		}
		log.outln("done.");
		
		final long usedMemPhase1 = currentlyUsedMemInMegs(); 
		
		// close to free additional memory
		br.close();
		Runtime.getRuntime().gc();
		
		log.out("building sdg... ");
		final SDG sdg = header.createSDG();
		sdg.setJoanaCompiler(joanaCompiler);
		final long maxUsedMemPhase2 = createNodesAndEdges(sdg, nodeStubs);
		createThreadsInformation(sdg, threadStubs);
		log.outln(" done.");
		
		final long endTime = System.currentTimeMillis();
		
		if (log.isEnabled()) {
			final long time = endTime - startTime;
			final long maxHeapInMegs = Runtime.getRuntime().maxMemory() / (1024 * 1024);
			final long maxMem = Math.max(usedMemPhase1, maxUsedMemPhase2);
			log.outln("read " + header + " with " + sdg.vertexSet().size() + " nodes and " + sdg.edgeSet().size()
				+ " edges in " + time + "ms using " + maxMem + "m of " + maxHeapInMegs + "m max heap.");
		}
		
		return sdg;
	}
	
	private static long currentlyUsedMemInMegs() {
		return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024 * 1024);
	}
	
	private static ThreadInstanceStub parseThreadInstance(final String str) throws RecognitionException {
		final ANTLRStringStream stream = new ANTLRStringStream(str);
		final SDGThreadInstance_Lexer lexer = new SDGThreadInstance_Lexer(stream);
		final CommonTokenStream cts = new CommonTokenStream(lexer);
		final SDGThreadInstance_Parser parse = new SDGThreadInstance_Parser(cts);
		
		return parse.thread();
	}
	
	private static SDGNodeStub parseVertex(final String str) throws RecognitionException {
		final ANTLRStringStream stream = new ANTLRStringStream(str);
		final SDGVertex_Lexer lexer = new SDGVertex_Lexer(stream);
		final CommonTokenStream cts = new CommonTokenStream(lexer);
		final SDGVertex_Parser parse = new SDGVertex_Parser(cts);
		
		return parse.node();
	}

	private static String skipEmpty(final BufferedReader br) throws IOException {
		while (true) {
			final String line = br.readLine();
			if (line == null) {
				return null;
			} if (!line.isEmpty()) {
				return line.trim();
			}
		}
	}
	
	private static StringBuffer readUntil(final BufferedReader br, final String endsWith) throws IOException {
		final StringBuffer sbuf = new StringBuffer();
		boolean found = false;
		while (!found) {
			final String line = br.readLine();
			if (line == null) {
				// not found -> return null
				return null;
			} else if (line.endsWith(endsWith)) {
				found = true;
			}
			
			sbuf.append(line);
		}
		
		return sbuf;
	}

	private static SDGHeader parseHeader(final BufferedReader br) throws IOException {
		final StringBuffer sb = readUntil(br, "{");
		final String str = sb.toString().trim();
		final String tok[] = str.split("\\s+");
		if (tok.length < 2 || !tok[0].equals("SDG")) {
			error("first 'SDG' token not found.");
		}
		
		int version = -1;
		String name = null;

		if (tok.length > 2) {
			try {
				version = Integer.parseInt(tok[1]);
			} catch (NumberFormatException nex) {
			}
			
			if (version == -1) {
				name = tok[1]; 
			} else if (tok.length > 3) {
				name = tok[2];
			}
		}
		
		if (version == -1) {
			version = SDG.DEFAULT_VERSION;
		}
		
		if (name != null) {
			if (name.startsWith("\"")) {
				name = name.substring(1);
			}
			if (name.endsWith("\"")) {
				name = name.substring(0, name.length() - 1);
			}
		}
		
		return new SDGHeader(version, name);
	}
	
    static class SDGHeader {
        private int version;
        private String name;
        
        private SDGHeader(int version, String name) {
        	this.version = version;
        	this.name = name;
        }
        
        private SDG createSDG() {
        	SDG sdg = (name == null ? new SDG() : new SDG(name));
        	return sdg;
        }
        
        public String toString() {
        	return "SDG of " + name + " (v" + version + ")";
        }
    }
    
    private static void createThreadsInformation(final SDG sdg, final LinkedList<ThreadInstanceStub> stubs) {
    	if (stubs.isEmpty()) {
    		return;
    	}
    	
		final Logger log = Log.getLogger(Log.L_SDG_GRAPH_PARSE_INFO);
		log.out("(thread info...");
    	
    	final LinkedList<ThreadInstance> threads = new LinkedList<>();
    	
    	while (!stubs.isEmpty()) {
    		final ThreadInstanceStub tis = stubs.removeFirst();
    		final ThreadInstance ti = tis.create(sdg);
    		threads.add(ti);
    	}
    	
    	if (!threads.isEmpty()) {
    		final ThreadsInformation tnfo = new ThreadsInformation(threads);
    		sdg.setThreadsInfo(tnfo);
    	}
    	
    	log.out("ok)");
    }

    private long createNodesAndEdges(final SDG sdg, final LinkedList<SDGNodeStub> stubs) {
		final Logger log = Log.getLogger(Log.L_SDG_GRAPH_PARSE_INFO);
    	log.out("(nodes...");
    	// 1. create all nodes
	    for (SDGNodeStub n : stubs) {
	    	final SDGNode node = n.createNode(nodeFact);
	    	sdg.addVertex(node);
	    }
    	log.out("ok)");

    	long maxMem = currentlyUsedMemInMegs();
    	int count = 0;
    	
    	log.out("(edges...");
	    // 2. create all edges
    	while (!stubs.isEmpty()) {
    		count++;
    		if (count % 10000 == 0) {
    			long curMaxMem = currentlyUsedMemInMegs();
    			maxMem = Math.max(maxMem, curMaxMem);
    		}
    		final SDGNodeStub n = stubs.removeFirst();
    		n.createEdges(sdg);
    	}
    	log.out("ok)");
    	
    	return maxMem;
    } 

    private static void error(final String msg) {
    	throw new RuntimeException(msg);
    }
    
}
