/*
 * Copyright (c) 2014, Oracle America, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 *  * Neither the name of Oracle nor the names of its contributors may be used
 *    to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

package edu.kit.joana.wala.eval.jmh;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.graph.EdgeReversedGraph;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.AuxCounters.Type;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGBuildPreparation;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGEdge;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.CFG;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.collections.SimpleVector;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.DependenceGraph;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.PDG;
import edu.kit.joana.wala.core.PDGEdge;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ControlDependenceVariant;
import edu.kit.joana.wala.core.graphs.EfficientDominators;
import edu.kit.joana.wala.core.graphs.NTICDGraphPostdominanceFrontiers;
import edu.kit.joana.wala.core.graphs.SinkdomControlSlices;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators;
import edu.kit.joana.wala.core.graphs.EfficientDominators.DomTree;
import edu.kit.joana.wala.core.graphs.SinkpathPostDominators.ISinkdomEdge;
import edu.kit.joana.wala.core.graphs.FCACD;
import edu.kit.joana.wala.core.graphs.NTICDControlSlices;
import edu.kit.joana.wala.util.WriteGraphToDot;

@Fork(value = 1, jvmArgsAppend = "-Xss128m")
public class JavaBenchmark {
	
	public static final String JOANA_API_TEST_DATA_CLASSPATH = "../../api/joana.api.testdata/bin";
	public static final String ANNOTATIONS_PASSON_CLASSPATH = "../../api/joana.api.annotations.passon/bin";
	
	private static final Stubs STUBS = Stubs.JRE_15;
	
	private static void setDefaults(SDGConfig config) {
		config.setParallel(false);
		config.setComputeSummaryEdges(false);
	}

	
	public static final SDGConfig nticd_isinkdom = new SDGConfig(
			JOANA_API_TEST_DATA_CLASSPATH,
			null,
			STUBS
		); {
			setDefaults(nticd_isinkdom);
			nticd_isinkdom.setControlDependenceVariant(ControlDependenceVariant.NTICD_ISINKDOM);
	}

	
	public static class ClassicPostdominance {

		public static DomTree<PDGNode> classicPostdominance(DirectedGraph<PDGNode, PDGEdge> cfg, PDGNode entry, PDGNode exit) {
			final DirectedGraph<PDGNode, PDGEdge> reversedCfg = new EdgeReversedGraph<PDGNode, PDGEdge>(cfg);
			final EfficientDominators<PDGNode, PDGEdge> dom = EfficientDominators.compute(reversedCfg, exit);

			return dom.getDominationTree();
		}
	}
	
	public static class WeakControlClosure {
		public static Set<PDGNode> viaNTICD(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = NTICDControlSlices.wcc(graph, ms, PDGEdge.class, edgeFactory);
			return result;
		}
		
		public static Set<PDGNode> viaISINKDOM(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = SinkdomControlSlices.wcc(graph, ms, PDGEdge.class, edgeFactory);
			return result;
		}
		
		public static Set<PDGNode> viaFCACD(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = FCACD.wcc(graph, ms);
			return result;
		}
	}
	
	public static class NticdMyWod {
		public static Set<PDGNode> viaMYWOD(DirectedGraph<PDGNode, PDGEdge> graph, Set<PDGNode> ms) {
			final Set<PDGNode> result = NTICDControlSlices.nticdMyWod(graph, ms, PDGEdge.class, edgeFactory);
			return result;
		}
	}

	public static final EdgeFactory<PDGNode, PDGEdge> edgeFactory = new EdgeFactory<PDGNode,PDGEdge>() {
		public PDGEdge createEdge(PDGNode from, PDGNode to) {
			return new PDGEdge(from, to, PDGEdge.Kind.CONTROL_DEP);
		};
	};
	
	public static final EdgeFactory<SDGNode, SDGEdge> sdgEdgeFactory = new EdgeFactory<SDGNode,SDGEdge>() {
		public SDGEdge createEdge(SDGNode from, SDGNode to) {
			return SDGEdge.Kind.CONTROL_DEP_COND.newEdge(from, to);
		};
	};
	
	private static CFG extractICFG(SDG sdg){
		// create new graph and copy all ICFG-related edges and associated
		// vertices into it
		// almost all node ids should be positive, here
		final CFG icfg = new CFG(() -> new SimpleVector<>(10, sdg.vertexSet().size()));
		final Set<SDGEdge> edges = sdg.edgeSet();

		for (SDGEdge e : edges) {
			if (e.getKind() == SDGEdge.Kind.CALL
					|| e.getKind() == SDGEdge.Kind.FORK
					|| e.getKind() == SDGEdge.Kind.RETURN
					|| e.getKind() == SDGEdge.Kind.JUMP_FLOW
					|| e.getKind() == SDGEdge.Kind.CONTROL_FLOW
					|| e.getKind() == SDGEdge.Kind.NO_FLOW) {

				icfg.addVertex(e.getSource());
				icfg.addVertex(e.getTarget());
				icfg.addEdge(e);
			}
		}

		// implicit assumption: the root node of sdg, if any,  is a node with adjacent CONTROL_FLOW edges 
		icfg.setRoot(sdg.getRoot());

		icfg.trimToSize();

		return icfg;
	}


	
	public abstract static class Graphs<V extends IntegerIdentifiable, E extends KnowsVertices<V>> {
		private final boolean dumpingEnabled = true;
		public ArrayList<DirectedGraph<V,E>> graphs;
		
		public void dumpGraph(int n, int i, DirectedGraph<V,E> graph) {
			if (!dumpingEnabled) return;
			final String cfgFileName = WriteGraphToDot.sanitizeFileName(this.getClass().getSimpleName()+"-" + graph.getClass().getName() + "-" + n + "-" + i +"-cfg.dot");
			try {
				WriteGraphToDot.write(graph, cfgFileName, e -> true, v -> Integer.toString(v.getId()));
			} catch (FileNotFoundException e) {
			}
			final DirectedGraph<SinkpathPostDominators.Node<V>, ISinkdomEdge<SinkpathPostDominators.Node<V>>> isinkdom = SinkpathPostDominators.compute(graph).getResult();
			final String isinkdomFileName = WriteGraphToDot.sanitizeFileName(this.getClass().getSimpleName()+"-" + graph.getClass().getName() + "-" + n + "-" + i +"-isinkdom.dot");
			try {
				WriteGraphToDot.write(isinkdom, isinkdomFileName, e -> true, v -> Integer.toString(v.getId()));
			} catch (FileNotFoundException e) {
			}
		}

	}
	
	@AuxCounters(Type.EVENTS)
	@State(Scope.Thread)
	public static class Size {
		public int size;
		
		@Setup(Level.Iteration)
		public void clean() {
			size = 0;
		}
	}

	final static Map<String, Integer> nrOfCfg = new HashMap<>(); {
		nrOfCfg.put("JLex.Main", 303);
	}
	
	@State(Scope.Benchmark)
	public static class JavaProcedureGraphs extends Graphs<PDGNode, PDGEdge> {
		@Param({"0", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33", "34", "35", "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49", "50", "51", "52", "53", "54", "55", "56", "57", "58", "59", "60", "61", "62", "63", "64", "65", "66", "67", "68", "69", "70", "71", "72", "73", "74", "75", "76", "77", "78", "79", "80", "81", "82", "83", "84", "85", "86", "87", "88", "89", "90", "91", "92", "93", "94", "95", "96", "97", "98", "99", "100", "101", "102", "103", "104", "105", "106", "107", "108", "109", "110", "111", "112", "113", "114", "115", "116", "117", "118", "119", "120", "121", "122", "123", "124", "125", "126", "127", "128", "129", "130", "131", "132", "133", "134", "135", "136", "137", "138", "139", "140", "141", "142", "143", "144", "145", "146", "147", "148", "149", "150", "151", "152", "153", "154", "155", "156", "157", "158", "159", "160", "161", "162", "163", "164", "165", "166", "167", "168", "169", "170", "171", "172", "173", "174", "175", "176", "177", "178", "179", "180", "181", "182", "183", "184", "185", "186", "187", "188", "189", "190", "191", "192", "193", "194", "195", "196", "197", "198", "199", "200", "201", "202", "203", "204", "205", "206", "207", "208", "209", "210", "211", "212", "213", "214", "215", "216", "217", "218", "219", "220", "221", "222", "223", "224", "225", "226", "227", "228", "229", "230", "231", "232", "233", "234", "235", "236", "237", "238", "239", "240", "241", "242", "243", "244", "245", "246", "247", "248", "249", "250", "251", "252", "253", "254", "255", "256", "257", "258", "259", "260", "261", "262", "263", "264", "265", "266", "267", "268", "269", "270", "271", "272", "273", "274", "275", "276", "277", "278", "279", "280", "281", "282", "283", "284", "285", "286", "287", "288", "289", "290", "291", "292", "293", "294", "295", "296", "297", "298", "299", "300", "301", "302", "303", "304", "305", "306", "307", "308", "309", "310", "311", "312", "313", "314", "315", "316", "317", "318", "319", "320", "321", "322", "323", "324", "325", "326", "327", "328", "329", "330", "331", "332", "333", "334", "335", "336", "337", "338", "339", "340", "341", "342", "343", "344", "345", "346", "347", "348", "349", "350", "351", "352", "353", "354", "355", "356", "357", "358", "359", "360", "361", "362", "363", "364", "365", "366", "367", "368", "369", "370", "371", "372", "373", "374", "375", "376", "377", "378", "379", "380", "381", "382", "383", "384", "385", "386", "387", "388", "389", "390", "391", "392", "393", "394", "395", "396", "397", "398", "399", "400", "401", "402", "403", "404", "405", "406", "407", "408", "409", "410", "411", "412", "413", "414", "415", "416", "417", "418", "419", "420", "421", "422", "423", "424", "425", "426", "427", "428", "429", "430", "431", "432", "433", "434", "435", "436", "437", "438", "439", "440", "441", "442", "443", "444", "445", "446", "447", "448", "449", "450", "451", "452", "453", "454", "455", "456", "457", "458", "459", "460", "461", "462", "463", "464", "465", "466", "467", "468", "469", "470", "471", "472", "473", "474", "475", "476", "477", "478", "479", "480", "481", "482", "483", "484", "485", "486", "487", "488", "489", "490", "491", "492", "493", "494", "495", "496", "497", "498", "499", "500", "501", "502", "503", "504", "505", "506", "507", "508", "509", "510", "511", "512", "513", "514", "515", "516", "517", "518", "519", "520", "521", "522", "523", "524", "525", "526", "527", "528", "529", "530", "531", "532", "533", "534", "535", "536", "537", "538", "539", "540", "541", "542", "543", "544", "545", "546", "547", "548", "549", "550", "551", "552", "553", "554", "555", "556", "557", "558", "559", "560", "561", "562", "563", "564", "565", "566", "567", "568", "569", "570", "571", "572", "573", "574", "575", "576", "577", "578", "579", "580", "581", "582", "583", "584", "585", "586", "587", "588", "589", "590", "591", "592", "593", "594", "595", "596", "597", "598", "599", "600", "601", "602", "603", "604", "605", "606", "607", "608", "609", "610", "611", "612", "613", "614", "615", "616", "617", "618", "619", "620", "621", "622", "623", "624", "625", "626", "627", "628", "629", "630", "631", "632", "633", "634", "635", "636", "637", "638", "639", "640", "641", "642", "643", "644", "645", "646", "647", "648", "649", "650", "651", "652", "653", "654", "655", "656", "657", "658", "659", "660", "661", "662", "663", "664", "665", "666", "667", "668", "669", "670", "671", "672", "673", "674", "675", "676", "677", "678", "679", "680", "681", "682", "683", "684", "685", "686", "687", "688", "689", "690", "691", "692", "693", "694", "695", "696", "697", "698", "699", "700", "701", "702", "703", "704", "705", "706", "707", "708", "709", "710", "711", "712", "713", "714", "715", "716", "717", "718", "719", "720", "721", "722", "723", "724", "725", "726", "727", "728", "729", "730", "731", "732", "733", "734", "735", "736", "737", "738", "739", "740", "741", "742", "743", "744", "745", "746", "747", "748", "749", "750", "751", "752", "753", "754", "755", "756", "757", "758", "759", "760", "761", "762", "763", "764", "765", "766", "767", "768", "769", "770", "771", "772", "773", "774", "775", "776", "777", "778", "779", "780", "781", "782", "783", "784", "785", "786", "787", "788", "789", "790", "791", "792", "793", "794", "795", "796", "797", "798", "799", "800", "801", "802", "803", "804", "805", "806", "807", "808", "809", "810", "811", "812", "813", "814", "815", "816", "817", "818", "819", "820", "821", "822", "823", "824", "825", "826", "827", "828", "829", "830", "831", "832", "833", "834", "835", "836", "837", "838", "839", "840", "841", "842", "843", "844", "845", "846", "847", "848", "849", "850", "851", "852", "853", "854", "855", "856", "857", "858", "859", "860", "861", "862", "863", "864", "865", "866", "867", "868", "869", "870", "871", "872", "873", "874", "875", "876", "877", "878", "879", "880", "881", "882", "883", "884", "885", "886", "887", "888", "889", "890", "891", "892", "893", "894", "895", "896", "897", "898", "899", "900", "901", "902", "903", "904", "905", "906", "907", "908", "909", "910", "911", "912", "913", "914", "915", "916", "917", "918", "919", "920", "921", "922", "923", "924", "925", "926", "927", "928", "929", "930", "931", "932", "933", "934", "935", "936", "937", "938", "939", "940", "941", "942", "943", "944", "945", "946", "947", "948", "949", "950", "951", "952", "953", "954", "955", "956", "957", "958", "959", "960", "961", "962", "963", "964", "965", "966", "967", "968", "969", "970", "971", "972", "973", "974", "975", "976", "977", "978", "979", "980", "981", "982", "983", "984", "985", "986", "987", "988", "989", "990", "991", "992", "993", "994", "995", "996", "997", "998", "999", "1000"})
		int which;
		
		@Param({"JLex.Main"})
		String className;

		
		@Setup(Level.Trial)
		public void doSetup() throws ClassHierarchyException, UnsoundGraphException, CancelException, IOException {
			if (which >= nrOfCfg.get(className)) throw new IllegalArgumentException(); // jmh will skip this benchmark instance
			
			final SDGConfig config = nticd_isinkdom;
			this.graphs = new ArrayList<>();
			int i = 0;
			{
				final String classPath = JOANA_API_TEST_DATA_CLASSPATH + File.pathSeparator + ANNOTATIONS_PASSON_CLASSPATH;
				config.setClassPath(classPath);
				JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
				config.setEntryMethod(mainMethod.toBCString());

				
				final PrintStream out = IOFactory.createUTF8PrintStream(new ByteArrayOutputStream());
				final IProgressMonitor monitor = NullProgressMonitor.INSTANCE;
				
				final com.ibm.wala.util.collections.Pair<SDG, SDGBuilder> p =
						SDGBuildPreparation.computeAndKeepBuilder(out, SDGProgram.makeBuildPreparationConfig(config), monitor);
				final SDGBuilder builder = p.snd;
				
				final PDG pdg = builder.getAllPDGs().get(which); {
					final DependenceGraph cfg = pdg.createCfgWithoutParams();
					final List<PDGNode> toRemove = new LinkedList<>();
					for (PDGNode n : pdg.vertexSet()) {
						if (n.getKind() == PDGNode.Kind.ENTRY && !pdg.entry.equals(n)) toRemove.add(n);
					}
					for (PDGNode n : toRemove) {
						if (!cfg.outgoingEdgesOf(n).isEmpty()) throw new AssertionError();
						cfg.removeNode(n);
					}
					
					cfg.addEdge(pdg.entry, pdg.exit, new PDGEdge(pdg.entry, pdg.exit, PDGEdge.Kind.CONTROL_FLOW));
					this.graphs.add(cfg);
					dumpGraph(cfg.vertexSet().size(), i++, cfg);
				}
			}
		}

	}
	

	//@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testProcedureCFGNTICD(JavaProcedureGraphs javaGraphs, Size size, Blackhole blackhole) {
		if (javaGraphs.graphs.size() != 1) throw new IllegalArgumentException();
		for (DirectedGraph<PDGNode, PDGEdge> cfg : javaGraphs.graphs) {
			size.size = cfg.vertexSet().size(); 
			final NTICDGraphPostdominanceFrontiers<PDGNode, PDGEdge> result = NTICDGraphPostdominanceFrontiers.compute(cfg, edgeFactory, PDGEdge.class);
			blackhole.consume(result);
		}
	}
	
	
	@State(Scope.Benchmark)
	public static class JavaWholeProgramGraph extends Graphs<SDGNode, SDGEdge> {
		@Param({"JLex.Main"})
		String className;

		
		@Setup(Level.Trial)
		public void doSetup() throws ClassHierarchyException, UnsoundGraphException, CancelException, IOException {
			
			final SDGConfig config = nticd_isinkdom;
			this.graphs = new ArrayList<>();
			final String classPath = JOANA_API_TEST_DATA_CLASSPATH + File.pathSeparator + ANNOTATIONS_PASSON_CLASSPATH;
			config.setClassPath(classPath);
			JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(className);
			config.setEntryMethod(mainMethod.toBCString());


			final PrintStream out = IOFactory.createUTF8PrintStream(new ByteArrayOutputStream());
			final IProgressMonitor monitor = NullProgressMonitor.INSTANCE;

			final SDG sdg = SDGBuildPreparation.compute(out, SDGProgram.makeBuildPreparationConfig(config), monitor);
			final CFG icfg = extractICFG(sdg);
			
			this.graphs.add(icfg);
			dumpGraph(icfg.vertexSet().size(), 0, icfg);
		}
	}
	
	@Benchmark
	@Warmup(iterations = 1)
	@Measurement(iterations = 1)
	@BenchmarkMode(Mode.AverageTime)
	public void testWholeProgramCFGNTICD(JavaWholeProgramGraph javaGraphs, Size size, Blackhole blackhole) {
		if (javaGraphs.graphs.size() != 1) throw new IllegalArgumentException();
		for (DirectedGraph<SDGNode, SDGEdge> cfg : javaGraphs.graphs) {
			size.size = cfg.vertexSet().size(); 
			final NTICDGraphPostdominanceFrontiers<SDGNode, SDGEdge> result = NTICDGraphPostdominanceFrontiers.compute(cfg, sdgEdgeFactory, SDGEdge.class);
			blackhole.consume(result);
		}
	}

	
	
	//public static void mainPrintParam(String[] args) {
	public static void main(String[] args) {
		final int nr     = 1000;
		final int stride = 1; 
		boolean isFirst = true;
		System.out.print("@Param({");
		for (int i = 0; i <= nr; i++) {
			if (!isFirst) System.out.print(", ");
			isFirst = false;
			System.out.print("\""+(i*stride)+"\"");
		}
		System.out.println("})");
	}


	
	public static void mainManual(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
			.include(JavaBenchmark.class.getSimpleName())
			.forks(1)
			.build();
		new Runner(opt).run();
	}
}
