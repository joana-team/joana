/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.wala.console.console;

import com.amihaiemil.eoyaml.*;
import com.google.common.collect.Multimap;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.InterfaceImplementationOptions;
import com.ibm.wala.ipa.callgraph.UninitializedFieldHelperOptions;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.AnnotationsReader;
import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;
import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.IFCType;
import edu.kit.joana.api.annotations.AnnotationType;
import edu.kit.joana.api.annotations.IFCAnnotation;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.*;
import edu.kit.joana.api.sdg.opt.FilePass;
import edu.kit.joana.api.sdg.opt.PreProcPasses;
import edu.kit.joana.api.sdg.opt.ProGuardPass;
import edu.kit.joana.api.sdg.opt.SetValuePass;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.SecurityNode.SecurityNodeFactory;
import edu.kit.joana.ifc.sdg.core.conc.DataConflict;
import edu.kit.joana.ifc.sdg.core.conc.OrderConflict;
import edu.kit.joana.ifc.sdg.core.violations.*;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.io.graphml.SDG2GraphML;
import edu.kit.joana.ifc.sdg.lattice.*;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
import edu.kit.joana.ifc.sdg.lattice.impl.PowersetLattice;
import edu.kit.joana.ifc.sdg.lattice.impl.ReversedLattice;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.setter.SearchVisitor;
import edu.kit.joana.setter.SetValueStore;
import edu.kit.joana.setter.Tool;
import edu.kit.joana.setter.ValueToSet;
import edu.kit.joana.ui.annotations.*;
import edu.kit.joana.ui.ifc.wala.console.console.Pattern.PatternType;
import edu.kit.joana.ui.ifc.wala.console.gui.tree.ProgramPartToString;
import edu.kit.joana.ui.ifc.wala.console.io.*;
import edu.kit.joana.ui.ifc.wala.console.io.IFCConsoleOutput.Answer;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.Triple;
import edu.kit.joana.util.io.IOFactory;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import picocli.CommandLine;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static edu.kit.joana.api.sdg.SDGBuildPreparation.searchProgramParts;
import static edu.kit.joana.ui.ifc.wala.console.console.EntryLocator.getEntryPointIdAttribute;

public class IFCConsole {

	/** @formatter:off */
	public enum CMD {
		// format is
		// 		(String name, int arity, String format, String description)
		// or 	(String name, int minArity, int maxArity, String format, String description)
		HELP(			"help", 				0, 		"",
							"Display this help."),
		FIND_COMMAND(			"findCommand", 				1, 		"",
				"Find a command that contains the string"),
		SEARCH_ENTRIES(	"searchEntries", 		0, 		"",
							"Searches for possible entry methods."),
		SEARCH_ENTRY_POINTS(	"searchEntryPointsOutYaml", 		0, 		1, "[pattern]",
				"Searches for possible entry methods that are @EntryPoint annotated."),
		SELECT_ENTRY(	"selectEntry", 			1, 		"",
							"Selects an entry method for sdg generation."),
		SELECT_ENTRY_POINT(	"selectEntryPoint", 			1, 		"<pattern>",
				"Selects an entry point for sdg generation."),
		SEARCH_SOURCES("searchSources", 1, 1, "<tag>",
				"Search sources that have the given tag"),
		SEARCH_SINKS("searchSinks", 1, 1, "<tag>",
				"Search sinks that have the given tag"),
		SELECT_SOURCES("selectSources", 0, 1, "[pattern]",
				"Select sources that match the pattern (or that have a tag that matches it)"),
		SELECT_SINKS("selectSinks", 0, 1, "[pattern]",
				"Select sinks that match the pattern (or that have a tag that matches it)"),
		SELECT_DECLASS("selectDeclass", 0, 1, "[pattern]",
				"Select declassification annotations that match the pattern (or that have a tag that matches it)"),
		SEARCH_DECLASS("searchDeclass", 1, 1, "<tag>",
				"Search declassification annotations that have the given tag"),
		USE_ENTRY_POINT("useEntryPoint", 1, "tag", "Select the entry point with the given tag, build the sdg, select sources and sinks with this tag"),
		RUN_ENTRY_POINTS_YAML("runEntryPointsYAML", 0, 2, "<out file, '-' for std out, is the default> <pattern matching the entry points, optional, matches all if not present>",
				"Stores the analysis results for the entry points in the passed file as YAML"),
		SET_CLASSPATH(	"setClasspath", 		1, 		"<path>",
							"Sets the class path for sdg generation. Can be for example a bin directory or a jar file."),
		SET_EXCEPTIONS( "setExceptionAnalysis", 1, "<exception analysis type>", "Sets the type of exception analysis to perform during SDG construction. Possible values are: " + Arrays.toString(ExceptionAnalysis.values())),
		SET_POINTSTO(	"setPointsTo", 			1, 		"<points-to precision>",
							"Sets the points-to precision for sdg generation."),
		SET_COMPUTE_INTERFERENCES("setComputeInterferences", 1, "true|false", "Sets whether interference edges shall be computed or not."),
		SET_MHP_TYPE(
						"setMHPType", 		1, 	"<mhp type>", 			"Sets the type of MHP analysis to use if interference edges are activated. Possible values are: " + Arrays.toString(MHPType.values())),
		INFO(			"info", 				0, 		"",
							"Display the current configuration for sdg generation and ifc analysis"),
		BUILD_SDG(		"buildSDG", 			0, 	3, 	"<compute interference?> <mhptype> [<exception analysis type>]",
							"Build sdg with respect to selected entry method. It is possible to use this command parameterless, then the current values of the respective options are taken. Otherwise, provide <compute interference> , <mhptype> and optionally <exception analysis type>. If, in this latter form, <exception analysis type> is not provided, INTERPROC is used."),
		BUILD_SDG_IF_NEEDED(		"buildSDGIfNeeded", 			0, 	3, 	"<compute interference?> <mhptype> [<exception analysis type>]",
				"Like 'buildSDG', but only builds the SDG if needed"),
		LOAD_SDG(		"loadSDG", 				1, 		"<filename>",
							"Load sdg stored in <filename>."),
		SOURCE(			"source", 				2, 		"<index> <level>",
							"Annotate specified part of method with provided security level <level>. <index> is either of the form p<number> for parameters of i<number> for instructions."),
		SAVE_SDG(		"saveSDG", 				1, 		"<filename>",
							"Store current SDG in file specified by <filename>."),
		EXPORT_SDG("exportSDG", 				1, 		"<filename>",
				"Export current SDG in GraphML format to file specified by <filename>."),
		SINK(			"sink", 				2, 		"<index> <level>",
							"Annotate specified node with required security level <level>. <index> refers to the indices shown in the currently active method."),
		CLEAR(			"clear", 				1, 		"<index>",
							"Clear annotation of specified node. <index> refers to the indices shown in the currently active method."),
		CLEARALL(		"clearAll", 			0, 		"",
							"Clear all annotations."),
		DECLASS(		"declass", 				3, 		"<index> <level1> <level2>",
							"Declassify specified node from <level1> to <level2>. <index> refers to the indices shown in the currently active method."),
		RUN(			"run", 					0, 	2, 	" [type] ",
							"Run IFC analysis with specified data. The optional parameter type denotes the type of ifc analysis. It can be " + IFCTYPE_CLASSICAL_NI + ", " + IFCTYPE_LSOD + ", " + IFCTYPE_RLSOD + " or " + IFCTYPE_iRLSOD + ". If it is omitted, classical non-interference is used (or the type set via 'type')"),
		RUN_YAML(			"runYaml", 					0, 	1, 	" [file or '-', the default] ",
				"Run IFC analysis with specified data. Output result into the file as YAML."),
		SET_TYPE("setType", 1, "<type>", "Type of the ifc analysis"),
		SET_TIME_SENSITIVITY("setTimeSensitivity", 1, "true|false", "Is time sensitive?"),
		ONLY_DIRECT_FLOW("onlyDirectFlow", 1, "true|false", "Only direct/data flow?"),
		RESET(			"reset", 				0, 		"",
							"Reset node data."),
		SAVE_ANNOT(		"saveAnnotations", 		1, 		"<filename>",
							"Save annotations done so far in specified file."),
		LOAD_ANNOT(		"loadAnnotations", 		1, 		"<filename>",
							"Load annotations from specified file."),
		SHOW_ANNOT(		"showAnnotations", 		0, 		"",
							"Show the annotations done so far."),
		LOAD_LATTICE(	"loadLattice", 			1, 		"<filename>",
							"Load security lattice definition from file specified by <filename>."),
		SET_LATTICE(	"setLattice", 			1, 		"<latticeSpec>",
							"Set lattice to the one given by <latticeSpec>. <latticeSpec> is a comma-separated list (without any spaces) of inequalities of the form lhs<=rhs specifying the lattice."),
		SEARCH(			"search", 				1, 		"<pattern>",
							"Searches for method names containing the given pattern and displays result. <pattern> is a java-style regular expression."),
		SET_STUBSPATH(	"setStubsPath", 		1, 		"<path>",
							"Sets the path to the stubs file. If <path> is 'none', then no stubs will be used."),
		LIST(			"list", 				0, 		"",
							"Displays last method search results."),
		SELECT(			"select", 				1, 		"<index>",
							"Selects method with index <index> from last search result list."),
		ACTIVE(			"active", 				0, 		"",
							"Shows the active method."),
		SAVESCRIPT(		"saveScript", 			1, 		"<filename>",
							"Saves the instructions up to now to given file."),
		LOADSCRIPT(		"loadScript", 			1, 		"<filename>",
							"Loads instructions from given file and executes them."),
		QUIT(			"quit", 				0, 		"",
							"Exit the IFC console."),
		SHOW_CLASSES(	"showClasses", 			0, 		"",
							"Shows all classes contained in the current sdg"),
		SHOWBCI(		"showBCI", 				0, 		"",
							"Shows all bc indices seen so far."),
	  USE_BYTE_CODE_OPTIMIZATION("useByteCodeOptimization", 0, 1,
				"<false or lib class path>",
				"If the arguments is not 'false': Use byte code optimizations and parse @SetValue annotations"),
	  OPTIMIZE_CLASSPATH("optimizeClassPath", 0, 1, "<lib class path>",
				"Optimize the byte code in the class path and return the new class path"),
		VERIFY_ANNOT(	"verifyAnnotations", 	0, 		"",
							"Verifies that the recorded annotations are mapped consistently to the sdg and vice versa."),
        CHOP(			"chop", 				2, 		"<source> <sink>",
        					"Generates a chop between two program points"),
	  SET_PRUNING_POLICY("setPruningPolicy", 1, 1, "<policy>",
				String.format("Sets the pruning policy: %s", Arrays.stream(PruningPolicy.values())
						.map(v -> String.format("%s (%s)", v.name(), v.description)).collect(Collectors.joining(", ")))),
		SET_VALUE("setValue", 2, 3, "<part> <value> <'field'|'value', default 'value'>", "Set the value of a parameter, method return or field, "
				+ ", program part syntax: field=[class name].[field name], method=[class name].[method name]([param classes, comma separated]), "
				+ "method parameter=[method]->[param number, starting at 0 omitting the type of 'this']. If the last parameter "
				+ "is \"field\" then the value refers to an instance field of the same class (syntax: [field name]) or a static field "
				+ "of the same class (syntax: .#[field name]) or another class (syntax: [pkg1]/…/[pkgn]/[class name][$[inner class name]]#[field name]"),
		SELECT_SET_VALUES("selectSetValues", 0, 1, "[tag, default is \"\"]",
				"Select all @SetValue annotations with the passed tag"),
		SHOW_SET_VALUES("showSetValues", 0, "", "Show parameters, method returns and fields that are set");

		private final String name;
		private final int minArity;
		private final int maxArity;
		private final String format;
		private final String description;

		private CMD(String name, int arity, String format, String descr) {
			this(name, arity, arity, format, descr);
		}

		private CMD(String name, int minArity, int maxArity, String format, String descr) {
			this.name = name;
			this.minArity = minArity;
			this.maxArity = maxArity;
			this.format = format;
			this.description = descr;
		}

		public String getName() {
			return this.name;
		}

		public String getExpectedFormat() {
			return this.format;
		}

		public int getMinArity() {
			return this.minArity;
		}

		public int getMaxArity() {
			return this.maxArity;
		}

		public String getDescription() {
			return this.description;
		}
	}
/** @formatter:on */
	
	private static abstract class Command {

		private final CMD cmd;
		private String stringRepr = null;

		public Command(CMD cmd) {
			this.cmd = cmd;
		}

		public int getMinArity() {
			return cmd.getMinArity();
		}

		abstract boolean execute(String[] args);

		public String getName() {
			return cmd.getName();
		}

		String getExpectedFormat() {
			return cmd.getExpectedFormat();
		}

		public String getDescription() {
			return cmd.getDescription();
		}

		public String toString() {
			if (stringRepr == null) {
				StringBuffer sb = new StringBuffer();
				sb.append(getName());

				if (getMinArity() > 0) {
					sb.append(" ");
					sb.append(getExpectedFormat());
				}

				sb.append("\n\t" + getDescription());
				stringRepr = sb.toString();
			}

			return stringRepr;
		}

		public CMD getCMD() {
			return cmd;
		}
	}

	private static class CommandRepository {
		private final SortedMap<String, Command> availCommands = new TreeMap<String, Command>();

		public void addCommand(Command cmd) {
			availCommands.put(cmd.getName(), cmd);
		}

		public boolean knowsCommand(String cmd) {
			return availCommands.containsKey(cmd);
		}

		// public int getArity(String cmd) {
		// return availCommands.get(cmd).getArity();
		// }

		public boolean executeCommand(CMD cmd, String[] args) {
			final Command command = availCommands.get(cmd.getName());
            return command.execute(args);
		}

		public CMD getCommand(String cmdstr) {
			Command cmd = availCommands.get(cmdstr);

			return (cmd == null ? null : cmd.getCMD());
		}

		public Collection<String> getCommands() {
			return availCommands.keySet();
		}

		public String getHelpMessage(String cmdName) {
			return availCommands.get(cmdName).toString();
		}
	}

	private static final String IFCTYPE_CLASSICAL_NI = "classical-ni";
	private static final String IFCTYPE_LSOD = "lsod";
	private static final String IFCTYPE_RLSOD = "rlsod";
	private static final String IFCTYPE_iRLSOD = "irlsod";

	public static final String LATTICE_BINARY = "BINARY";
	public static final String LATTICE_TERNARY = "TERNARY";
	public static final String LATTICE_DIAMOND = "DIAMOND";

	public static final String DONT_USE_STUBS = "<none>";
	public static final String AVOID_TIME_TRAVEL = "TIMESENS";

	private BufferedReader in;
	// private PrintStream out;
	// private PrintStream errOut;
	// private PrintStream infoOut;
	//
	private final IFCConsoleOutput out;

	private boolean showPrompt = true;

	// private SDG sdg;
	private IFCAnalysis ifcAnalysis = null;
	private String classPath = "bin";
	private PointsToPrecision pointsTo = PointsToPrecision.INSTANCE_BASED;
	private ExceptionAnalysis excAnalysis = ExceptionAnalysis.INTRAPROC;
	private boolean computeInterference = false;
	private MHPType mhpType = MHPType.NONE;
	// private IStaticLattice<String> securityLattice;
	private final Collection<IViolation<SecurityNode>> lastAnalysisResult = new LinkedList<IViolation<SecurityNode>>();
	private TObjectIntMap<IViolation<SDGProgramPart>> groupedIFlows = new TObjectIntHashMap<IViolation<SDGProgramPart>>();
	private Set<edu.kit.joana.api.sdg.SDGInstruction> lastComputedChop = null;
	private final EntryLocator loc = new EntryLocator();
	private final List<IFCConsoleListener> consoleListeners = new LinkedList<IFCConsoleListener>();
	private IProgressMonitor monitor = NullProgressMonitor.INSTANCE;
	private final SDGMethodSelector methodSelector = new SDGMethodSelector(this);
	private IStaticLattice<String> secLattice = IFCAnalysis.stdLattice;
	private final CommandRepository repo = new CommandRepository();
	private final String outputDirectory = "./";
	private String latticeFile;
	private Stubs stubsPath = Stubs.JRE_15;
	private boolean recomputeSDG = true;
	private ChopComputation chopComputation = ChopComputation.ALL;
	private boolean onlyDirectFlow = false; 
	
	private final List<String> script = new LinkedList<String>();
	protected IFCType type = IFCType.CLASSICAL_NI;
	protected boolean isTimeSensitive = false;
	private PruningPolicy pruningPolicy = PruningPolicy.APPLICATION;
	private boolean useByteCodeOptimizations = false;
	private String optLibPath = "";
	private SetValueStore setValueStore = new SetValueStore();
	private Map<SDGProgramPart, Pair<String, ValueToSet.Mode>> valuesToSet = new HashMap<>();
	private String classPathAfterOpt = null;
	/**
	 * @see UninitializedFieldHelperOptions
	 */
	private UninitializedFieldHelperOptions.FieldTypeMatcher uninitializedFieldTypeMatcher = typeReference -> false;

	private InterfaceImplementationOptions interfaceImplOptions = InterfaceImplementationOptions.createEmpty();

	private boolean annotateOverloadedMethods = false;

	/**
	 * BC strings
	 */
	private Collection<String> additionalEntryMethods = Collections.emptyList();

	public IFCConsole(BufferedReader in, IFCConsoleOutput out) {
		this.in = in;
		this.out = out;
		// this.infoOut = infoOut;
		// this.errOut = errOut;
		initialize();
	}

	private Command makeCommandHelp() {
		return new Command(CMD.HELP) {
			@Override
			boolean execute(String[] args) {
				showUsageOutline();
				return true;
			}
		};
	}
	
	private Command makeCommandFindCommand() {
		return new Command(CMD.FIND_COMMAND) {
			@Override
			boolean execute(String[] args) {
				java.util.regex.Pattern pattern = 
						java.util.regex.Pattern.compile(args[1], java.util.regex.Pattern.CASE_INSENSITIVE);
				for (String cmdName : repo.getCommands()) {
					String msg = repo.getHelpMessage(cmdName);
					if (pattern.matcher(msg).find()) {
						out.logln(repo.getHelpMessage(cmdName));
					}
				}
				return true;
			}
		};
	}

	private Command makeCommandSearchEntries() {
		return new Command(CMD.SEARCH_ENTRIES) {

			@Override
			boolean execute(String[] args) {
				return searchEntries();
			}
		};
	}
	
	private Command makeCommandSearchEntryPoints() {
		return new Command(CMD.SEARCH_ENTRY_POINTS) {

			@Override
			boolean execute(String[] args) {
				return searchEntryPointsOutYaml(args.length == 2 ? args[1] : "");
			}
		};
	}
	
	private Command makeCommandSelectSources() {
		return new Command(CMD.SELECT_SOURCES) {
			
			@Override
			boolean execute(String[] args) {
				return selectSources(args.length == 2 ? new Pattern(args[1], true, PatternType.ID, PatternType.SIGNATURE) : new Pattern());
			}
		};
	}

	private Command makeCommandSelectSinks() {
		return new Command(CMD.SELECT_SINKS) {
			
			@Override
			boolean execute(String[] args) {
				return selectSinks(args.length == 2 ? new Pattern(args[1], true, PatternType.ID, PatternType.SIGNATURE) : new Pattern());
			}
		};
	}
	
	private Command makeCommandUseEntryPoint() {
		return new Command(CMD.USE_ENTRY_POINT) {
			
			@Override
			boolean execute(String[] args) {
				return makeCommandSelectSetValues().execute(args)
				    && selectEntryPoint(args[1], s -> ifcAnalysis.addSinkClasses(s.toArray(new String[0])))
						&& makeCommandBuildSDGIfNeeded().execute(new String[] {"bla"}) 
						&& makeCommandSelectSources().execute(args) 
						&& makeCommandSelectSinks().execute(args)
						&& makeCommandSelectDeclassifications().execute(args);
			}
		};
	}
	
	private Command makeCommandRunEntryPointsYAML() {
		return new Command(CMD.RUN_ENTRY_POINTS_YAML) {
			
			@Override
			boolean execute(String[] args) {
				return useEntryPointsYAML(args.length == 1 ? "-" : args[1], args.length <= 2 ? ".*" : args[2]);
			}
		};
	}
	
	private Command makeCommandSelectEntry() {
		return new Command(CMD.SELECT_ENTRY) {

			@Override
			boolean execute(String[] args) {
				Integer i = parseInteger(args[1]);
				if (i != null) {
					return selectEntry(i);
				}
				JavaMethodSignature sig = JavaMethodSignature.fromString(args[1]);
				if (sig != null) {
					return selectEntry(sig);
				}
				
				return false;
			}
		};
	}

	private Command makeCommandSelectEntryPoint() {
		return new Command(CMD.SELECT_ENTRY_POINT) {

			@Override
			boolean execute(String[] args) {
				return selectEntryPoint(args[1], s -> ifcAnalysis.addSinkClasses(s.toArray(new String[0])));
			}
		};
	}
	
	private Command makeCommandSetClasspath() {
		return new Command(CMD.SET_CLASSPATH) {

			@Override
			boolean execute(String[] args) {
				setClasspath(args[1]);
				out.logln("classPath = " + classPath);
				return true;
			}

		};
	}
	
	private Command makeCommandSearchSources() {
		return new Command(CMD.SEARCH_SOURCES) {

			@Override
			boolean execute(String[] args) {
				return searchSinksAndSources(args.length == 2 ? Optional.of(args[1]) : Optional.empty(), AnnotationType.SOURCE);
			}

		};
	}
	
	private Command makeCommandSearchSinks() {
		return new Command(CMD.SEARCH_SINKS) {

			@Override
			boolean execute(String[] args) {
				return searchSinksAndSources(args.length == 2 ? Optional.of(args[1]) : Optional.empty(), AnnotationType.SINK);
			}

		};
	}
	
	private Command makeCommandSearchDeclassifications() {
		return new Command(CMD.SEARCH_DECLASS) {

			@Override
			boolean execute(String[] args) {
				return showDeclassificationAnnotations(args.length == 2 ? Optional.of(args[1]) : Optional.empty());
			}

		};
	}

	private Command makeCommandSelectDeclassifications() {
		return new Command(CMD.SELECT_DECLASS) {

			@Override
			boolean execute(String[] args) {
				return selectDeclassificationAnnotations(args.length == 2 ? Optional.of(args[1]) : Optional.empty());
			}

		};
	}

	private Command makeCommandUseByteCodeOptimization(){
		return new Command(CMD.USE_BYTE_CODE_OPTIMIZATION) {
			@Override boolean execute(String[] args) {
				String arg = args.length == 2 ? args[1] : "";
				useByteCodeOptimizations = !arg.equals("false");
				optLibPath = arg;
				return true;
			}
		};
	}

	private Command makeCommandOptimizeClassPath(){
		return new Command(CMD.OPTIMIZE_CLASSPATH) {
			@Override boolean execute(String[] args) {
				return optimizeClassPath(args.length == 1 ? args[0] : "").map(s -> {
					out.logln(s);
					return true;
				}).orElse(false);
			}
		};
	}

	private Command makeCommandSetPointsTo() {
		return new Command(CMD.SET_POINTSTO) {

			@Override
			boolean execute(String[] args) {
				setPointsTo(args[1]);
				out.logln("points-to = " + pointsTo.desc);
				return true;
			}

		};
	}

	private Command makeCommandSetExceptionAnalysis() {
		return new Command(CMD.SET_EXCEPTIONS) {

			@Override
			boolean execute(String[] args) {
				setExceptionAnalysis(args[1]);
				out.logln("exceptionAnalysis = " + excAnalysis.desc);
				return true;
			}

		};
	}

	private Command makeCommandSetMHPType() {
		return new Command(CMD.SET_MHP_TYPE) {

			@Override
			boolean execute(String[] args) {
				setMHPType(args[1]);
				out.logln("mhpAnalysis = " + mhpType);
				return true;
			}

		};
	}

	private Command makeCommandSetComputeInterferences() {
		return new Command(CMD.SET_COMPUTE_INTERFERENCES) {

			@Override
			boolean execute(String[] args) {
				if (!("true".equals(args[1]) || "false".equals(args[1]))) {
					out.logln("invalid setting: " + args[1]);
					return false;
				}
				
				boolean b = "true".equals(args[1]);
				setComputeInterferences(b);
				out.logln("computeInterferences = " + args[1]);
				return true;
			}

		};
	}

	private Command makeCommandSetStubsPath() {
		return new Command(CMD.SET_STUBSPATH) {

			@Override
			boolean execute(String[] args) {
				Stubs stubs = Stubs.fromString(args[1]);
				if (stubs == null) {
					out.error("Specified stubs not available!");
					return false;
				} else {
					setStubsPath(stubs);
					out.logln("stubs = " + stubs);
					return true;
				}

			}

		};
	}

	private Command makeCommandInfo() {
		return new Command(CMD.INFO) {

			@Override
			boolean execute(String[] args) {
				displayCurrentConfig();
				return true;
			}
		};
	}

	private Command makeCommandBuildSDG() {
		return new Command(CMD.BUILD_SDG) {
			@Override
			boolean execute(String[] args) {
				if (args.length == 1) {
					return buildSDG(IFCConsole.this.computeInterference, IFCConsole.this.mhpType, IFCConsole.this.excAnalysis);
				} else if (args.length == 3) {
					if ("true".equals(args[1])) {
						return buildSDG(true, MHPType.valueOf(MHPType.class, args[2]), ExceptionAnalysis.INTERPROC);
					} else {
						assert "false".equals(args[1]);
						return buildSDG(false, MHPType.NONE,
								ExceptionAnalysis.valueOf(ExceptionAnalysis.class, args[2]));
					}
				} else {
					assert args.length == 4;
					if ("true".equals(args[1])) {
						return buildSDG(true, MHPType.valueOf(MHPType.class, args[2]),
								ExceptionAnalysis.valueOf(ExceptionAnalysis.class, args[3]));
					} else {
						assert "false".equals(args[1]);
						return buildSDG(false, MHPType.valueOf(MHPType.class, args[2]),
								ExceptionAnalysis.valueOf(ExceptionAnalysis.class, args[3]));
					}
				}
			}
		};
	}
	
	private Command makeCommandBuildSDGIfNeeded() {
		return new Command(CMD.BUILD_SDG_IF_NEEDED) {
			
			@Override
			boolean execute(String[] args) {
				if (recomputeSDG) {
					return makeCommandBuildSDG().execute(args);
				}
				return true;
			}
		};
	}

	private Command makeCommandSaveSDG() {
		return new Command(CMD.SAVE_SDG) {

			@Override
			boolean execute(String[] args) {
				return saveSDG(args[1]);
			}

		};
	}

	private Command makeCommandExportSDG() {
		return new Command(CMD.EXPORT_SDG) {

			@Override
			boolean execute(String[] args) {
				return exportGraphML(args[1]);
			}

		};
	}

	// this command is redundant!
	// private Command makeCommandBuildCSDG() {
	// return new Command(CMD.BUILD_CSDG) {
	// @Override
	// boolean execute(String[] args) {
	// if (args.length == 2) {
	// return buildCSDG(args[1]);
	// } else {
	// assert args.length == 3;
	// return buildCSDG(args[1],
	// ExceptionAnalysis.valueOf(ExceptionAnalysis.class, args[2]));
	// }
	// }
	// };
	// }

	private Command makeCommandLoadSDG() {
		return new Command(CMD.LOAD_SDG) {
			@Override
			boolean execute(String[] args) {
				return loadSDG(args[1], getMHPType());
			}
		};
	}

	private Command makeCommandSource() {
		return new Command(CMD.SOURCE) {

			@Override
			boolean execute(String[] args) {
				return annotateProgramPartAsSrcOrSnk(args[1], args[2], AnnotationType.SOURCE);
			}
		};
	}

	private Command makeCommandSink() {
		return new Command(CMD.SINK) {
			@Override
			boolean execute(String[] args) {
				return annotateProgramPartAsSrcOrSnk(args[1], args[2], AnnotationType.SINK);
			}
		};
	}

	private Command makeCommandClear() {
		return new Command(CMD.CLEAR) {
			@Override
			boolean execute(String[] args) {
				return clearAnnotation(args[1]);
			}
		};
	}

	private Command makeCommandClearAll() {
		return new Command(CMD.CLEARALL) {
			@Override
			boolean execute(String[] args) {
				if (ifcAnalysis == null || ifcAnalysis.getProgram() == null) {
					out.info("No program loaded. Build or load SDG first!");
					return true;
				}
				ifcAnalysis.clearAllAnnotations();
				recomputeSDG |= setValueStore.clear();
				return true;
			}
		};
	}

	private Command makeCommandDeclass() {
		return new Command(CMD.DECLASS) {
			@Override
			boolean execute(String[] args) {
				return declassifyProgramPart(args[1], args[2], args[3]);
			}
		};
	}

	private Command makeCommandRunYaml() {
		return new Command(CMD.RUN_YAML) {
			
			@Override
			boolean execute(String[] args) {
				return runAnalysisYAML(args.length == 1 ? "-" : args[1]);
			}
		};
	}
	
	private Command makeCommandSetType() {
		return new Command(CMD.SET_TYPE) {
			
			@Override
			boolean execute(String[] args) {
				IFCType newType = parseIFCType(args[1]);
				if (newType == null) {
					out.error("'" + args[1] + "' is not a valid ifc type, use " + Arrays.toString(IFCType.values()));
					return false;
				}
				type = newType;
				return true;
			}
		};
	}
	
	private Command makeCommandSetTimeSensitivity() {
		return new Command(CMD.SET_TIME_SENSITIVITY) {
			
			@Override
			boolean execute(String[] args) {
				switch (args[1]) {
				case "true":
					isTimeSensitive = true;
					break;
				case "false":
					isTimeSensitive = false;
					break;
				default:
					out.error("argument has to be either 'true' or 'false'");
					return false;
				}
				return true;
			}
		};
	}
	
	private Command makeCommandOnlyDirectFlow() {
		return new Command(CMD.ONLY_DIRECT_FLOW) {
			
			@Override
			boolean execute(String[] args) {
				switch (args[1]) {
				case "true":
					recomputeSDG |= !onlyDirectFlow;
					onlyDirectFlow = true;
					break;
				case "false":
					recomputeSDG |= onlyDirectFlow;
					onlyDirectFlow = false;
					break;
				default:
					out.error("argument has to be either 'true' or 'false'");
					return false;
				}
				return true;
			}
		};
	}
	
	private Command makeCommandRun() {
		return new Command(CMD.RUN) {
			@Override
			boolean execute(String[] args) {
				if (args.length == 1) {
					return doIFC(type, isTimeSensitive);
				} else {
					IFCType ifcType = parseIFCType(args[1]);
					// standard value for time-sensitivity is false; only set to true if mentioned explicitly
					boolean timeSens = (args.length > 2 && AVOID_TIME_TRAVEL.equals(args[2])) || (args.length <= 2 && isTimeSensitive);

					if (ifcType == null) {
						out.error("unknown ifc type: " + args[1]);
						return false;
					} else {
						return doIFC(ifcType, timeSens);
					}
				}
			}			
		};
	}
	
	private IFCType parseIFCType(String s) {
		for (IFCType t : IFCType.values()) {
			if (t.name().equals(s)) {
				return t;
			}
		}
		return null;
	}

	private Command makeCommandReset() {
		return new Command(CMD.RESET) {
			@Override
			boolean execute(String[] args) {
				reset();
				return true;
			}
		};
	}

	private Command makeCommandSaveMarkings() {
		return new Command(CMD.SAVE_ANNOT) {
			@Override
			boolean execute(String[] args) {
				return saveAnnotations(args[1]);
			}
		};
	}

	private Command makeCommandLoadMarkings() {
		return new Command(CMD.LOAD_ANNOT) {

			@Override
			boolean execute(String[] args) {

				return loadAnnotations(args[1]);

			}

		};
	}

	private Command makeCommandShowMarkings() {
		return new Command(CMD.SHOW_ANNOT) {
			@Override
			boolean execute(String[] args) {
				showAnnotations();
				return true;
			}
		};
	}

	private Command makeCommandLoadLattice() {
		return new Command(CMD.LOAD_LATTICE) {

			@Override
			boolean execute(String[] args) {
				return loadLattice(args[1]);
			}
		};
	}

	private Command makeCommandSetLattice() {
		return new Command(CMD.SET_LATTICE) {

			@Override
			boolean execute(String[] args) {
				return setLattice(args[1]);
			}

		};
	}

	private Command makeCommandSearch() {
		return new Command(CMD.SEARCH) {

			@Override
			boolean execute(String[] args) {
				return searchMethodsByName(args[1]);
			}
		};
	}

	private Command makeCommandList() {
		return new Command(CMD.LIST) {

			@Override
			boolean execute(String[] args) {
				displayLastSearchResults();
				return true;
			}
		};
	}

	private Command makeCommandSelect() {
		return new Command(CMD.SELECT) {

			@Override
			boolean execute(String[] args) {
				Integer i = parseInteger(args[1]);
				if (i != null) {
					return selectMethod(i);
				}

				return false;
			}
		};
	}

	private Command makeCommandActive() {
		return new Command(CMD.ACTIVE) {

			@Override
			boolean execute(String[] args) {
				return displayActiveMethod();
			}
		};
	}

	private Command makeCommandLoadScript() {
		return new Command(CMD.LOADSCRIPT) {

			@Override
			boolean execute(String[] args) {
				return loadInstructions(args[1]);
			}

		};
	}

	private Command makeCommandSaveScript() {
		return new Command(CMD.SAVESCRIPT) {

			@Override
			boolean execute(String[] args) {
				return saveInstructions(args[1]);
			}
		};
	}

	private Command makeCommandShowClasses() {
		return new Command(CMD.SHOW_CLASSES) {

			@Override
			boolean execute(String[] args) {
				return showClasses();
			}

		};
	}

	private Command makeCommandVerifyAnnotations() {
		return new Command(CMD.VERIFY_ANNOT) {

			@Override
			boolean execute(String[] args) {
				return verifyAnnotations();
			}
		};
	}

  private Command makeCommandChop() {
		return new Command(CMD.CHOP) {
			@Override
			boolean execute(String[] args) {
                return createChop(args[1], args[2]);
			}
		};
	}

	private Command makeCommandSetPruningPolicy() {
		return new Command(CMD.SET_PRUNING_POLICY) {
			@Override boolean execute(String[] args) {
				return setPruningPolicy(args[1]);
			}
		};
	}



	private boolean verifyAnnotations() {
		if (getSDG() == null) {
			return ifcAnalysis.getSources().isEmpty() && ifcAnalysis.getSinks().isEmpty()
					&& ifcAnalysis.getDeclassifications().isEmpty();
		} else {
			Collection<IFCAnnotation> sources = ifcAnalysis.getSources();
			Collection<IFCAnnotation> sinks = ifcAnalysis.getSinks();
			Collection<IFCAnnotation> declass = ifcAnalysis.getDeclassifications();
			for (SDGNode node : getSDG().vertexSet()) {
				SecurityNode sNode = (SecurityNode) node;
				boolean found = false;
				if (sNode.isInformationSource()) {
					for (IFCAnnotation source : sources) {
						if (ifcAnalysis.getProgram().covers(source.getProgramPart(),sNode)) {
							found = true;
							break;
						}
					}
					if (!found) {
						out.error("Node " + sNode + " in sdg is annotated but has no corresponding annotation object!");
						return false;
					}
				} else if (sNode.isInformationSink()) {
					for (IFCAnnotation sink : sinks) {
						if (ifcAnalysis.getProgram().covers(sink.getProgramPart(), sNode)) {
							found = true;
							break;
						}
					}
					if (!found) {
						out.error("Node " + sNode + " in sdg is annotated but has no corresponding annotation object!");
						return false;
					}
				} else if (sNode.isDeclassification()) {
					for (IFCAnnotation dec : declass) {
						if (ifcAnalysis.getProgram().covers(dec.getProgramPart(), sNode)) {
							found = true;
							break;
						}
					}
					if (!found) {
						out.error("Node " + sNode + " in sdg is annotated but has no corresponding annotation object!");
						return false;
					}
				} else {
					// assert sNode.getProvided() == null && sNode.getRequired()
					// == null;
					// for (IFCAnnotation source : sources) {
					// if (source.getProgramPart().covers(sNode)) {
					// out.error("Node " + node +
					// " in sdg which is not annotated but has a corresponding annotation object: "
					// + source.getProgramPart() + "!");
					// return false;
					// }
					// }
					//
					// for (IFCAnnotation sink : sinks) {
					// if (sink.getProgramPart().covers(sNode)) {
					// out.error("Node " + node +
					// " in sdg which is not annotated but has a corresponding annotation object: "
					// + sink.getProgramPart() + "!");
					// return false;
					// }
					// }
					//
					// for (IFCAnnotation dec : declass) {
					// if (dec.getProgramPart().covers(sNode)) {
					// out.error("Node " + node +
					// " in sdg which is not annotated but has a corresponding annotation object: "
					// + dec.getProgramPart() + "!");
					// return false;
					// }
					// }
				}
			}
			out.info("Annotations are completely verified.");
			return true;
		}
	}

	Command makeCommandSetValue(){
		return new Command(CMD.SET_VALUE) {
			@Override boolean execute(String[] args) {
				return setValueOfProgramPart(args[1], args[2],
						(args.length == 4 && args.equals("field")) ? ValueToSet.Mode.FIELD : ValueToSet.Mode.VALUE);
			}
		};
	}

	Command makeCommandSelectSetValues(){
		return new Command(CMD.SELECT_SET_VALUES) {
			@Override boolean execute(String[] args) {
				return selectSetValues(args.length == 2 ? args[1] : "");
			}
		};
	}

	Command makeCommandShowSetValues(){
		return new Command(CMD.SHOW_SET_VALUES) {
			@Override boolean execute(String[] args) {
				showSetValues();
				return true;
			}
		};
	}

	private void initialize() {

		// setLattice("public<=secret");
		repo.addCommand(makeCommandHelp());
		repo.addCommand(makeCommandFindCommand());
		repo.addCommand(makeCommandSearchEntries());
		repo.addCommand(makeCommandSearchEntryPoints());
		repo.addCommand(makeCommandSelectEntryPoint());
		repo.addCommand(makeCommandSelectEntry());
		repo.addCommand(makeCommandSearchSinks());
		repo.addCommand(makeCommandSearchSources());
		repo.addCommand(makeCommandSelectSinks());
		repo.addCommand(makeCommandSelectSources());
		repo.addCommand(makeCommandUseEntryPoint());
		repo.addCommand(makeCommandRunEntryPointsYAML());
		repo.addCommand(makeCommandSetTimeSensitivity());
		repo.addCommand(makeCommandSetType());
		repo.addCommand(makeCommandOnlyDirectFlow());
		
		repo.addCommand(makeCommandSetClasspath());
		repo.addCommand(makeCommandSetExceptionAnalysis());
		repo.addCommand(makeCommandSetPointsTo());
		repo.addCommand(makeCommandSetComputeInterferences());
		repo.addCommand(makeCommandSetMHPType());
		repo.addCommand(makeCommandSetStubsPath());
		repo.addCommand(makeCommandInfo());
		repo.addCommand(makeCommandBuildSDG());
		repo.addCommand(makeCommandBuildSDGIfNeeded());
		// repo.addCommand(makeCommandBuildCSDG()); <-- this command is
		// redundant!
		repo.addCommand(makeCommandLoadSDG());
		repo.addCommand(makeCommandSaveSDG());
		repo.addCommand(makeCommandExportSDG());
		repo.addCommand(makeCommandSearchDeclassifications());

		repo.addCommand(makeCommandUseByteCodeOptimization());
		repo.addCommand(makeCommandOptimizeClassPath());

		repo.addCommand(makeCommandSetPruningPolicy());

		// ifc commands

		repo.addCommand(makeCommandSource());
		repo.addCommand(makeCommandSink());
		repo.addCommand(makeCommandClear());
		repo.addCommand(makeCommandClearAll());
		repo.addCommand(makeCommandDeclass());
		repo.addCommand(makeCommandRun());
		repo.addCommand(makeCommandRunYaml());
		repo.addCommand(makeCommandReset());
		repo.addCommand(makeCommandSaveMarkings());
		repo.addCommand(makeCommandLoadMarkings());
		repo.addCommand(makeCommandShowMarkings());

		// lattice commands

		repo.addCommand(makeCommandLoadLattice());
		repo.addCommand(makeCommandSetLattice());

		// method search and selection

		repo.addCommand(makeCommandSearch());
		repo.addCommand(makeCommandList());
		repo.addCommand(makeCommandSelect());
		repo.addCommand(makeCommandActive());

		repo.addCommand(makeCommandLoadScript());
		repo.addCommand(makeCommandSaveScript());
		repo.addCommand(makeCommandShowClasses());
		repo.addCommand(makeCommandVerifyAnnotations());
        repo.addCommand(makeCommandChop());

		setLattice(LATTICE_BINARY);
	}

	public boolean selectMethod(int i) {
		if (methodSelector.lastSearchResultEmpty()) {
			out.info("Last search result is empty. Cannot select anything from empty list!");
			return false;
		}

		if (!methodSelector.indexValid(i)) {
			out.error("Invalid method index!");
			return false;
		}

		methodSelector.selectMethod(i);
		displayActiveMethod();
		return true;
	}

	public boolean searchMethodsByName(String name) {
		boolean found;
		try {
			found = methodSelector.searchMethodsContainingName(name);
			if (found) {
				displayLastSearchResults();
				return true;
			} else {
				out.info("No search results. Last search results remain active.");
				return false;
			}
		} catch (java.util.regex.PatternSyntaxException e) {
			out.error("Invalid search pattern: " + e.getMessage());
			return false;
		}
	}

	public boolean annotateProgramPartAsSrcOrSnk(String programPart, String level, AnnotationType type) {
		if (inSecurityLattice(level)) {
			SDGProgramPart toMark = getProgramPartFromSelectorString(programPart, false);
			if (toMark == null) {
				return false;
			}

			IFCAnnotation ann = new IFCAnnotation(type, level, toMark);
			if (ifcAnalysis.isAnnotationLegal(ann)) {
				ifcAnalysis.addAnnotation(ann);
				out.logln(String.format("Annotating '%s' as %s of security level '%s'...", toMark.toString(), type.toString(), level));
				return true;
			} else {
				out.error("Illegal Annotation!");
				return false;
			}
		} else {
			out.error("Level " + level + " not in security lattice! Try one of " + getSecurityLevels());
			return false;
		}
	}

	public boolean declassifyProgramPart(String programPartDesc, String level1, String level2) {
		if (!inSecurityLattice(level1)) {
			out.error("Level " + level1 + " is not an element of security lattice! Try one of "
					+ getSecurityLevels());
			return false;
		}
		if (!inSecurityLattice(level2)) {
			out.error("Level " + level2 + " is not an element of security lattice! Try one of "
					+ getSecurityLevels());
			return false;
		}
		if (!greaterOrEqual(level1, level2)) {
			out.error("Level " + level1 + " is not greater than or equal to " + level2 + "!");
			return false;
		}
		
		SDGProgramPart toMark = getProgramPartFromSelectorString(programPartDesc, false);
		if (toMark != null) {
			out.error("Program part with name " + programPartDesc + " not found!");
			return false;
		}

		IFCAnnotation ann = new IFCAnnotation(level1, level2, toMark);
		if (ifcAnalysis.isAnnotationLegal(ann)) {
			ifcAnalysis.addDeclassification(toMark, level1, level2);
			return true;
		} else {
			out.error("Illegal Annotation!");
			return false;
		}
	}

	public boolean clearAnnotation(String programPartDesc) {
		SDGProgramPart toClear = getProgramPartFromSelectorString(programPartDesc, true);
		if (toClear == null) {
			out.error("Program part with name " + programPartDesc + " not found!");
			return false;
		}

		ifcAnalysis.clearAllAnnotationsOfMethodPart(toClear);
		return true;
		// Integer clearIndex;
		// clearIndex = parseInteger(programPartDesc);//
		// getMethodPartFromAnnotationIndex(args[1]);
		// if (clearIndex != null) {
		// if (annotationManager.annotationIndexValid(clearIndex)) {
		// annotationManager.clearAnnotation(clearIndex);
		// return true;
		// } else {
		// out.error("Invalid annotation index! Must be between 0 and "
		// + (annotationManager.getNumberOfAnnotations() - 1) + "!");
		// return false;
		// }
		// } else {
		// return false;
		// }
	}

	public boolean selectEntry(int i) {
		recomputeSDG = true;
		if (loc.foundPossibleEntries()) {
			if (loc.entryIndexValid(i)) {
				loc.selectEntry(i);
				out.logln("entry = " + loc.getActiveEntry().toHRString());
				return true;
			} else {
				out.error("Invalid index! Must be in range 0-" + (loc.getNumberOfFoundEntries() - 1));
				return false;
			}
		} else {
			out.info("Cannot select any entry method from empty list! Invoke search first!");
			return false;
		}
	}

	public boolean selectEntry(JavaMethodSignature sig) {
		if (loc.foundPossibleEntries()) {
			if (loc.getIndex(sig) >= 0) {
				loc.selectEntry(sig);
				out.logln("entry = " + loc.getActiveEntry().toHRString());
				return true;
			} else {
				out.error("Method " + sig + " not found!");
				return false;
			}
		} else {
			out.info("Cannot select any entry method from empty list! Invoke search first!");
			return false;
		}
	}

	public boolean searchEntries() {
		JavaMethodSignature oldSelected = loc.getActiveEntry();
		boolean found = loc.doSearch(classPath, out);
		if (!found) {
			out.error("No entry methods found.");
			return false;
		}

		loc.displayLastEntrySearchResults(out);
		if (loc.getNumberOfFoundEntries() == 1) {
			selectEntry(0);
		} else if (loc.getLastSearchResults().contains(oldSelected)) {
			selectEntry(oldSelected);
		}
		return true;
	}

	public boolean searchEntryPointsOutYaml(String pattern) {
		JavaMethodSignature oldSelected = loc.getActiveEntry();
		Pattern pat = pattern.isEmpty() ?
				new Pattern() : 
			new Pattern(pattern, true, PatternType.ID, PatternType.SIGNATURE); 
		Optional<List<Pair<IMethod, Annotation>>> result = loc.doSearchForEntryPointAnnotated(classPath, out, pat);
		if (!result.isPresent()) {
			out.error("No entry methods found.");
			return false;
		}
		out.logln(new YamlCollectionDump(result.get().stream().map(p -> {
			return getEntryPointIdAttribute(p.getSecond()).orElse(p.getFirst().getSignature().toString());
		}).collect(Collectors.toList())).represent().toString());
		return true;
	}

	public boolean selectEntryPoint(String pattern, Consumer<List<String>> classSinkConsumer, boolean printError) {
		JavaMethodSignature oldSelected = loc.getActiveEntry();
		Optional<List<Pair<IMethod, Annotation>>> result =
				loc.doSearchForEntryPointAnnotated(classPath, out, new Pattern(pattern, true, PatternType.ID, PatternType.SIGNATURE));
		if (!result.isPresent() || result.get().size() != 1) {
			result = loc.doSearchForEntryPointAnnotated(classPath, out, new Pattern(pattern, false, PatternType.ID, PatternType.SIGNATURE));
		}
		if (!result.isPresent()) {
			if (printError){
				out.error("Entry point '" + pattern + "' not found");
			}
			return false;
		}
		if (result.get().size() > 1) {
			if (printError){
				out.error("Entry point '" + pattern + "' is ambiguous");
			}
			return false;
		}
		Pair<IMethod, Annotation> p = result.get().get(0);
		if (ifcAnalysis != null) {
			ifcAnalysis.clearAllAnnotations();
			recomputeSDG |= setValueStore.clear();
		}
		return selectEntryPoint(JavaMethodSignature.fromString(p.getFirst().getSignature()), p.getSecond(), classSinkConsumer);
	}
	
	public boolean selectEntryPoint(String pattern, Consumer<List<String>> classSinkConsumer) {
		this.secLattice = IFCAnalysis.stdLattice;
		return selectEntryPoint(pattern, classSinkConsumer, true);
	}
	
	public boolean useEntryPointsYAML(String outputFile, String pattern) {
		if (outputFile.equals("-")) {
			return useEntryPointsYAML(out.getPrintStream(), pattern);
		}
		try (PrintStream stream = new PrintStream(new File(outputFile))){
			return useEntryPointsYAML(stream, pattern);
		} catch (IOException e) {
			out.error(e.getMessage());
			return false;
		}
	}
	
	public boolean useEntryPointsYAML(PrintStream stream, String pattern) {
		YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
		Optional<List<Pair<IMethod, Annotation>>> result = 
				loc.doSearchForEntryPointAnnotated(classPath, out, new Pattern(pattern, true, PatternType.ID, PatternType.SIGNATURE));
		if (result.isPresent()) {
			for (Pair<IMethod, Annotation> p : result.get()) {
				Optional<YamlMapping> map = useEntryPointYaml(p.getFirst(), p.getSecond());
				if (!map.isPresent()) {
					return false;
				}
				seq = seq.add(map.get());
			}
			stream.println(seq.build().toString());
			return true;
		}
		out.error("No entry points found");
		return false;
	}
	
	public Optional<YamlMapping> useEntryPointYaml(IMethod method, Annotation annotation){
		if (!selectEntryPoint(JavaMethodSignature.fromString(method.getSignature()), annotation, s -> ifcAnalysis.addSinkClasses(new String[0]))) {
			return Optional.empty();
		}
		String tag = "";
		if (annotation.getNamedArguments().containsKey("tag")) {
			tag = (String)((ConstantElementValue)annotation.getNamedArguments().get("tag")).val;
		}
		Pattern pattern = new Pattern(tag, false, PatternType.SIGNATURE, PatternType.ID);
		if (!selectSetValues(tag) || !buildSDGIfNeeded() || !selectSources(pattern) || !selectSinks(pattern)) {
			return Optional.empty();
		}
		return runAnalysisYAML();
	}
	
	public boolean runAnalysisYAML(String outputFile) {
		if (outputFile.equals("-")) {
			return runAnalysisYAML(out.getPrintStream());
		}
		try (PrintStream stream = new PrintStream(new File(outputFile))){
			return runAnalysisYAML(stream);
		} catch (IOException e) {
			out.error(e.getMessage());
			return false;
		}
	}
	
	public boolean runAnalysisYAML(PrintStream output){
		Optional<YamlMapping> map = runAnalysisYAML();
		if (map.isPresent()) {
			output.println(map.get().toString());
			return true;
		}
		return false;
	}

	public Optional<YamlMapping> runAnalysisYAML(){
		if (ifcAnalysis == null || ifcAnalysis.getProgram() == null) {
			out.info("No program to analyze.");
			return Optional.empty();
		}
		YamlMappingBuilder mapBuilder = YamlUtil.mapping();
		ifcAnalysis.setTimesensitivity(isTimeSensitive);
		out.logln("Performing IFC - Analysis type: " + type);
		Optional<Collection<? extends IViolation<SecurityNode>>> viosOpt = doIFCAndOptAndCatch(type, false);
		if (!viosOpt.isPresent()){
			ifcAnalysis.getAnnManager().unapplyAllAnnotations();
			return Optional.empty();
		}
		Collection<? extends IViolation<SecurityNode>> vios = viosOpt.get();
		lastAnalysisResult.clear();
		lastAnalysisResult.addAll(vios);

		groupedIFlows.clear();
		
		mapBuilder = mapBuilder.add("entry_point_method", loc.getActiveEntry().toHRString());
		SDGProgramPart entryPP = ifcAnalysis.getProgramPart(loc.getActiveEntry().toBCString());
		if (getProgram().getIdManager().contains(entryPP)){
			mapBuilder = mapBuilder.add("entry_point_id", getProgram().getIdManager().get(entryPP));
		}
		mapBuilder = mapBuilder.add("tag", ifcAnalysis.getSourceSinkAnnotationTag());
		mapBuilder = mapBuilder.add("found_flows",	lastAnalysisResult.isEmpty() ? "false" : "true");
		mapBuilder = mapBuilder.add("only_direct_flow", onlyDirectFlow ? "true" : "false");
		if (lastAnalysisResult.isEmpty()) {
			out.logln("No violations found.");
			ifcAnalysis.getAnnManager().unapplyAllAnnotations();
			return Optional.of(mapBuilder.build());
		}
		YamlSequenceBuilder flowsBuilder = Yaml.createYamlSequenceBuilder();
		groupedIFlows = ifcAnalysis.groupByPPPart(vios, false);
		out.logln("done, found " + groupedIFlows.size() + " security violation(s):");
		Set<String> output = new TreeSet<String>();
		for (IViolation<SDGProgramPart> vio : groupedIFlows.keySet()) {
			YamlMappingBuilder vioBuilder = YamlUtil.mapping();
			YamlMappingBuilder[] vioBuild = new YamlMappingBuilder[] {vioBuilder};
			vio.accept(new IViolationVisitor<SDGProgramPart>() {

				YamlMapping convertProgramPartToYaml(SDGProgramPart part){
					YamlMappingBuilder builder = YamlUtil.mapping()
							.add("kind", part.getClass().getName().toLowerCase().replaceAll("(visit|actual|node|SDG)", ""));
					if (getProgram().getIdManager().contains(part)){
						builder = builder.add("id", getProgram().getIdManager().get(part));
					}
					return part.acceptVisitor(new SDGProgramPartVisitor<YamlMappingBuilder, YamlMappingBuilder>() {
						@Override protected YamlMappingBuilder visitClass(SDGClass cl, YamlMappingBuilder data) {
							return data.add("class", cl.getTypeName().toHRString());
						}

						@Override protected YamlMappingBuilder visitAttribute(SDGAttribute a, YamlMappingBuilder data) {
							return visitClass(a.getOwningClass(), data.add("type", a.getType()).add("name", a.getName()));
						}

						@Override protected YamlMappingBuilder visitMethod(SDGMethod m, YamlMappingBuilder data) {
							return visitMethod(m.getSignature(), data);
						}

						protected YamlMappingBuilder visitMethod(JavaMethodSignature signature, YamlMappingBuilder data){
							return data.add("class", signature.getDeclaringType().toHRString())
									.add("name", signature.getMethodName())
									.add("selector", signature.getSelector())
									.add("return", signature.getReturnType().toHRString())
									.add("parameters", signature.getArgumentTypes().stream().map(JavaType::toHRString)
											.collect(YamlUtil.sequenceCollector()));
						}

						@Override protected YamlMappingBuilder visitActualParameter(SDGActualParameter ap, YamlMappingBuilder data) {
							return data.add("method", visitMethod(ap.getOwningMethod(), YamlUtil.mapping()).build())
												 .add("index", ap.getIndex() + "")
												 .add("call", visitCall(ap.getOwningCall(), YamlUtil.mapping()).build())
									       .add("type", ap.getType().toHRString());
						}

						@Override protected YamlMappingBuilder visitParameter(SDGFormalParameter p, YamlMappingBuilder data) {
							return data.add("method", visitMethod(p.getOwningMethod(), YamlUtil.mapping()).build())
									.add("index", p.getIndex() + "")
									.add("type", p.getType().toHRString());
						}

						@Override protected YamlMappingBuilder visitExit(SDGMethodExitNode e, YamlMappingBuilder data) {
							return data.add("method", visitMethod(e.getOwningMethod(), YamlUtil.mapping()).build())
									.add("type", e.getType().toHRString());
						}

						@Override protected YamlMappingBuilder visitException(SDGMethodExceptionNode e, YamlMappingBuilder data) {
							return data.add("method", visitMethod(e.getOwningMethod(), YamlUtil.mapping()).build());
						}

						@Override protected YamlMappingBuilder visitInstruction(SDGInstruction i, YamlMappingBuilder data) {
							return data.add("method", visitMethod(i.getOwningMethod(), YamlUtil.mapping()).build())
									.add("operation", i.getOperation())
									.add("label", i.getLabel())
									.add("type", i.getType());
						}

						@Override protected YamlMappingBuilder visitCall(SDGCall c, YamlMappingBuilder data) {
							return data.add("this", visitActualParameter(c.getThis(), YamlUtil.mapping()).build())
									.add("method", visitMethod(c.getOwningMethod(), YamlUtil.mapping()).build())
									.add("possibleTargets", c.getPossibleTargets().stream()
											.map(t -> visitMethod(t, YamlUtil.mapping()).build())
											.collect(YamlUtil.sequenceCollector()))
									.add("parameters", c.getActualParameters().stream()
											.map(p -> visitActualParameter(p, YamlUtil.mapping()).build())
											.collect(YamlUtil.sequenceCollector()));
						}

						@Override protected YamlMappingBuilder visitCallReturnNode(SDGCallReturnNode c, YamlMappingBuilder data) {
							return data.add("method", visitMethod(c.getOwningMethod(), YamlUtil.mapping()).build())
									.add("call", visitCall(c.getOwningCall(), YamlUtil.mapping()).build());
						}

						@Override protected YamlMappingBuilder visitCallExceptionNode(SDGCallExceptionNode c, YamlMappingBuilder data) {
							return data.add("method", visitMethod(c.getOwningMethod(), YamlUtil.mapping()).build())
									.add("call", visitCall(c.getOwningCall(), YamlUtil.mapping()).build());
						}

						@Override protected YamlMappingBuilder visitPhi(SDGPhi phi, YamlMappingBuilder data) {
							return data.add("method", visitMethod(phi.getOwningMethod(), YamlUtil.mapping()).build());
						}

						@Override protected YamlMappingBuilder visitFieldOfParameter(SDGFieldOfParameter fop, YamlMappingBuilder data) {
							return data.add("declaringClass", fop.getDeclaringClass())
									.add("field", fop.getFieldName())
									.add("accessPath", fop.getAccessPath().stream().collect(YamlUtil.sequenceCollector()))
									.add("parent", convertProgramPartToYaml(fop.getParent()))
									.add("root", convertProgramPartToYaml(fop.getRoot()))
									.add("method", visitMethod(fop.getOwningMethod(), YamlUtil.mapping()).build());
						}

						@Override protected YamlMappingBuilder visitLocalVariable(SDGLocalVariable local, YamlMappingBuilder data) {
							return data.add("method", visitMethod(local.getOwningMethod(), YamlUtil.mapping()).build())
									.add("name", local.getName());
						}
					}, builder).build();
				}

				@Override
				public void visitIllegalFlow(IIllegalFlow<SDGProgramPart> iFlow) {
					vioBuild[0] = vioBuild[0].add("type", "illegal")
					          .add("attacker_level", iFlow.getAttackerLevel())
					          .add("source", convertProgramPartToYaml(iFlow.getSource()))
							      .add("source_level", ifcAnalysis.getProgramPartLevel(iFlow.getSource(), AnnotationType.SOURCE))
					          .add("sink", convertProgramPartToYaml(iFlow.getSink()))
										.add("sink_level", ifcAnalysis.getProgramPartLevel(iFlow.getSink(), AnnotationType.SINK));
				}

				private void visitAbstractConflictLeak(String type, AbstractConflictLeak<SDGProgramPart> conf) {
					vioBuild[0] = vioBuild[0].add("type", type)
					  .add("attacker_level", conf.getAttackerLevel())
					  .add("triggers", new YamlCollectionDump(conf.getAllTriggers().stream().map(Object::toString).collect(Collectors.toList())).represent())
					  .add("conflict_edge", YamlUtil.mapping()
							  .add("source", convertProgramPartToYaml(conf.getConflictEdge().getSource()))
							  .add("target", convertProgramPartToYaml(conf.getConflictEdge().getTarget())).build());
				}
				
				@Override
				public void visitDataConflict(DataConflict<SDGProgramPart> dataConf) {
					visitAbstractConflictLeak("data", dataConf);
					vioBuild[0] = vioBuild[0].add("influenced", convertProgramPartToYaml(dataConf.getInfluenced()));
				}

				@Override
				public void visitOrderConflict(OrderConflict<SDGProgramPart> orderConf) {
					visitAbstractConflictLeak("order", orderConf);
				}

				@Override
				public <L> void visitUnaryViolation(IUnaryViolation<SDGProgramPart, L> unVio) {
					vioBuild[0] = vioBuild[0].add("type", "unary")
							  .add("actual_level", unVio.getActualLevel().toString())
							  .add("expected_level", unVio.getExpectedLevel().toString())
							  .add("node", convertProgramPartToYaml(unVio.getNode()));
				}

				@Override
				public <L> void visitBinaryViolation(IBinaryViolation<SDGProgramPart, L> binVio) {
					vioBuild[0] = vioBuild[0].add("type", "binary")
							  .add("attacker_level", binVio.getAttackerLevel().toString())
							  .add("influenced_by", convertProgramPartToYaml(binVio.getInfluencedBy()))
							  .add("node", convertProgramPartToYaml(binVio.getNode()));
				}
				
			});
			output.add(String
					.format("Security violation: %s (internal: %d security violations on the SDG node level)",
							vio.toString(), groupedIFlows.get(vio)));
			flowsBuilder = flowsBuilder.add(vioBuild[0].build());
		}
		for (String s : output) {
			out.logln(s);
		}
		mapBuilder = mapBuilder.add("flow", flowsBuilder.build());
		ifcAnalysis.getAnnManager().unapplyAllAnnotations();
		return Optional.of(mapBuilder.build());
	}
	
	public boolean selectEntryPoint(JavaMethodSignature sig, Annotation ann, Consumer<List<String>> classSinkConsumer) {
		loc.selectEntry(sig);
		if (ann == null){
			return true;
		}
		Map<String, ElementValue> map = ann.getNamedArguments();
		if (map.containsKey("levels") && map.containsKey("lattice")) {
			if (!parseLatticeAnnotation((ArrayElementValue)map.get("levels"), (ArrayElementValue)map.get("lattice"))){
				return false;
			}
		} else if (map.containsKey("levels") || map.containsKey("lattice")) {
			out.error("The 'levels' and 'lattice' have to be set together in EntryPoint annotations");
			return false;
		}
		if (map.containsKey("datasets")) {
			if (!parseDataSetsAnnotation((ArrayElementValue)map.get("datasets"))){
				return false;
			}
		}
		if (map.containsKey("adversaries")) {
			if (!parseAdversariesAnnotation((ArrayElementValue)map.get("adversaries"))){
				return false;
			}
		}
		if (map.containsKey("pointsToPrecision")) {
			setPointsTo(((edu.kit.joana.ui.annotations.PointsToPrecision)((ConstantElementValue)map.get("pointsToPrecision")).val).name());
		}
		if (map.containsKey("chops")) {
			chopComputation = (ChopComputation)((ConstantElementValue)map.get("chops")).val;
		} else {
			chopComputation = ChopComputation.ALL;
		}
		if (map.containsKey("classSinks")) {
			if (!parseClassSinks((ArrayElementValue)map.get("classSinks"), classSinkConsumer)) {
				return false;
			}
		}
		if (map.containsKey("onlyDirectFlow")) {
			boolean only = ((ConstantElementValue)map.get("chops")).val.equals(true);
			recomputeSDG |= onlyDirectFlow != only;
			onlyDirectFlow = only;
		} else {
			recomputeSDG |= onlyDirectFlow;
			onlyDirectFlow = false;
		}
		if (map.containsKey("pruningPolicy")){
			if (!setPruningPolicy(((AnnotationsReader.EnumElementValue)map.get("pruningPolicy")).enumVal)){
				return false;
			}
		}
		if (map.containsKey("kind")) {
			if (!parseKind((ConstantElementValue)map.get("kind"), Optional.ofNullable((ConstantElementValue)map.getOrDefault("file", null)))) {
				return false;
			}
		}
		if (map.containsKey("uninitializedFieldTypeRegexp")){
			setUninitializedFieldTypeMatcher(t -> t.getName().toString().matches((String)((ConstantElementValue)map.get("uninitializedFieldTypeRegexp")).val));
		}
		return true;
	}

	public void setUninitializedFieldTypeMatcher(UninitializedFieldHelperOptions.FieldTypeMatcher fieldTypeMatcher){
		recomputeSDG |= !uninitializedFieldTypeMatcher.equals(fieldTypeMatcher);
	  this.uninitializedFieldTypeMatcher = fieldTypeMatcher;
	}
	
	private boolean parseClassSinks(ArrayElementValue val, Consumer<List<String>> classSinkConsumer) {
		ElementValue[] arr = val.vals;
		classSinkConsumer.accept(Arrays.stream(arr).map(c -> (String)((ConstantElementValue)c).val).collect(Collectors.toList()));
		return true;
	}
	
	private boolean parseKind(ConstantElementValue kind, Optional<ConstantElementValue> file) {
		switch ((EntryPointKind)kind.val) {
		case UNKNOWN:
			break;
		case CONCURRENT:
			setMHPType(MHPType.PRECISE_UNSAFE.name());
			setComputeInterferences(true);
			break;
		case SEQUENTIAL:
			setMHPType(MHPType.NONE.name());
			setComputeInterferences(false);
			break;
		case FROMFILE:
			if (!file.isPresent()) {
				out.error("Must provide file path when using " + EntryPointKind.FROMFILE);
				return false;
			}
			SDGProgram p;
			try {
				p = SDGProgram.loadSDG((String)file.get().val, MHPType.PRECISE_UNSAFE);
			} catch (IOException e) {
				out.error(e.getMessage());
				return false;
			}
			setSDGProgram(p);
			recomputeSDG = false;
			final PrintStream outs = IOFactory.createUTF8PrintStream(new ByteArrayOutputStream());
			SDGConfig config = new SDGConfig(classPath, loc.getActiveEntry().toBCString(), stubsPath);
			config.setAdditionalEntryMethods(additionalEntryMethods);
			config.setComputeInterferences(computeInterference);
			config.setMhpType(mhpType);
			config.setExceptionAnalysis(excAnalysis);
			config.setPointsToPrecision(pointsTo);
			config.setFieldPropagation(FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION);
			config.setFieldHelperOptions(new UninitializedFieldHelperOptions(uninitializedFieldTypeMatcher));
			config.setInterfaceImplOptions(interfaceImplOptions);
			com.ibm.wala.util.collections.Pair<Long, SDGBuilder.SDGBuilderConfig> pair;
			try {
				pair = SDGBuildPreparation.prepareBuild(outs, SDGProgram.makeBuildPreparationConfig(config), NullProgressMonitor.INSTANCE);
			} catch (ClassHierarchyException | IOException e) {
				out.error(e.getMessage());
				return false;
			}
			final IClassHierarchy cha = pair.snd.cha;
			p.fillWithAnnotations(cha, cha);
		}
		return true;
	}
	
	private boolean parseLatticeAnnotation(ArrayElementValue levelsVal, ArrayElementValue latticeVal) {
		final EditableLatticeSimple<String> specifiedLattice  = new EditableLatticeSimple<String>();
		for (ElementValue o : levelsVal.vals) {
			String level = "";
			if (o instanceof ConstantElementValue && ((ConstantElementValue) o).val instanceof String){
					level = (String) ((ConstantElementValue) o).val;
			} else {
				out.error("Illegal levels specification: " + o + "  - use literal Strings instead (e.g.: { \"low\", \"high\" })");
			}
			specifiedLattice.addElement(level);
		}
		Object[] mayflows = (Object[]) latticeVal.vals;
		for (Object o : mayflows) {
			AnnotationAttribute mayflow = (AnnotationAttribute) o;
			String from = (String)((ConstantElementValue)mayflow.elementValues.get("from")).val;
			String to = (String)((ConstantElementValue)mayflow.elementValues.get("to")).val;
			if(!specifiedLattice.getElements().contains(from)) {
				out.error("Unknown from-level: " + from);
				return false;
			}
			if(!specifiedLattice.getElements().contains(to)) {
				out.error("Unknown to-level: " + from);
				return false;
			}
			specifiedLattice.setImmediatelyGreater(from, to);
		}
		final Collection<String> antiSymmetryViolations = LatticeValidator.findAntisymmetryViolations(specifiedLattice);
		if (antiSymmetryViolations.isEmpty()) {
			this.secLattice = LatticeUtil.dedekindMcNeilleCompletion(specifiedLattice);
			if (ifcAnalysis != null){
				ifcAnalysis.setLattice(secLattice);
			}
		} else {
			out.error("Cycle in user-specified lattice. Elements contained in a cycle: " + antiSymmetryViolations);
			this.secLattice = null;
			if (ifcAnalysis != null){
				ifcAnalysis.setLattice(secLattice);
			}
			return false;
		}
		return true;
	}
	
	private boolean parseDataSetsAnnotation(ArrayElementValue val) {
		final Set<String> datasets  = new HashSet<>();
		Object[] levels = (Object[]) val.vals;
		for (Object o : levels) {
			String dataset = (String) o;
			boolean fresh = datasets.add(dataset);
			if (!fresh) {
				out.error("Duplicate dataset: " + dataset);
				return false;
			}
		}
		final PrecomputedLattice<Set<String>> pre = new PrecomputedLattice<Set<String>>(new PowersetLattice<String>(datasets));
		ifcAnalysis.addAllJavaSourceIncludesAnnotations(pre.getFromOriginalMap(), pre);
		return true;
	}
	
	private boolean parseAdversariesAnnotation(ArrayElementValue val) {
		final Set<String> datasets  = new HashSet<>();
		Object[] levels = (Object[]) val.vals;
		for (Object o : levels) {
			String dataset = (String) o;
			boolean fresh = datasets.add(dataset);
			if (!fresh) {
				out.error("Duplicate adversary: " + dataset);
				return false;
			}
		}
		final PrecomputedLattice<Set<String>> pre = new PrecomputedLattice<Set<String>>(new PowersetLattice<String>(datasets));
		ifcAnalysis.addAllJavaSourceMayKnowAnnotations(pre.getFromOriginalMap(), new ReversedLattice<String>(pre));
		return true;
	}
	
	public boolean searchSinksAndSources(Optional<String> pattern, AnnotationType type) {
		if (ifcAnalysis == null) {
			out.error("Load or build SDG first!");
			return false;
		}
		Pattern pat = pattern.isPresent() ? new Pattern(pattern.get(), true, PatternType.ID, PatternType.SIGNATURE) : new Pattern();
		List<IFCAnnotation> anns = searchSinksAndSources(pat, type);
		if (anns.isEmpty()) {
			return false;
		}
 		YamlSequenceBuilder seqBuilder = Yaml.createYamlSequenceBuilder();
		for (IFCAnnotation ann : anns) {
			seqBuilder = seqBuilder.add(YamlUtil.mapping()
					.add("part", ann.getProgramPart().acceptVisitor(ProgramPartToString.getStandard(), null))
					.add("level", ann.getLevel1())
					.build());
		}
		out.logln(seqBuilder.build().toString());
		return true;
	}

	public List<IFCAnnotation> searchSinksAndSources(Pattern pattern, AnnotationType type) {
		assert ifcAnalysis != null;
		ifcAnalysis.setSourceSinkAnnotationTag(pattern.pattern);
		Pair<Multimap<SDGProgramPart, Pair<Source,String>>,
        	Multimap<SDGProgramPart, Pair<Sink,String>>> anns = ifcAnalysis.getJavaSourceAnnotations();
		List<IFCAnnotation> res = new ArrayList<>();
		if (type == AnnotationType.SOURCE) {
			anns.getFirst().asMap().forEach((part, col) -> {
				col.forEach(p -> {
					if (pattern.matchSource(part, p.getFirst())) {
						String level = p.getFirst().level();
						res.add(new IFCAnnotation(type, level == null ? Level.HIGH : level, part));
					}
				});
			});
		}
		if (type == AnnotationType.SINK) {
			anns.getSecond().asMap().forEach((part, col) -> {
				col.forEach(p -> {
					if (pattern.matchSink(part, p.getFirst())) {
						String level = p.getFirst().level();
						res.add(new IFCAnnotation(type, level == null ? Level.LOW : level, part));
					}
				});
			});
		}
		return res;
	}
	
	public boolean selectSources(Pattern pattern) {
		if (ifcAnalysis == null) {
			out.error("Load or build SDG first!");
			return false;
		}
		List<IFCAnnotation> anns = searchSinksAndSources(pattern, AnnotationType.SOURCE);
		anns.forEach(ann -> {
			ifcAnalysis.addAnnotation(ann);
			out.logln("Selected source '" + ann + "'");
		});
		return true;
	}
	
	public boolean selectSinks(Pattern pattern) {
		if (ifcAnalysis == null) {
			out.error("Load or build SDG first!");
			return false;
		}
		List<IFCAnnotation> anns = searchSinksAndSources(pattern, AnnotationType.SINK);
		anns.forEach(ann -> {
			ifcAnalysis.addAnnotation(ann);
			out.logln("Selected sink '" + ann + "'");
		});
		return true;
	}
	
	public List<Pair<IFCAnnotation, List<String>>> searchDeclassificationAnnotations(Optional<String> pattern) {
		if (ifcAnalysis == null) {
			out.error("Load or build SDG first!");
			return Collections.emptyList();
		}
		Pattern pat = pattern.isPresent() ? new Pattern(pattern.get(), true, PatternType.ID, PatternType.SIGNATURE) : new Pattern();
		return searchDeclassificationAnnotations(pat);
	}

	public List<Pair<IFCAnnotation, List<String>>> searchDeclassificationAnnotations(Pattern pattern) {
		assert ifcAnalysis != null;
		ifcAnalysis.setSourceSinkAnnotationTag(pattern.pattern);
        Multimap<SDGProgramPart, Pair<Declassification, String>> anns = ifcAnalysis.getJavaSourceAnnotations(false).getSecond();
		List<Pair<IFCAnnotation, List<String>>> res = new ArrayList<>();
		anns.asMap().forEach((part, col) -> {
			col.forEach(p -> {
				if (pattern.matchDeclassification(part, p.getFirst())) {
					Declassification declass = p.getFirst();
					res.add(Pair.pair(new IFCAnnotation(
							declass.from() == null ? Level.HIGH : declass.from(), 
							declass.to() == null ? Level.LOW : declass.to(),
							part), Arrays.asList(p.getFirst().tags())));
				}
			});
		});
		return res;
	}
	
	@SuppressWarnings("unchecked")
	public boolean showDeclassificationAnnotations(Optional<String> pattern) {
		List<Pair<IFCAnnotation, List<String>>> annPairs = searchDeclassificationAnnotations(pattern);
		if (annPairs.isEmpty()) {
			return false;
		}
		YamlSequenceBuilder seq = Yaml.createYamlSequenceBuilder();
		for (Pair<IFCAnnotation, List<String>> annPair : annPairs) {
			IFCAnnotation ann = annPair.getFirst();
			seq = seq.add(YamlUtil.mapping()
					.add("program_part", ann.getProgramPart().toString())
					.add("from", ann.getLevel1())
					.add("to", ann.getLevel2())
					.add("tags", new YamlCollectionDump((List<Object>)(List<?>)annPair.getSecond()).represent())
					.build());
		}
		out.logln(seq.toString());
		return true;
	}
	
	public boolean selectDeclassificationAnnotations(Optional<String> pattern) {
		List<Pair<IFCAnnotation, List<String>>> annPairs = searchDeclassificationAnnotations(pattern);
		annPairs.forEach(p -> ifcAnalysis.addAnnotation(p.getFirst()));
		return true;
	}
	
	public void setClasspath(String newClasspath) {
		this.classPath = newClasspath;
	}

	public void setPointsTo(final String newPts) {
		for (final PointsToPrecision pts : PointsToPrecision.values()) {
			if (pts.name().equals(newPts)) {
				recomputeSDG |= pointsTo != pts;
				this.pointsTo = pts;
				break;
			}
		}
	}

	public void setExceptionAnalysis(final String newExc) {
		for (final ExceptionAnalysis exc : ExceptionAnalysis.values()) {
			if (exc.name().equals(newExc)) {
				recomputeSDG |= excAnalysis != exc;
				this.excAnalysis = exc;
				break;
			}
		}
	}

	public void setMHPType(final String newMHPType) {
		for (final MHPType mhp : MHPType.values()) {
			if (mhp.name().equals(newMHPType)) {
				recomputeSDG |= mhpType != mhp;
				this.mhpType = mhp;
				break;
			}
		}
	}

	public void setComputeInterferences(boolean cmpInt) {
		recomputeSDG |= computeInterference != cmpInt;
		this.computeInterference = cmpInt;
	}

	public Stubs getStubsPath() {
		return stubsPath;
	}

	public void setStubsPath(Stubs newStubsPath) {
		recomputeSDG |= stubsPath != newStubsPath;
		this.stubsPath = newStubsPath;
	}

	public Collection<String> getSecurityLevels() {
		return ifcAnalysis.getLattice().getElements();
	}

	private boolean inSecurityLattice(String level) {
		return ifcAnalysis.getLattice().getElements().contains(level);
	}

	private boolean greaterOrEqual(String level1, String level2) {
		return ifcAnalysis.getLattice().leastUpperBound(level1, level2).equals(level1);
	}

	public void displayLastSearchResults() {
		if (methodSelector.noSearchResults()) {
			out.info("No search results.");
		} else {
			for (int i = 0; i < methodSelector.numberOfSearchResults(); i++) {
				out.logln("[" + i + "] " + methodSelector.getMethod(i));
			}
		}
	}

	public Collection<SDGClass> getClasses() {
		return ifcAnalysis.getProgram().getClasses();
	}

	public SDGProgramPart getProgramPartFromSelectorString(String desc, boolean silent) {
		return ifcAnalysis.getProgramPart(desc);
	}

	public String getSelectorStringFromMethodPart(SDGProgramPart part) {
		return SDGProgramPartWriter.getStandardVersion().writeSDGProgramPart(part);
	}

	public boolean displayActiveMethod() {
		if (methodSelector.lastSearchResultEmpty()) {
			out.info("No method to select");
			return false;
		} else {
			if (!methodSelector.methodSelected()) {
				out.info("No method selected.");
				return false;
			}
			displayMethod(methodSelector.getActiveMethod());
			return true;
		}
	}

	public void displayMethod(SDGMethod m) {
		int mIndex = methodSelector.getIndex(m);
		out.logln("Displaying method " + m.getSignature().toHRString());
		out.logln("Parameters: ");
		for (SDGFormalParameter p : m.getParameters()) {
			out.logln("[p" + p.getIndex() + "] " + p);
		}
		out.logln("Instructions: ");
		for (int i = 0; i < m.getNumberOfInstructions(); i++) {
			out.logln("[m" + mIndex + "->i" + i + "] " + m.getInstruction(i));
		}
	}

	private void setSDGProgram(SDGProgram newSDGProgram) {
		if (ifcAnalysis == null) {
			ifcAnalysis = new IFCAnalysis(newSDGProgram, this.secLattice);
		} else {
			ifcAnalysis.setProgram(newSDGProgram);
			ifcAnalysis.setLattice(this.secLattice);
		}
		recomputeSDG = true;
	}

	private void setSDG(SDG newSDG, MHPAnalysis mhp) {
		setSDGProgram(new SDGProgram(newSDG, mhp, annotateOverloadedMethods));
	}

	public void displayCurrentConfig() {
		out.logln("classpath = " + classPath);
		out.logln("entry = " + (loc.getActiveEntry() == null ? "<none>" : loc.getActiveEntry()));
		out.logln("output directory = " + outputDirectory);
		out.logln("points-to = " + pointsTo.desc);
		out.logln("lattice = " + latticeFile);
		// out.logln("sdg = " + sdgFile);

	}

	private Integer parseInteger(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public boolean loadLattice(String latFile) {

		FileInputStream in;

		try {
			in = new FileInputStream(latFile);
		} catch (FileNotFoundException fnfe) {
			out.error("File " + latFile + " not found!");
			return false;
		}

		IEditableLattice<String> lattice;

		try {
			lattice = LatticeUtil.loadLattice(in);
		} catch (WrongLatticeDefinitionException wlde) {
			out.error("Lattice specified in " + latFile + " is invalid! Old lattice is left untouched!");
			return false;
		} catch (IOException ioe) {
			out.error("I/O error while reading lattice from file " + latFile + "!");
			return false;
		}

		if (checkAndSetLattice(lattice)) {
			latticeFile = latFile;
			return true;
		} else {
			return false;
		}
	}

	private boolean checkAndSetLattice(IStaticLattice<String> l0) {
		try {
			l0.getBottom();
		} catch (InvalidLatticeException e) {
			out.error("Wrong lattice definition! Specified partial order has no bottom element and is thus no lattice! Old lattice is left untouched!");
			return false;
		}
		for (String s : l0.getElements()) {
			for (String t : l0.getElements()) {
				try {
					l0.leastUpperBound(s, t);
				} catch (InvalidLatticeException e) {
					out.error("Specified partial order is no lattice! Elements " + s + " and " + t
							+ " have no least upper bound! Old lattice is left untouched!");
					return false;
				}
			}
		}
		this.secLattice = l0;
		if (this.ifcAnalysis != null) {
			this.ifcAnalysis.setLattice(l0);
		}
		return true;
	}

	/**
	 * Sets the lattice used from now on.
	 *
	 * @param latticeSpec
	 *            either one of the constants for the built-in lattices (
	 *            {@link #LATTICE_BINARY}, {@link #LATTICE_TERNARY},
	 *            {@link #LATTICE_DIAMOND}, or a comma-separated inequalities
	 *            specifying a user-defined lattice.
	 * @return {@code true} if latticeSpec specifies one of the predefined
	 *         lattices (see above) or a valid user-defined lattice,
	 *         {@code false} otherwise.
	 */
	public boolean setLattice(String latticeSpec) {
		IStaticLattice<String> newLattice;
		latticeFile = "[preset: " + latticeSpec + "]";
		if (LATTICE_BINARY.equals(latticeSpec)) {
			newLattice = BuiltinLattices.getBinaryLattice();
		} else if (LATTICE_TERNARY.equals(latticeSpec)) {
			newLattice = BuiltinLattices.getTernaryLattice();
		} else if (LATTICE_DIAMOND.equals(latticeSpec)) {
			newLattice = BuiltinLattices.getDiamondLattice();
		} else {
			latticeSpec = latticeSpec.replaceAll("\\s*,\\s*", "\n");
			try {
				newLattice = LatticeUtil.loadLattice(latticeSpec);
			} catch (WrongLatticeDefinitionException e) {
				out.error("Error while parsing lattice: " + e.getMessage() + " Old lattice is left untouched!");
				return false;
			}
			latticeFile = "[user-defined: " + latticeSpec + "]";
		}
		if (checkAndSetLattice(newLattice)) {
			out.logln("current lattice: " + latticeFile);
		}
		return checkAndSetLattice(newLattice);
	}

	public CMD searchCommand(final String cmdstr) {
		CMD cmd = null;
		String[] parts = cmdstr.split("\\s+");
		if (repo.knowsCommand(parts[0])) {
			cmd = repo.getCommand(parts[0]);
		}

		return cmd;
	}

	public synchronized boolean processCommand(final CMD cmd, final String[] parts) {
		beforeCommand(cmd, parts);
		boolean success;

		try {
			success = executeAndLogCommand(cmd, parts);
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));

			out.error("Error: " + sw.toString());
			e.printStackTrace();
			success = false;
		}

		afterCommand(cmd, parts);

		return success;

	}

	private void beforeCommand(final CMD cmd, final String[] parts) {

		for (IFCConsoleListener l : consoleListeners) {
			l.cmdIssued(cmd, parts);
		}
	}

	private void afterCommand(final CMD cmd, final String[] parts) {
		for (IFCConsoleListener l : consoleListeners) {
			l.cmdDone(cmd, parts);
		}
	}

	private synchronized boolean executeAndLogCommand(final CMD cmd, final String[] parts) {
		boolean success;
		if (parts.length - 1 < cmd.getMinArity() || parts.length - 1 > cmd.getMaxArity()) {
			if (cmd.getMinArity() < cmd.getMaxArity()) {
				out.error("Invalid number of parameters. Command " + parts[0] + " expects between " + cmd.getMinArity()
						+ " and " + cmd.getMaxArity() + " parameters.");
			} else {
				out.error("Invalid number of parameters. Command " + parts[0] + " expects " + cmd.getMinArity()
						+ " parameters.");
			}
			success = false;
		} else {
			success = repo.executeCommand(cmd, parts);

			if (success) {
				switch (cmd) {
				case LOADSCRIPT:
				case SAVESCRIPT:
				case QUIT:
					break;
				default:
					script.add(glueTogether(parts));
					break;
				}
			}
		}
		notifyAll();
		return success;
	}

	private String glueTogether(String[] parts) {
		StringBuffer sb = new StringBuffer();
		for (String part : parts) {
			sb.append(part + " ");
		}
		sb.replace(sb.length() - 1, sb.length(), "");
		return sb.toString();
	}

	public synchronized boolean processCommand(final String cmd) {

		String[] parts = cmd.split("\\s+");
		if (!repo.knowsCommand(parts[0])) {
			out.error("Command not found: " + cmd);
			List<String> commands = findCommands(parts[0]);
			if (commands.size() > 0) {
				out.error("Did you mean:");
				for (String command : commands) {
					out.error(repo.getHelpMessage(command));
				}
			}
			notify();
			return false;
		} else {
			CMD c = searchCommand(cmd);
			return processCommand(c, parts);
		}

	}
	
	public List<String> findCommands(String needle){
		List<String> commands = new ArrayList<>();
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(needle, java.util.regex.Pattern.CASE_INSENSITIVE);
		for (String cmdName : repo.getCommands()) {
			String msg = repo.getHelpMessage(cmdName);
			if (pattern.matcher(msg).find()) {
				commands.add(cmdName);
			}
		}
		return commands;
	}

	public boolean saveInstructions(String filename) {
		PrintStream fileOut;

		try {
			fileOut = IOFactory.createUTF8PrintStream(new FileOutputStream(filename));
		} catch (FileNotFoundException fnfe) {
			out.error("File " + filename + " not found!");
			return false;
		}

		for (String instruction : script) {
			fileOut.println(instruction);
			out.logln("Instruction " + instruction + " written into file " + filename);
		}
		fileOut.close();
		return true;
	}

	public boolean loadInstructions(String filename) {
		BufferedReader fileIn;

		try {
			fileIn = new BufferedReader(IOFactory.createUTF8ISReader(new FileInputStream(filename)));
		} catch (FileNotFoundException fnfe) {
			out.error("File " + filename + " not found!");
			return false;
		}

		try {
			String nextLine = fileIn.readLine();
			while (nextLine != null) {
				if (!nextLine.startsWith("#") && !nextLine.replaceAll("\\s", "").equals("")) {
					if (!processCommand(nextLine)) {
						return false;
					}
				}
				nextLine = fileIn.readLine();
			}
		} catch (IOException ioe) {
			out.error("I/O error while reading from file " + filename + "!");
			return false;
		} finally {
			try {
				fileIn.close();
			} catch (IOException ioe) {
				out.error("I/O error while closing file " + filename + "!");
				return false;
			}
		}



		return true;
	}

	public void showAnnotations() {
		new NumberedIFCAnnotationDumper(out.getPrintStream()).dumpAnnotations(ifcAnalysis
				.getAnnotations());
	}

	public void showUsageOutline() {
		out.logln("Available commands:");
		for (String cmdName : repo.getCommands()) {
			out.logln(repo.getHelpMessage(cmdName));
		}
		out.logln("Or invoke 'quit' to exit.");
	}

	public boolean loadAnnotations(String fileName) {
		ifcAnalysis.clearAllAnnotations();
		Set<IFCAnnotation> annotations;
		try {
			annotations = new IFCAnnotationReader(methodSelector, new FileInputStream(fileName)).readAnnotations();
		} catch (FileNotFoundException fnfe) {
			out.error("File " + fileName + " not found!");
			return false;
		} catch (IOException ioe) {
			out.error("I/O error while reading annotations from file " + fileName + "!");
			return false;
		} catch (InvalidAnnotationFormatException iafe) {
			out.error("Annotation " + iafe.getInvalidAnnotation() + " in file " + fileName + " has illegal format!");
			return false;
		} catch (MethodNotFoundException mnfe) {
			out.error("Annotation " + mnfe.getAnnotation() + " in file " + fileName + " refers to missing method "
					+ mnfe.getMethodName() + "!");
			return false;
		}

		for (IFCAnnotation annotation : annotations) {
			ifcAnalysis.addAnnotation(annotation);
		}
		showAnnotations();
		return true;
	}

	public boolean saveAnnotations(String fileName) {
		IFCAnnotationDumper dumper;

		try {
			dumper = new IFCAnnotationDumper(IOFactory.createUTF8PrintStream(new FileOutputStream(fileName)));
		} catch (FileNotFoundException e) {
			out.error("File " + fileName + " not found!");
			return false;
		}

		dumper.dumpAnnotations(ifcAnalysis.getAnnotations());
		return true;
	}

	public void setProgressMonitor(IProgressMonitor progress) {
		this.monitor = progress;
	}
	
	public synchronized boolean buildSDGIfNeeded() {
		if (recomputeSDG) {
			return buildSDG();
		}
		return true;
	}

	public synchronized boolean buildSDG() {
		return buildSDG(false, MHPType.NONE, ExceptionAnalysis.INTERPROC);
	}
	
	private boolean buildSDGIfNeeded(boolean computeInterference, MHPType mhpType, ExceptionAnalysis exA) {
		if (recomputeSDG) {
			return buildSDG(computeInterference, mhpType, exA);
		}
		return true;
	}

	private boolean buildSDG(boolean computeInterference, MHPType mhpType, ExceptionAnalysis exA) {
		if (!loc.entrySelected() && additionalEntryMethods.isEmpty()) {
			out.error("No entry method selected. Select entry method first!");
			return false;
		}
		Optional<SDGProgram> sdg = createSDG(classPath, computeInterference, mhpType, exA);
		if (sdg.isPresent()){
			setSDGProgram(sdg.get());
			recomputeSDG = false;
			return true;
		}
		recomputeSDG = true;
		return false;
	}

	SetValuePass createSetValuePass(){
		Map<String, edu.kit.joana.setter.misc.Pair<String, ValueToSet.Mode>> vToS =
				valuesToSet.entrySet().stream().filter(e -> ImprovedCLI.programPartToString(e.getKey()) != null).collect(
						Collectors.toMap(e -> ImprovedCLI.programPartToString(e.getKey()),
															e -> new edu.kit.joana.setter.misc.Pair<>(e.getValue().getFirst(), e.getValue().getSecond())));
		return new SetValuePass(new Tool(new SetValueStore(), SetValue.class)){
			@Override public void collect(Path file) throws IOException {
				tool.searchAnnotations(file, null, new SearchVisitor.Matcher() {
					@Override public Optional<edu.kit.joana.setter.misc.Pair<String, ValueToSet.Mode>> matchField(
							String fullyQualifiedClassName, String fieldName) {
						return Optional.ofNullable(vToS.getOrDefault(fullyQualifiedClassName + "#" + fieldName, null));
					}

					@Override public Optional<edu.kit.joana.setter.misc.Pair<String, ValueToSet.Mode>> matchMethodReturn(
							String fullyQualifiedClassName, String methodName, String desc, List<String> parameterTypes) {
						return Optional.ofNullable(vToS.getOrDefault(fullyQualifiedClassName + "." + methodName + desc, null));
					}

					@Override public Optional<edu.kit.joana.setter.misc.Pair<String, ValueToSet.Mode>> matchMethodParameter(
							String fullyQualifiedClassName, String methodName, String desc, List<String> parameterTypes, int parameterNumber) {
						return Optional.ofNullable(vToS.getOrDefault(fullyQualifiedClassName + "." + methodName + desc + "->" + parameterNumber, null));
					}
				});
			}
		};
	}

	private Optional<SDGProgram> createSDG(String classPath, boolean computeInterference, MHPType mhpType, ExceptionAnalysis exA){
		return createSDG(classPath, computeInterference, mhpType, exA, true);
	}

	private Optional<SDGProgram> createSDG(String classPath, boolean computeInterference, MHPType mhpType, ExceptionAnalysis exA, boolean setValues){
		try {
			if (this.valuesToSet.size() > 0){
				classPath = new PreProcPasses(createSetValuePass()).process(null, "", classPath);
				classPathAfterOpt = classPath;
			}
			String entryPoint = "";
			if (loc.getActiveEntry() != null){
				entryPoint = loc.getActiveEntry().toBCString();
			} else {
				entryPoint = additionalEntryMethods.iterator().next();
			}
			SDGConfig config = new SDGConfig(classPath, entryPoint, stubsPath);
			config.setAdditionalEntryMethods(additionalEntryMethods);
			config.setPruningPolicy(pruningPolicy);
			config.setComputeInterferences(computeInterference);
			config.setMhpType(mhpType);
			config.setExceptionAnalysis(exA);
			config.setPointsToPrecision(pointsTo);
			config.setFieldPropagation(FieldPropagation.OBJ_GRAPH_SIMPLE_PROPAGATION);
			config.setFieldHelperOptions(new UninitializedFieldHelperOptions(uninitializedFieldTypeMatcher));
			config.setInterfaceImplOptions(interfaceImplOptions);
			config.setAnnotateOverloadingMethods(annotateOverloadedMethods);
			SDGProgram program = SDGProgram.createSDGProgram(config, out.getPrintStream(), monitor);
			if (onlyDirectFlow) {
				SDGProgram.throwAwayControlDeps(program.getSDG());
			}
			return Optional.of(program);
		} catch (ClassHierarchyException e) {
			out.error(e.getMessage());
			return Optional.empty();
		} catch (IOException e) {
			out.error("\nI/O problem during sdg creation: " + e.getMessage());
			return Optional.empty();
		} catch (CancelException e) {
			out.error("\nSDG creation cancelled.");
			return Optional.empty();
		} catch (UnsoundGraphException e) {
			out.error("\nResulting SDG is not sound: " + e.getMessage());
			return Optional.empty();
		}
	}

	PreProcPasses createOptPasses(){
		return new PreProcPasses(new ProGuardPass());
	}

	public synchronized boolean saveSDG(String path) {
		if (ifcAnalysis == null || ifcAnalysis.getProgram() == null) {
			out.info("No active program.");
		} else {
			BufferedOutputStream bOut;

			try {
				bOut = new BufferedOutputStream(new FileOutputStream(path));
			} catch (FileNotFoundException e) {
				out.error("I/O problem while writing sdg into file " + path + "!");
				return false;
			}
			SDGSerializer.toPDGFormat(ifcAnalysis.getProgram().getSDG(), bOut);
		}

		return true;
	}
	
	public synchronized boolean exportGraphML(String path) {
		if (ifcAnalysis == null || ifcAnalysis.getProgram() == null) {
			out.info("No active program.");
		} else {
			BufferedOutputStream bOut;
			
			try {
				bOut = new BufferedOutputStream(new FileOutputStream(path));
			} catch (FileNotFoundException e) {
				out.error("I/O problem while exporting GraphML into file " + path + "!");
				return false;
			}
			
			final SDG sdg = ifcAnalysis.getProgram().getSDG();
			try {
				SDG2GraphML.convertHierachical(sdg, bOut);
			} catch (XMLStreamException e) {
				out.error("A problem occured while exporting the SDG to GraphML." +
				          "This is most certainly a bug, please report it to the JOANA developers.");
				return false;
			}
		}
		
		return true;
	}

	public synchronized boolean loadSDG(String path, MHPType mhpType) {

		SDG sdg;
		MHPAnalysis mhp;

		try {
			sdg = SDG.readFrom(path, new SecurityNodeFactory());
			mhp = mhpType.getMhpAnalysisConstructor().apply(sdg);
			
		} catch (IOException e) {
			String errorMsg = "I/O error while reading sdg from file " + path;
			if (e.getMessage() != null) {
				errorMsg += '\n' + e.getMessage();
			}
			out.error(errorMsg);
			return false;
		}

		setSDG(sdg, mhp);
		// sdgFile = path;
		return true;
	}

	public void reset() {
		ifcAnalysis.clearAllAnnotations();
		recomputeSDG |= setValueStore.clear();
		methodSelector.reset();
	}

	public Collection<? extends IViolation<SecurityNode>> getLastAnalysisResult() {
		return lastAnalysisResult;
	}

	public TObjectIntMap<IViolation<SDGProgramPart>> getLastAnalysisResultGrouped() {
		return groupedIFlows;
	}

	public boolean doIFC(IFCType ifcType, boolean timeSens) {
		if (ifcAnalysis == null || ifcAnalysis.getProgram() == null) {
			out.info("No program to analyze.");
			return false;
		} else {
			ifcAnalysis.setTimesensitivity(timeSens);
			out.logln("Performing IFC - Analysis type: " + ifcType);
			Optional<Collection<? extends IViolation<SecurityNode>>> viosOpt = doIFCAndOptAndCatch(ifcType);
			if (!viosOpt.isPresent()){
				return false;
			}
			Collection<? extends IViolation<SecurityNode>> vios = viosOpt.get();

			lastAnalysisResult.clear();
			lastAnalysisResult.addAll(vios);

			groupedIFlows.clear();

			if (lastAnalysisResult.isEmpty()) {
				out.logln("No violations found.");
			} else {
				groupedIFlows = ifcAnalysis.groupByPPPart(vios);
				out.logln("done, found " + groupedIFlows.size() + " security violation(s):");
				Set<String> output = new TreeSet<String>();
				for (IViolation<SDGProgramPart> vio : groupedIFlows.keySet()) {
					output.add(String
							.format("Security violation: %s (internal: %d security violations on the SDG node level)",
									vio.toString(), groupedIFlows.get(vio)));
				}
				for (String s : output) {
					out.logln(s);
				}
			}
			return true;
		}
	}

	/**
	 * Do the chosen byte code optimizations and run the IFC analysis
	 */
	public Optional<Collection<? extends IViolation<SecurityNode>>> doIFCAndOptAndCatch(IFCType ifcType) {
		return doIFCAndOptAndCatch(ifcType, true);
	}

	/**
	 * Do the chosen byte code optimizations and run the IFC analysis
	 */
	public Optional<Collection<? extends IViolation<SecurityNode>>> doIFCAndOptAndCatch(IFCType ifcType, boolean unapplyAllAnnotations) {
		try {
			return Optional.of(doIFCAndOpt(ifcType, unapplyAllAnnotations));
		} catch (IOException ex){
			out.error(ex.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Do the chosen byte code optimizations and run the IFC analysis
	 */
	public Collection<? extends IViolation<SecurityNode>> doIFCAndOpt(IFCType ifcType, boolean unapplyAllAnnotations) throws IOException {
		Collection<IFCAnnotation> annotations = ifcAnalysis.getAnnotations();
		if (useByteCodeOptimizations) {
			if (this.valuesToSet.size() > 0){
				classPath = new PreProcPasses(createSetValuePass()).process(null, "", classPath);
				classPathAfterOpt = classPath;
			}
			PreProcPasses passes = createOptPasses();
			passes.processAndUpdateSDG(ifcAnalysis, optLibPath,
					classPathAfterOpt == null ? classPath : classPathAfterOpt,
					cp -> createSDG(cp, computeInterference, mhpType, excAnalysis, false).get());
		}
		annotations.forEach(ifcAnalysis::addAnnotation);
		ifcAnalysis.setTimesensitivity(isTimeSensitive);
		return ifcAnalysis.doIFC(ifcType, unapplyAllAnnotations);
	}

		public Optional<String> optimizeClassPath(String libPath){
			PreProcPasses passes = createOptPasses();
			try {
				return Optional.of(passes.process(ifcAnalysis, libPath,
						classPathAfterOpt == null ? classPath : classPathAfterOpt));
			} catch (IOException e) {
				e.printStackTrace();
				out.error(e.getMessage());
				return Optional.empty();
			}
		}

    public Set<edu.kit.joana.api.sdg.SDGInstruction> getLastComputedChop() {
        return this.lastComputedChop;
    }

    public boolean createChop(final String source, final String sink) {
        final SDGProgramPart sourceP = getProgramPartFromSelectorString(source, false);
        final SDGProgramPart sinkP = getProgramPartFromSelectorString(sink, false);
        return createChop(sourceP, sinkP);
    }

    public boolean createChop(final SDGProgramPart source, final SDGProgramPart sink) {
        final SDGProgram program = getProgram();
        
        if (source == null) {
            out.info("Chop: Source is null - aborted");
            return false;
        }
        if (sink == null) {
            out.info("Chop: Sink is null - aborted");
            return false;
        }
        if (program == null) {
            out.info("No program loaded");
            return false;
        }

        this.out.logln("Calculating Chop from " + source + " to " + sink + "...");

        final Set<edu.kit.joana.api.sdg.SDGInstruction> chop = program.computeInstructionChop(source, sink);
        this.lastComputedChop = chop;

        out.logln("Chop from " + source + " to " + sink + " is:");
        for (final edu.kit.joana.api.sdg.SDGInstruction inst : chop) {
            out.logln("  " + inst);
        }
        return true;
    }

	public static String convertIFCType(IFCType ifcType) {
		switch (ifcType) {
		case CLASSICAL_NI:
			return IFCTYPE_CLASSICAL_NI;
		case LSOD:
			return IFCTYPE_LSOD;
		case RLSOD:
			return IFCTYPE_RLSOD;
		case iRLSOD:
			return IFCTYPE_iRLSOD;
		default:
			throw new IllegalStateException("not all cases handled by this method!");
		}
	}

	public boolean showClasses() {
		if (ifcAnalysis == null) {
			out.error("Load or build SDG first!");
			return false;
		}
		Collection<SDGClass> classes = ifcAnalysis.getProgram().getClasses();

		for (SDGClass cl : classes) {
			out.logln(cl.getDescription());
		}

		return true;
	}

	public void interactive() throws IOException {
		String nextCommand = null;
		boolean quit = false;
		while (!quit) {
			if (showPrompt) {
				out.log("> ");
			}
			nextCommand = in.readLine();
			if (nextCommand == null) {
				quit = true;
			} else if (isQuit(nextCommand)) {
				quit = true;
			} else {
				processCommand(nextCommand);
			}
		}
	}

	public void setShowPrompt(boolean showPrompt) {
		this.showPrompt = showPrompt;
	}

	public boolean isQuit(String cmd) {
		return CMD.QUIT.getName().equals(cmd);
	}

	public synchronized EntryLocator getEntryLocator() {
		return loc;
	}

	public boolean setClassPath(String classPath) {
		recomputeSDG |= !this.classPath.equals(classPath);
		this.classPath = classPath;
		return true;
	}

	public String getClassPath() {
		return classPath;
	}

	public PointsToPrecision getPointsTo() {
		return pointsTo;
	}

	public ExceptionAnalysis getExceptionAnalysis() {
		return excAnalysis;
	}

	public boolean getComputeInterferences() {
		return computeInterference;
	}

	public MHPType getMHPType() {
		return mhpType;
	}

  public Collection<IFCAnnotation> getSources() {
		if (ifcAnalysis == null) {
			return new LinkedList<IFCAnnotation>();
		} else {
			return ifcAnalysis.getSources();
		}
	}

	public Collection<IFCAnnotation> getSinks() {
		if (ifcAnalysis == null) {
			return new LinkedList<IFCAnnotation>();
		} else {
			return ifcAnalysis.getSinks();
		}
	}

	public Collection<IFCAnnotation> getDeclassifications() {
		if (ifcAnalysis == null) {
			return new LinkedList<IFCAnnotation>();
		} else {
			return ifcAnalysis.getDeclassifications();
		}
	}

	public String getLatticeFile() {
		return latticeFile;
	}

	public SDG getSDG() {
		if (ifcAnalysis == null) {
			return null;
		} else {
			return ifcAnalysis.getProgram().getSDG();
		}
	}

	public void addListener(IFCConsoleListener l) {
		this.consoleListeners.add(l);
	}

	public IStaticLattice<String> getLattice() {
		return ifcAnalysis.getLattice();
	}

	public boolean canAnnotate(Collection<SDGProgramPart> selectedParts, AnnotationType type) {
		boolean ret = true;

		for (SDGProgramPart part : selectedParts) {
			if (!canAnnotate(part)) {
				ret = false;
			}
		}

		if (ret) {
			return true;
		}
		
		Answer ans = out
				.question("At least one of the selected program parts is already annotated. Do you want to overwrite these annotations?");
		return ans == Answer.YES;
	}

	public boolean canAnnotate(SDGProgramPart part) {
		return !ifcAnalysis.isAnnotated(part);
	}

	public void executionAborted(CMD cmd, String[] args, Throwable error) {
		List<String> argList;
		if (args.length == 1) {
			argList = Collections.<String> emptyList();
		} else {
			argList = Arrays.asList(args);
			argList = argList.subList(1, argList.size() - 1);
		}
		out.error("Execution of command " + cmd + " applied to arguments " + argList
				+ " aborted due to the following error: " + error);

		afterCommand(cmd, args);
	}

	/**
	 * Returns the program currently under analysis, if there is any. Returns
	 * {@code null} if no program was loaded.
	 *
	 * @return the program currently under analysis, {@code null} if there is
	 *         none
	 */
	public SDGProgram getProgram() {
		if (ifcAnalysis == null) {
			return null;
		} else {
			return ifcAnalysis.getProgram();
		}
	}

	public boolean setPruningPolicy(String policy){
		try {
			PruningPolicy newPolicy = PruningPolicy.valueOf(policy.toUpperCase());
			recomputeSDG |= newPolicy != pruningPolicy;
			pruningPolicy = newPolicy;
			return true;
		} catch (IllegalArgumentException ex){
			out.error("No such pruning policy " + policy);
			return false;
		}
	}

	boolean selectSetValues(String tag){
		Tool tool = new Tool(setValueStore, SetValue.class);
		try {
			new PreProcPasses(new FilePass() {
				@Override public void setup(String libClassPath) {

				}

				@Override public void collect(Path file) throws IOException {
					tool.searchAnnotations(file, tag, new SearchVisitor.Matcher(){});
				}

				@Override public void store(Path source, Path target) throws IOException {

				}

				@Override public void teardown() {

				}

				@Override public boolean requiresKnowledgeOnAnnotations() {
					return false;
				}
			}).process(null, "", classPath);
		} catch (IOException e) {
			out.error(e.getMessage());
			return false;
		}
		return true;
	}

	SetValueStore searchSetValues(String tag){
		SetValueStore store = new SetValueStore();
		Tool tool = new Tool(store, SetValue.class);
		try {
			new PreProcPasses(new FilePass() {
				@Override public void setup(String libClassPath) {

				}

				@Override public void collect(Path file) throws IOException {
					tool.searchAnnotations(file, tag, new SearchVisitor.Matcher(){});
				}

				@Override public void store(Path source, Path target) throws IOException {

				}

				@Override public void teardown() {

				}

				@Override public boolean requiresKnowledgeOnAnnotations() {
					return false;
				}
			}).process(null, "", classPath);
		} catch (IOException e) {
			out.error(e.getMessage());
			return null;
		}
		return store;
	}

	void showSetValues(){
		YamlMappingBuilder mapping = YamlUtil.mapping();
		for (SetValueStore.ClassAnnotation classAnn : setValueStore.getClassAnnotations()) {
			YamlMappingBuilder classMapping = YamlUtil.mapping();
			if (classAnn.methodAnns.size() > 0) {
				YamlMappingBuilder methodsMapping = YamlUtil.mapping();
				for (Map.Entry<SetValueStore.MethodIdentifier, SetValueStore.MethodAnnotation> methodAnnEntry : classAnn.methodAnns.entrySet()) {
					YamlMappingBuilder methodMapping = YamlUtil.mapping();
					SetValueStore.MethodAnnotation methodAnn = methodAnnEntry.getValue();
					if (methodAnn.returnAnn.isPresent()) {
						methodMapping = methodMapping.add("return", methodAnn.returnAnn.get().value);
					}
					for (Map.Entry<Integer, SetValueStore.ParameterAnnotation> paramEntry : methodAnn.paramAnns.entrySet()) {
						methodMapping = methodsMapping.add(paramEntry.getKey().toString(), paramEntry.getValue().valueToSet.value);
					}
					methodsMapping = methodsMapping.add(methodAnnEntry.getKey().name + methodAnnEntry.getKey().descriptor, methodMapping.build());
				}
				classMapping = classMapping.add("methods", methodsMapping.build());
			}
			if (classAnn.fieldAnns.size() > 0) {
				YamlMappingBuilder fieldsMapping = YamlUtil.mapping();
				for (Map.Entry<String, ValueToSet> fieldEntry : classAnn.fieldAnns.entrySet()) {
					fieldsMapping = fieldsMapping.add(fieldEntry.getKey(), fieldEntry.getValue().value);
				}
				classMapping = classMapping.add("fields", fieldsMapping.build());
			}
			mapping = mapping.add(classAnn.className, classMapping.build());
		}
		out.logln(mapping.build().toString());
	}

	boolean setValueOfProgramPart(String programPart, String value, ValueToSet.Mode mode){
		if (programPart.contains("(")){
			String[] parts = programPart.split("[()]");
			String[] methodNameParts = parts[0].split("\\.");
			String className = String.join(".", Arrays.asList(methodNameParts).subList(0, methodNameParts.length - 1));
			String exMethodName = methodNameParts[methodNameParts.length - 1];
			String[] args = parts[1].split(",");
			if (programPart.contains("->")){
				int num = Integer.parseInt(parts[2].split("->")[1]);
				return setValue(new SearchVisitor.Matcher() {
					@Override public Optional<edu.kit.joana.setter.misc.Pair<String, ValueToSet.Mode>> matchMethodParameter(String fullyQualifiedClassName, String methodName,
							String desc, List<String> parameterTypes, int parameterNumber) {
						if (fullyQualifiedClassName.equals(className) && methodName.equals(exMethodName) && parameterTypes.equals(Arrays.asList(args))
							&& parameterNumber == num){
							return Optional.of(new edu.kit.joana.setter.misc.Pair<>(value, mode));
						}
						return Optional.empty();
					}
				});
			} else {
				return setValue(new SearchVisitor.Matcher() {
					@Override public Optional<edu.kit.joana.setter.misc.Pair<String, ValueToSet.Mode>> matchMethodReturn(String fullyQualifiedClassName, String methodName,
							String desc, List<String> parameterTypes) {
						if (fullyQualifiedClassName.equals(className) && methodName.equals(exMethodName) && parameterTypes.equals(Arrays.asList(args))){
							return Optional.of(new edu.kit.joana.setter.misc.Pair<>(value, mode));
						}
						return Optional.empty();
					}
				});
			}
		} else {
			String[] parts = programPart.split(".");
			String className = String.join("\\.", Arrays.asList(parts).subList(0, parts.length - 1));
			String exFieldName = parts[parts.length - 1];
			return setValue(new SearchVisitor.Matcher() {
				@Override public Optional<edu.kit.joana.setter.misc.Pair<String, ValueToSet.Mode>> matchField(String fullyQualifiedClassName, String fieldName) {
					if (fullyQualifiedClassName.equals(className) && fieldName.equals(exFieldName)){
						return Optional.of(new edu.kit.joana.setter.misc.Pair<>(value, mode));
					}
					return Optional.empty();
				}
			});
		}
	}

	boolean setValue(SearchVisitor.Matcher matcher){
		Tool tool = new Tool(setValueStore, SetValue.class);
		try {
			new PreProcPasses(new FilePass() {
				@Override public void setup(String libClassPath) {

				}

				@Override public void collect(Path file) throws IOException {
					tool.searchAnnotations(file, null, matcher);
				}

				@Override public void store(Path source, Path target) throws IOException {
				}

				@Override public void teardown() {
				}

				@Override public boolean requiresKnowledgeOnAnnotations() {
					return false;
				}
			}).process(null, "", classPath);
		} catch (IOException e) {
			out.error(e.getMessage());
			return false;
		}
		return true;
	}

	public void setInterfaceImplOptions(InterfaceImplementationOptions interfaceImplOptions) {
		this.interfaceImplOptions = interfaceImplOptions;
	}

	/**
   * Wrapper for using the class with {@link ImprovedCLI}
   */
	public class Wrapper
			implements ImprovedCLI.ClassPathEnabled, ImprovedCLI.EntryPointEnabled, ImprovedCLI.SinksAndSourcesEnabled,
			ImprovedCLI.SetValueEnabled, ImprovedCLI.BuildSDGEnabled, ImprovedCLI.RunAnalysisEnabled<AnalysisObject>, ImprovedCLI.ClassSinksEnabled,
			ImprovedCLI.DeclassificationEnabled, ImprovedCLI.OptimizationEnabled, ImprovedCLI.SDGOptionsEnabled, ImprovedCLI.RunEnabled<AnalysisObject>,
			ImprovedCLI.ExportSDGEnabled, ImprovedCLI.SaveSDGEnabled, ImprovedCLI.ViewEnabled, ImprovedCLI.LoadSDGEnabled {

		Map<AnnotationType, Set<IFCAnnotation>> annotationsPerType = new HashMap<>();
		List<SDGClass> sinkClasses = new ArrayList<>();
		private Pair<String, List<String>> cachedProgramParts = Pair.pair("", new ArrayList<>());

		@Override public List<String> getPossibleEntryMethods(String regexp) {
      return loc.justSearch(classPath, out,false, regexp);
    }

    @Override public List<Pair<String, String>> getPossibleEntryPoints() {
      Pattern pat = new Pattern(".*", true, PatternType.ID);
      Optional<List<Pair<IMethod, Annotation>>> result = loc.doSearchForEntryPointAnnotated(classPath, out, pat);
      if (!result.isPresent()) {
        return Collections.emptyList();
      }
      return result.get().stream()
          .map(p -> Pair.pair(getEntryPointIdAttribute(p.getSecond()).orElse(""), p.getFirst().getSignature()))
          .collect(Collectors.toList());
    }

    @Override public boolean setEntryMethod(String method) {
    	loc.doSearch(classPath, out, false, java.util.regex.Pattern.quote(method));
			if (loc.foundPossibleEntries()) {
				loc.selectEntry(0);
				recomputeSDG = true;
				return true;
			} else {
				return false;
			}
    }

    @Override public String setEntryPoint(String tag) {
			if (IFCConsole.this.selectEntryPoint(tag, l -> l.forEach(s -> sinkClasses.add((SDGClass)ImprovedCLI.programPartFromString("L" + s.replace(".", "/") + ";"))))){
				loc.selectEntry(0);
				recomputeSDG = true;
				return loc.getActiveEntry().toBCString();
			} else {
				return null;
			}
    }

		@Override public String getCurrentEntry() {
			return loc.getActiveEntry() == null ? null : loc.getActiveEntry().toBCString();
		}

    @Override public boolean setClassPath(String classPath) {
      return IFCConsole.this.setClassPath(classPath);
    }

    @Override public String getClassPath() {
      return IFCConsole.this.getClassPath();
    }

		@Override public List<Pair<String, String>> getSinkAnnotations(String tag) {
			buildSDGIfNeeded();
			return searchSinksAndSources(new Pattern(tag, false, PatternType.ID), AnnotationType.SINK).stream().map(IFCConsole::annotationsToPartLevelPair).collect(Collectors.toList());
		}

		@Override public List<Pair<String, String>> getSourceAnnotations(String tag) {
			buildSDGIfNeeded();
			return searchSinksAndSources(new Pattern(tag, false, PatternType.ID), AnnotationType.SOURCE).stream().map(IFCConsole::annotationsToPartLevelPair).collect(Collectors.toList());
		}

		@Override public List<Triple<String, String, String>> getDeclassAnnotations(String tag) {
			buildSDGIfNeeded();
			return searchDeclassificationAnnotations(new Pattern(tag, false, PatternType.ID)).stream()
					.map(p -> annotationsToPartLevelTriple(p.getFirst())).collect(Collectors.toList());
		}

		@Override public List<String> getAnnotatableEntities(String regexp) {
			if (!cachedProgramParts.getFirst().equals(getClassPath()) || cachedProgramParts.getSecond().isEmpty()){
				cachedProgramParts = Pair.pair(getClassPath(), searchProgramParts(out.getDebugPrintStream(), getClassPath(), true, false, true, true).stream()
						.map(ImprovedCLI::programPartToString).collect(Collectors.toList()));
			}
			java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regexp);
			return cachedProgramParts.getSecond().stream().filter(s -> s != null && pattern.matcher(s).matches()).collect(Collectors.toList());
		}

		@Override public boolean selectDeclassification(String entity, String fromLevel, String toLevel) {
			if (getAnnotatableEntities(java.util.regex.Pattern.quote(entity)).size() > 0) {
				annotationsPerType.computeIfAbsent(AnnotationType.DECLASS, s -> new HashSet<>())
						.add(new IFCAnnotation(fromLevel, toLevel, ImprovedCLI.programPartFromString(entity)));
				return true;
			}
			return false;
		}

		@Override public List<Triple<String, String, String>> getDeclassifications() {
			return annotationsPerType.getOrDefault(AnnotationType.DECLASS, Collections.emptySet()).stream()
					.map(a -> Triple.triple(ImprovedCLI.programPartToString(a.getProgramPart()), a.getLevel1(), a.getLevel2())).collect(Collectors.toList());
		}

		@Override public boolean removeDeclassifications(List<String> programParts) {
			return annotationsPerType.getOrDefault(AnnotationType.DECLASS, Collections.emptySet())
					.removeIf(p -> programParts.contains(ImprovedCLI.programPartToString(p.getProgramPart())));
		}

		@Override public boolean selectSource(String entity, String level) {
			if (getAnnotatableEntities(java.util.regex.Pattern.quote(entity)).size() > 0) {
				annotationsPerType.computeIfAbsent(AnnotationType.SOURCE, s -> new HashSet<>()).add(
						new IFCAnnotation(AnnotationType.SOURCE, level.isEmpty() ? Level.HIGH : level, ImprovedCLI.programPartFromString(entity)));
				return true;
			}
			return false;
		}

		/** E.g. for auto generated classes */
		public void selectSinkWithoutCheck(String entity, String level){
			annotationsPerType.computeIfAbsent(AnnotationType.SINK, s -> new HashSet<>()).add(
					new IFCAnnotation(AnnotationType.SINK, level.isEmpty() ? Level.LOW : level, ImprovedCLI.programPartFromString(entity)));
		}

		@Override public boolean selectSink(String entity, String level) {
			if (getAnnotatableEntities(java.util.regex.Pattern.quote(entity)).size() > 0) {
				selectSinkWithoutCheck(entity, level);
				return true;
			}
			return false;
		}

		@Override public List<Pair<String, String>> getSources() {
			return annotationsPerType.getOrDefault(AnnotationType.SOURCE, Collections.emptySet()).stream()
					.map(a -> Pair.pair(ImprovedCLI.programPartToString(a.getProgramPart()), a.getLevel1())).collect(Collectors.toList());
		}

		@Override public List<Pair<String, String>> getSinks() {
			return annotationsPerType.getOrDefault(AnnotationType.SINK, Collections.emptySet()).stream()
					.map(a -> Pair.pair(ImprovedCLI.programPartToString(a.getProgramPart()), a.getLevel1())).collect(Collectors.toList());
		}

		@Override public boolean removeSinks(List<String> programParts) {
			return annotationsPerType.getOrDefault(AnnotationType.SINK, Collections.emptySet())
					.removeIf(p -> programParts.contains(ImprovedCLI.programPartToString(p.getProgramPart())));
		}

		@Override public boolean removeSources(List<String> programParts) {
			return annotationsPerType.getOrDefault(AnnotationType.SOURCE, Collections.emptySet())
					.removeIf(p -> programParts.contains(ImprovedCLI.programPartToString(p.getProgramPart())));
		}

		public void setAnnotationsInIFCAnalysis(){
			buildSDGIfNeeded();
			annotationsPerType.values().stream().flatMap(Set::stream).forEach(ifcAnalysis::addAnnotation);
			sinkClasses.forEach(c -> ifcAnalysis.addSinkClass(c));
		}

		@Override public List<Pair<String, String>> getSettableEntities(String regexp) {
			return searchProgramParts(out.getDebugPrintStream(), getClassPath(), true, true, true, false).stream()
					.filter(s -> ImprovedCLI.programPartToString(s) != null)
					.map(p -> p.acceptVisitor(new SDGProgramPartVisitor2<Pair<String, String>, Object>(){
						@Override protected Pair<String, String> visitMethod(SDGMethod m, Object data) {
							return Pair.pair(ImprovedCLI.programPartToString(m), m.getSignature().getReturnType().toHRString());
						}

						@Override protected Pair<String, String> visitAttribute(SDGAttribute a, Object data) {
							return Pair.pair(ImprovedCLI.programPartToString(a), a.getType());
						}

						@Override protected Pair<String, String> visitParameter(SDGFormalParameter p, Object data) {
							return Pair.pair(ImprovedCLI.programPartToString(p), p.getType().toHRString());
						}
					}, null))
					.filter(p -> p.getFirst() != null && p.getFirst().matches(regexp)).collect(Collectors.toList());
		}

		@Override public List<Pair<String, ValueToSet>> getSetValueAnnotations(String tag) {
			return IFCConsole.this.searchSetValues(tag).getAnnotations().stream().map(p -> Pair.pair(p.first, p.second)).collect(Collectors.toList());
		}

		@Override public boolean setValueOfEntity(String entity, String value) {
    	valuesToSet.put(ImprovedCLI.programPartFromString(entity), Pair.pair(value, ValueToSet.Mode.VALUE));
    	recomputeSDG = true;
    	return true;
		}

		@Override public boolean setRefValueOfEntity(String entity, String ref) {
			valuesToSet.put(ImprovedCLI.programPartFromString(entity), Pair.pair(ref, ValueToSet.Mode.getFieldMode(ref)));
    	recomputeSDG = true;
			return true;
		}

		@Override public List<Pair<String, ValueToSet>> selectSetValueAnnotations(String tag) {
			List<Pair<String, ValueToSet>> setValueAnnotations = getSetValueAnnotations(tag);
			setValueAnnotations.forEach(p -> valuesToSet.put(
					ImprovedCLI.programPartFromString(p.getFirst()), Pair.pair(p.getSecond().value, p.getSecond().mode)));
			return setValueAnnotations;
		}

		@Override public List<Pair<String, Pair<String, ValueToSet.Mode>>> getSetValues() {
			return valuesToSet.entrySet().stream().map(e -> Pair.pair(ImprovedCLI.programPartToString(e.getKey()), e.getValue())).filter(p -> p.getFirst() != null).collect(Collectors.toList());
		}

		@Override public boolean removeSetValues(List<String> programParts) {
			programParts.stream().map(ImprovedCLI::programPartFromString).forEach(valuesToSet::remove);
			return true;
		}

		@Override public boolean needsRebuild() {
			return recomputeSDG;
		}

		@Override public boolean buildSDG() {
			return IFCConsole.this.buildSDG();
		}

		@Override public AnalysisObject getMixin(ImprovedCLI.AnalyzeCommand command) {
			return new AnalysisObject();
		}

		@Override public boolean run(AnalysisObject state) {
			setAnnotationsInIFCAnalysis();
			if (getSources().isEmpty()){
				System.err.println("No sources selected");
				return false;
			}
			if (getSinks().isEmpty()){
				System.err.println("No sinks selected");
				return false;
			}
			return runAnalysisYAML(state.out);
		}

		@Override public boolean addSinkClass(String klass) {
			sinkClasses.add((SDGClass)ImprovedCLI.programPartFromString(klass));
			return searchClasses(java.util.regex.Pattern.quote(klass)).size() > 0;
		}

		@Override public List<String> searchClasses(String regexp) {
			try {
				return SDGBuildPreparation.searchClasses(out.getDebugPrintStream(), getClassPath()).stream()
						.map(ImprovedCLI::programPartToString).filter(c -> c.matches(regexp)).collect(Collectors.toList());
			} catch (IOException | ClassHierarchyException e) {
				e.printStackTrace();
			}
			return Collections.emptyList();
		}

		@Override public List<String> getSinkClasses() {
			return sinkClasses.stream().map(ImprovedCLI::programPartToString).collect(Collectors.toList());
		}

		@Override public boolean removeSinkClasses(List<String> sinkClasses) {
			sinkClasses.stream().map(ImprovedCLI::programPartFromString).forEach(this.sinkClasses::remove);
			return true;
		}

		@Override public void enableOptimizations(String libPath) {
			optLibPath = libPath;
			useByteCodeOptimizations = true;
		}

		@Override public void disableOptimizations() {
			optLibPath = null;
			useByteCodeOptimizations = false;
		}

		@Override public Optional<String> getLibPath() {
			return Optional.ofNullable(optLibPath);
		}

		@Override public void setComputeInterference(boolean enable) {
			computeInterference = enable;
		}

		@Override public void setMHPType(MHPType mhpType) {
			IFCConsole.this.mhpType = mhpType;
		}

		@Override public void setExcAnalysis(ExceptionAnalysis excAnalysis) {
			IFCConsole.this.excAnalysis = excAnalysis;
		}

		@Override public boolean isInterferenceComputed() {
			return computeInterference;
		}

		@Override public MHPType getMhpType() {
			return mhpType;
		}

		@Override public ExceptionAnalysis getExcAnalysis() {
			return excAnalysis;
		}

		@Override public void setPruningPolicy(PruningPolicy pruningPolicy) {
			IFCConsole.this.pruningPolicy = pruningPolicy;
		}

		@Override public PruningPolicy getPruningPolicy() {
			return pruningPolicy;
		}

		@Override public void setOnlyDirectFlow(boolean only) {
			onlyDirectFlow = only;
		}

		@Override public boolean usesOnlyDirectFlow() {
			return onlyDirectFlow;
		}

		@Override public void setUninitializedFieldTypeMatcher(UninitializedFieldHelperOptions.FieldTypeMatcher fieldTypeMatcher) {
			IFCConsole.this.setUninitializedFieldTypeMatcher(fieldTypeMatcher);
		}

		@Override public UninitializedFieldHelperOptions.FieldTypeMatcher getUninitializedFieldTypeMatcher() {
			return uninitializedFieldTypeMatcher;
		}

		@Override public AnalysisObject getMixin(ImprovedCLI.RunCommand command) {
			return new AnalysisObject();
		}

		@Override public boolean exportSDG(File file) {
			return exportGraphML(file.toString());
		}

		@Override public boolean saveSDG(File file) {
			return IFCConsole.this.saveSDG(file.toString());
		}

		@Override public SDG getSDG() {
			return IFCConsole.this.getSDG();
		}

		@Override public boolean loadSDG(Path file) {
			return IFCConsole.this.loadSDG(file.toString(), IFCConsole.this.getMHPType());
		}
	}

	public class AnalysisObject {
		@CommandLine.Option(names = "--out", description = "Output file or '-' for standard out")
		String out = "-";
	}

	/**
	 * Only works for Source and Sink annotations
	 */
	static Pair<String, String> annotationsToPartLevelPair(IFCAnnotation ann){
		return Pair.pair(ImprovedCLI.programPartToString(ann.getProgramPart()), ann.getLevel1());
	}

	/**
	 * Only works for Declassification annotations
	 */
	static Triple<String, String, String> annotationsToPartLevelTriple(IFCAnnotation ann){
		return Triple.triple(ImprovedCLI.programPartToString(ann.getProgramPart()), ann.getLevel1(), ann.getLevel2());
	}

	public Wrapper createWrapper(){
	  return new Wrapper();
  }

  public IFCAnalysis getAnalysis(){
		return ifcAnalysis;
	}

	public void setAdditionalEntryMethods(Collection<String> additionalEntryMethods) {
		this.additionalEntryMethods = additionalEntryMethods;
	}


	public boolean isAnnotatingOverloadedMethods() {
		return annotateOverloadedMethods;
	}

	public void setAnnotateOverloadedMethods(boolean annotateOverloadedMethods) {
		this.annotateOverloadedMethods = annotateOverloadedMethods;
	}
}
