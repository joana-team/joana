package edu.kit.joana.ui.ifc.wala.console.console;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.UninitializedFieldHelperOptions;
import com.ibm.wala.types.TypeReference;
import edu.kit.joana.api.sdg.*;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.setter.ValueToSet;
import edu.kit.joana.ui.annotations.PruningPolicy;
import edu.kit.joana.ui.ifc.sdg.graphviewer.GraphViewer;
import edu.kit.joana.ui.ifc.sdg.graphviewer.util.SDGUtils;
import edu.kit.joana.util.Pair;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.util.Triple;
import edu.kit.joana.wala.core.SDGBuilder;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.fusesource.jansi.Ansi;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.reader.impl.LineReaderImpl;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliJLineCompleter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static picocli.CommandLine.*;

/**
 * Building a better CLI using picocli and JLine.
 *
 * The main idea is to split the core functionality into basic interfaces.
 */
public class ImprovedCLI {

  static Logger logger = Logger.getLogger("cli");

  /**
   * Supports classes, fields, methods, parameters
   *
   * @return null if not supported, classes: BCString, attributes: [class]#[attribute], methods: [class].[method signature],
   * method exits: [method…]->exit, parameters: [method…]->[parameter index]
   */
  public static String programPartToString(SDGProgramPart part){
    try {
      return part.acceptVisitor(new SDGProgramPartVisitor2<String, Object>() {
        @Override protected String visitClass(SDGClass cl, Object data) {
          return cl.getTypeName().toBCString();
        }

        @Override protected String visitAttribute(SDGAttribute a, Object data) {
          return visitClass(a.getOwningClass(), null) + "#" + a.getName();
        }

        @Override protected String visitMethod(SDGMethod m, Object data) {
          return m.getSignature().toBCString();
        }

        @Override protected String visitParameter(SDGFormalParameter p, Object data) {
          return visitMethod(p.getOwningMethod(), null) + "->" + p.getIndex();
        }

        @Override protected String visitExit(SDGMethodExitNode e, Object data) {
          return visitMethod(e.getOwningMethod(), null) + "->exit";
        }
      }, null);
    } catch (NullPointerException ex) {
      System.err.println(String.format("Ignoring %s, internal error", part));
      return null;
    }
  }

  /**
   * Supports classes, fields, methods, parameters, is a rough translation
   */
  public static SDGProgramPart programPartFromString(String str) {
    if (str.matches(".+#.+")) { // field
      String[] parts = str.split("#");
      return new SDGAttribute((SDGClass) programPartFromString(parts[0]), parts[1],
          JavaType.parseSingleTypeFromString("java.lang.Object"));
    }
    if (str.matches(".+\\..+")) {
      if (str.matches(".+->-?[0-9]+")) {
        String[] parts = str.split("->");
        return new SDGFormalParameter((SDGMethod) programPartFromString(parts[0]), Integer.parseInt(parts[1]), parts[1],
            JavaType.parseSingleTypeFromString("java.lang.Object"));
      }
      if (str.matches(".+->exit")) {
        String[] parts = str.split("->");
        return ((SDGMethod) programPartFromString(parts[0])).getExit();
      }
      return new SDGMethod(JavaMethodSignature.fromString(str), AnalysisScope.APPLICATION.toString(), false);
    }
    return new SDGClass(JavaType.parseSingleTypeFromString(str), Collections.emptyList(), Collections.emptyMap(),
        Collections.emptySet(), new SDG(), new TIntObjectHashMap<>());
  }

  @Retention(RetentionPolicy.RUNTIME) @Target({ ElementType.FIELD, ElementType.PARAMETER }) @interface State {
  }

  @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface DynamicMixin {
  }

  static class HelpOptions {
    @Option(names = { "-h", "--help" }, usageHelp = true, description = "Display this help and exit") boolean help;
  }

  /**
   * Basic functionality like setting the class path or enabling verbose output
   */
  public interface ClassPathEnabled {
    boolean setClassPath(String classPath);

    String getClassPath();

  }

  @Command(name = "classPath",
      description = "Get or set the current class path")
  static class ClassPathCommand implements Callable<Integer> {

    @ParentCommand CliCommands parent;

    @State ClassPathEnabled state;

    @Parameters(paramLabel = "classPath", arity = "0..1", description = "Class path to use")
    String classPath;

    @Override public Integer call() {
      if (classPath != null) {
        return exit(state.setClassPath(classPath));
      } else {
        parent.out.println(state.getClassPath());
        return 0;
      }
    }
  }

  public interface BuildSDGEnabled {

    boolean needsRebuild();

    boolean buildSDG();
  }

  @Command(name = "buildSDG", description = "Build the SDG if needed")
  static class BuildSDGCommand implements Callable<Integer> {

    @ParentCommand CliCommands parent;

    @State
    BuildSDGEnabled state;

    @Option(names = {"--force", "-f"}, description = "Build the SDG always")
    boolean force;

    @Override public Integer call() {
      if (state.needsRebuild() || force) {
        return exit(state.buildSDG());
      }
      return 0;
    }
  }

  public interface LoadSDGEnabled {

    boolean loadSDG(Path file);
  }

  @Command(name = "loadSDG", description = "Load the SDG from a file")
  static class LoadSDGCommand implements Callable<Integer> {

    @ParentCommand CliCommands parent;

    @State
    LoadSDGEnabled state;

    @Parameters(description = "SDG file", paramLabel = "FILE")
    Path file;

    @Override public Integer call() {
      return exit(state.loadSDG(file));
    }
  }

  public interface ExportSDGEnabled {

    boolean exportSDG(File file);
  }

  @Command(name = "exportSDG", description = "Export the SDG in GraphML format")
  static class ExportSDGCommand implements Callable<Integer> {

    @ParentCommand CliCommands parent;

    @State
    ExportSDGEnabled state;

    @Parameters
    File file;

    @Override public Integer call() {
      return exit(state.exportSDG(file));
    }
  }

  public interface SaveSDGEnabled {

    boolean saveSDG(File file);
  }

  @Command(name = "saveSDG", description = "Save the SDG")
  static class SaveSDGCommand implements Callable<Integer> {

    @ParentCommand CliCommands parent;

    @State
    SaveSDGEnabled state;

    @Parameters
    File file;

    @Override public Integer call() {
      return exit(state.saveSDG(file));
    }
  }


  interface SDGOptionsEnabled {

    void setComputeInterference(boolean enable);

    void setMHPType(MHPType mhpType);

    void setExcAnalysis(SDGBuilder.ExceptionAnalysis excAnalysis);

    boolean isInterferenceComputed();

    MHPType getMhpType();

    SDGBuilder.ExceptionAnalysis getExcAnalysis();

    void setPruningPolicy(PruningPolicy pruningPolicy);

    PruningPolicy getPruningPolicy();

    void setOnlyDirectFlow(boolean only);

    boolean usesOnlyDirectFlow();

    void setUninitializedFieldTypeMatcher(UninitializedFieldHelperOptions.FieldTypeMatcher fieldTypeMatcher);

    void setStubs(Stubs stubs);

    Stubs getStubs();

    void setExceptionalistEnabled(boolean enabled);

    boolean isExceptionalistEnabled();

    UninitializedFieldHelperOptions.FieldTypeMatcher getUninitializedFieldTypeMatcher();
  }

  public static final class BooleanCompletionCandidates implements Iterable<String> {

    @Override public Iterator<String> iterator() {
      return Arrays.asList("true", "false").iterator();
    }
  }

  @Command(name = "sdgOptions", description = "Options for the SDG creation",
      subcommands = { SDGOptionsCommand.InfoCommand.class, SDGOptionsCommand.EnabledUninitializedFieldTypesCommand.class })
  static class SDGOptionsCommand implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand CliCommands parent;

    SDGOptionsEnabled state;

    SDGOptionsCommand(@State SDGOptionsEnabled state){
      this.state = state;
    }

    @Command
    void computeInterference(@Parameters(paramLabel = "enable", completionCandidates = BooleanCompletionCandidates.class)
        boolean enable){
      state.setComputeInterference(enable);
    }

    @Command
    void mhpType(@Parameters(paramLabel = "mhpType") MHPType mhpType){
      state.setMHPType(mhpType);
    }

    @Command
    void excAnalysis(@Parameters(paramLabel = "excAnalysis") SDGBuilder.ExceptionAnalysis excAnalysis){
      this.state.setExcAnalysis(excAnalysis);
    }

    @Command
    void pruningPolicy(@Parameters(paramLabel = "pruningPolicy") PruningPolicy pruningPolicy){
      this.state.setPruningPolicy(pruningPolicy);
    }

    @Command
    void uninitializedFieldTypeRegexp(@Parameters(paramLabel = "regexp") String regexp,
        @Option(names = "owner", defaultValue = "") String owner, @Option(names = "field", defaultValue = "") String field){
      this.state.setUninitializedFieldTypeMatcher(new UninitializedFieldHelperOptions.FieldTypeMatcher() {
        @Override public boolean matchType(TypeReference typeReference) {
          return typeReference.getName().toString().matches(regexp);
        }

        @Override public boolean matchOwnerType(TypeReference typeReference) {
          return owner.isEmpty() ? matchType(typeReference) : typeReference.getName().toString().matches(owner);
        }

        @Override public boolean matchFieldType(TypeReference typeReference) {
          return field.isEmpty() ? matchType(typeReference) : typeReference.getName().toString().matches(field);
        }
      });
    }

    @Command(name = "enableUninitializedFieldTypes", description = "Equivalent to 'uninitializedFieldTypeRegexp '.*'")
    static class EnabledUninitializedFieldTypesCommand implements Runnable {
      @ParentCommand
      SDGOptionsCommand parent;
      @Override public void run() {
        parent.uninitializedFieldTypeRegexp(".*", ".*", ".*");
      }
    }

    @Command
    void onlyDirectFlow(@Parameters(paramLabel = "only", completionCandidates = BooleanCompletionCandidates.class)
        boolean only){
      this.state.setOnlyDirectFlow(only);
    }

    @Command(name = "info")
    static class InfoCommand implements Runnable {
      @ParentCommand
      SDGOptionsCommand parent;
      @Override public void run() {
        for (Pair<String, ?> p : Arrays.asList(
                Pair.pair("computeInterference", parent.state.isInterferenceComputed()),
                Pair.pair("mhpType", parent.state.getMhpType()),
                Pair.pair("excAnalysis", parent.state.getExcAnalysis()),
                Pair.pair("pruningPolicy", parent.state.getPruningPolicy()),
                Pair.pair("onlyDirectFlow", parent.state.usesOnlyDirectFlow()),
                Pair.pair("uninitializedFieldTypeRegexp", parent.state.getUninitializedFieldTypeMatcher()),
                Pair.pair("stubs", parent.state.getStubs()),
                Pair.pair("exceptionalistEnabled", parent.state.isExceptionalistEnabled()))) {
          System.out.println(String.format("%-20s = %s", p.getFirst(), p.getSecond().toString()));
        }
      }
    }

    @Command
    void stubs(@Parameters(paramLabel = "stubs") Stubs stubs) {
      this.state.setStubs(stubs);
    }

    @Command
    void setExceptionalistEnabled(@Parameters(paramLabel = "enabled", completionCandidates = BooleanCompletionCandidates.class)
        boolean enabled) {
      this.state.setExceptionalistEnabled(enabled);
    }


    @Override public Integer call() {
      return parent.printUsage(spec);
    }
  }

  interface OpenAPIEnabled {
    boolean isOpenAPIEnabled();
    void enableOpenAPI();
    void disableOpenAPI();
  }

  @Command(name = "openapi", description = "Enable and disable byte code optimizations",
      subcommands = { OpenAPICommand.DisableCommand.class, OpenAPICommand.InfoCommand.class })
  static class OpenAPICommand implements Callable<Integer> {
    @Spec Model.CommandSpec spec;

    @ParentCommand CliCommands parent;

    @State
    OpenAPIEnabled state;

    @Command
    void enable(){
      state.enableOpenAPI();
    }

    @Command(name = "disable")
    static class DisableCommand implements Runnable {
      @ParentCommand
      OpenAPICommand parent;
      @Override public void run() {
        parent.state.disableOpenAPI();
      }
    }

    @Command(name = "info")
    static class InfoCommand implements Runnable {
      @ParentCommand
      OpenAPICommand parent;
      @Override public void run() {
        if (parent.state.isOpenAPIEnabled()){
          System.out.println("enabled");
        } else {
          System.out.println("disabled");
        }
      }
    }

    @Override public Integer call() {
      return parent.printUsage(spec);
    }
  }

  interface OptimizationEnabled {

    void enableOptimizations(String libPath);

    void disableOptimizations();

    Optional<String> getLibPath();
  }

  @Command(name = "optimization", description = "Enable and disable byte code optimizations",
      subcommands = { OptimizeEnableCommand.DisableCommand.class, OptimizeEnableCommand.InfoCommand.class })
  static class OptimizeEnableCommand implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand CliCommands parent;

    @State
    OptimizationEnabled state;

    @Command
    void enable(@Parameters(paramLabel = "libPath", defaultValue = "") String libPath){
      state.enableOptimizations(libPath);
    }

    @Command(name = "disable")
    static class DisableCommand implements Runnable {
      @ParentCommand
      OptimizeEnableCommand parent;
      @Override public void run() {
        parent.state.disableOptimizations();
      }
    }

    @Command(name = "info")
    static class InfoCommand implements Runnable {
      @ParentCommand
      OptimizeEnableCommand parent;
      @Override public void run() {
        if (parent.state.getLibPath().isPresent()){
          System.out.println("enabled with library path: " + parent.state.getLibPath().get());
        } else {
          System.out.println("disabled");
        }
      }
    }

    @Override public Integer call() {
      return parent.printUsage(spec);
    }
  }

  public interface SetValueEnabled {
    /**
     * @param regexp matches the entity
     * @return [(entity, type)]
     */
    List<Pair<String, String>> getSettableEntities(String regexp);
    /**
     * @return [(entity, value to set)]
     */
    List<Pair<String, ValueToSet>> getSetValueAnnotations(String tag);
    boolean setValueOfEntity(String entity, String value);
    boolean setRefValueOfEntity(String entity, String ref);
    /**
     * @return [(entity, value to set)]
     */
    List<Pair<String, ValueToSet>> selectSetValueAnnotations(String tag);
    /**
     * @return [(entity, (value to set, field|value))]
     */
    List<Pair<String, Pair<String, ValueToSet.Mode>>> getSetValues();
    boolean removeSetValues(List<String> programParts);
  }

  static final String ENTITY_SYNTAX =
      "syntax: classes: @|italic byte code class string|@, attributes: @|italic [class]#[attribute]|@, "
          + "methods: @|italic [class].[method name]([parameter type descriptor])[return type descriptor]|@, "
          + "parameters: @|italic [method…]->[parameter index]|@";

  static final String METHOD_SYNTAX = "syntax: @|italic [byte code class string].[method name]([parameter type descriptor])[return type descriptor]|@";

  static final String ENTITY_REGEXP = "Regular expression that is used to filter the entities based on their representation, " + ENTITY_SYNTAX;

  @Command(name = "setValue",
      description = "Work with @|italic @SetValue |@ annotations and set the value of entities. An entity is either a method"
          + " (which return value can be set), a method parameter or a field",
      subcommands = {SetValueCommands.ListCommand.class, SetValueCommands.ListAnnsCommand.class})
  static class SetValueCommands implements Callable<Integer> {

    @Spec Model.CommandSpec spec;
    @ParentCommand CliCommands parent;

    @State
    SetValueEnabled state;


    @Command(name = "value", description = "Set the primitive or string value of a parameter, a method (its return value) or a field") int value(
        @Parameters(paramLabel = "entity", description = "Entity to set, " + ENTITY_SYNTAX) String entity,
        @Parameters(paramLabel = "value", description = "Value of the entity") String value) {
      return exit(state.setValueOfEntity(entity, value));
    }

    @Command(name = "field", description = { "Use the value of a field of a class.",
        "Accessing non static fields is only supported non static fields and methods, "
            + "the field syntax is just the field name.",
        "Accessing static fields is supported via @|italic pkg1/pkg2/…/className[$innerClass]#fieldName|@ "
            + "static fields of the own class can be accessed via @|italic .#fieldName|@" }) int field(
        @Parameters(paramLabel = "entity", description = "Entity to set, " + ENTITY_SYNTAX) String entity,
        @Parameters(paramLabel = "ref", description = "Reference to a field which's value is used") String ref) {
      return exit(state.setRefValueOfEntity(entity, ref));
    }

    @Command(name = "tag", description = "Select the @|italic @SetValue|@ annotation with the given tag") int tag(
        @Parameters(paramLabel = "tag", description = "An empty tag matches all annotations without a tag", arity = "1") String tag) {
      List<Pair<String, ValueToSet>> pairs = state.selectSetValueAnnotations(tag);
      if (pairs.isEmpty()) {
        parent.log("No annotations found");
        return ExitCode.SOFTWARE;
      }
      for (Pair<String, ValueToSet> pair : pairs) {
        parent.log(String.format("%s: %s", pair.getFirst(), pair.getSecond()));
      }
      return 0;
    }

    @Command(name = "listEntities", description = "List the entities that be set with their types") int listEntities(
        @Parameters(paramLabel = "regexp", defaultValue = ".*", description = ENTITY_REGEXP) String regexp) {
      for (Pair<String, String> settableEntity : state.getSettableEntities(regexp)) {
        parent.out.println(settableEntity.getFirst() + ": " + settableEntity.getSecond());
      }
      return 0;
    }

    @Command(name = "listAnnotated", description = "List all @|italic @SetValue|@ annotations that contain a specific tag") static class ListAnnsCommand
        implements Runnable {
      @ParentCommand SetValueCommands parent;

      @Parameters(description = "Tag that has to be included in the @|italic tags |@ attribute in the annotation of each entity") String tag;

      @Override public void run() {
        for (Pair<String, ValueToSet> settableEntity : parent.state.getSetValueAnnotations(tag)) {
          parent.parent.out.println(settableEntity.getFirst() + ": " + settableEntity.getSecond());
        }
      }
    }

    @Command(name = "current", description = "List all currently set values with their value") static class ListCommand
        implements Runnable {
      @ParentCommand SetValueCommands parent;

      @Override public void run() {
        for (Pair<String, Pair<String, ValueToSet.Mode>> pair : parent.state.getSetValues()) {
          parent.parent.out.println(
              pair.getFirst() + ": " + pair.getSecond().getFirst() + " " + pair.getSecond().getSecond().toString().toLowerCase());
        }
      }
    }

    @Command(description = "Remove the set values for the matching entities") boolean remove(
        @Parameters(paramLabel = "regexp", defaultValue = ".*", description = ENTITY_REGEXP) String regexp) {
      List<Pair<String, Pair<String, ValueToSet.Mode>>> removed = this.state.getSetValues().stream()
          .filter(p -> p.getFirst().matches(regexp)).collect(Collectors.toList());
      for (Pair<String, Pair<String, ValueToSet.Mode>> pair : removed) {
        parent.out.println("Removed " + pair.getFirst() + ": " + pair.getSecond().getFirst() + " " + pair.getSecond().getSecond());
      }
      return state.removeSetValues(removed.stream().map(Pair::getFirst).collect(Collectors.toList()));
    }

    @Override public Integer call() {
      return parent.printUsage(spec);
    }
  }

  public interface EntryPointEnabled {
    List<String> getPossibleEntryMethods(String regexp);

    /**
     * @return (tag, method)
     */
    List<Pair<String, String>> getPossibleEntryPoints();

    boolean setEntryMethod(String method);

    String setEntryPoint(String tag);

    String getCurrentEntry();
  }

  @Command(name = "entry", description = "Select the entry point", subcommands = { EntryPointCommand.ListEntryPoints.class,
      EntryPointCommand.CurrentEntryPoint.class }) static class EntryPointCommand implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand CliCommands parent;

    @State EntryPointEnabled state;

    @Command(description = "List the possible entry methods") void list(
        @Parameters(paramLabel = "regexp", defaultValue = ".*", description =
            "Regular expression for filtering, has to match the string representation of a method, "
                + METHOD_SYNTAX) String regexp) {
      for (String possibleEntryMethod : state.getPossibleEntryMethods(regexp)) {
        parent.out.println(possibleEntryMethod);
      }
    }

    @Command(name = "listAnnotated", description = "List all annotated entry points with their tag") static class ListEntryPoints
        implements Runnable {
      @ParentCommand EntryPointCommand parent;

      @Override public void run() {
        for (Pair<String, String> possibleEntryPoint : parent.state.getPossibleEntryPoints()) {
          parent.parent.out.println(possibleEntryPoint.getFirst() + ": " + possibleEntryPoint.getSecond());
        }
      }
    }

    @Command(name = "current", description = "Print the currently used entry method") static class CurrentEntryPoint
        implements Callable<Integer> {
      @ParentCommand EntryPointCommand parent;

      @Override public Integer call() throws Exception {
        String currentEntry = parent.state.getCurrentEntry();
        if (currentEntry != null) {
          System.out.println(currentEntry);
          return 0;
        } else {
          return 1;
        }
      }
    }

    @Command(description = "Select an entry point") int select(
        @Parameters(paramLabel = "method or tag", description = "Either a method (" + METHOD_SYNTAX + ") or the tag "
            + "that a single entry point has in its annotation") String methodOrTag) {
      if (state.setEntryPoint(methodOrTag) == null) {
        return exit(state.setEntryMethod(methodOrTag));
      }
      return 0;
    }

    @Override public Integer call() {
      return parent.printUsage(spec);
    }
  }

  public interface SinksAndSourcesEnabled {
    /**
     * @return [(entity, level)]
     */
    List<Pair<String, String>> getSinkAnnotations(String tag);

    /**
     * @return [(entity, level)]
     */
    List<Pair<String, String>> getSourceAnnotations(String tag);

    List<String> getAnnotatableEntities(String regexp);

    /**
     * @return [(entity, level)]
     */
    default List<Pair<String, String>> selectSourceAnnotations(String tag) {
      List<Pair<String, String>> anns = getSourceAnnotations(tag);
      anns.forEach(a -> selectSource(a.getFirst(), a.getSecond()));
      return anns;
    }

    /**
     * @return [(entity, level)]
     */
    default List<Pair<String, String>> selectSinkAnnotations(String tag) {
      List<Pair<String, String>> anns = getSinkAnnotations(tag);
      anns.forEach(a -> selectSink(a.getFirst(), a.getSecond()));
      return anns;
    }

    boolean selectSource(String entity, String level);

    boolean selectSink(String entity, String level);

    /**
     * @return [(entity, level)]
     */
    List<Pair<String, String>> getSources();

    /**
     * @return [(entity, level)]
     */
    List<Pair<String, String>> getSinks();

    boolean removeSinks(List<String> programParts);

    boolean removeSources(List<String> programParts);

    default boolean resetSinks() {
      return removeSinks(getSinks().stream().map(Pair::getFirst).collect(Collectors.toList()));
    }

    default boolean resetSources() {
      return removeSinks(getSources().stream().map(Pair::getFirst).collect(Collectors.toList()));
    }
  }

  static interface ClassSinksEnabled {
    boolean addSinkClass(String klass);

    List<String> searchClasses(String regexp);

    List<String> getSinkClasses();

    boolean removeSinkClasses(List<String> sinkClasses);
  }

  @Command(name = "classSinks", description = "Use whole classes as sinks") static class AddClassSinksCommand
      implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand CliCommands parent;

    @State ClassSinksEnabled state;

    @Command(name = "select", description = "Use parameters (except @|italic this|@) of all methods that belong to the "
        + "specified class or sub classes as sinks") boolean select(
        @Parameters(paramLabel = "class", description = "Byte code class name") String klass) {
      return state.addSinkClass(klass);
    }

    @Command(description = "List possible classes")
    void possibleClasses(@Parameters(paramLabel = "regexp", description = "Regular expression for filtering the classes using their byte code names",
        defaultValue = ".*")
        String regexp){
      parent.printList(state.searchClasses(regexp));
    }

    @Command(name = "current", description = "List the current sink classes")
    static class CurrentCommand implements Runnable {
      @ParentCommand SinksCommand parent;

      @Override public void run() {
        for (Pair<String, String> pair : parent.get()) {
          parent.parent.out.println(pair.getFirst() + ": " + pair.getSecond());
        }
      }
    }

    @Override
    public Integer call() {
      return parent.printUsage(spec);
    }

    @Command(description = "Remove sinks classes")
    boolean remove(@Parameters(paramLabel = "regexp", description = "Regular expression matching the classes that should be removed")
        String regexp){
      List<String> removed = this.state.getSinkClasses().stream().filter(c -> c.matches(regexp))
          .collect(Collectors.toList());
      for (String klass : removed) {
        parent.out.println("Removed " + klass);
      }
      return state.removeSinkClasses(removed);
    }
  }

  @Command(name = "sinks", description = "Annotate sinks", subcommands = {SinksCommand.CurrentCommand.class,
      SinksCommand.ResetCommand.class })
  public static class SinksCommand implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand
    CliCommands parent;

    SinksAndSourcesEnabled state;

    SinksCommand(@State SinksAndSourcesEnabled state){
      this.state = state;
    }

    List<Pair<String, String>> getAnnotations(String tag){
      return state.getSinkAnnotations(tag);
    }

    List<Pair<String, String>> selectAnnotations(String tag){
      return state.selectSinkAnnotations(tag);
    }

    boolean select(String entity, String level){
      return state.selectSink(entity, level);
    }

    boolean remove(List<String> programParts){
      return state.removeSinks(programParts);
    }

    List<Pair<String, String>> get(){
      return state.getSinks();
    }

    @Command(description = "List all annotations that contain a specific tag")
    void listAnnotations(@Parameters(paramLabel="tag",
        description = "Tag that has to be included in the @|italic tags|@ attribute in the annotation of each entity")
        String tag){
      for (Pair<String, String> annotation : getAnnotations(tag)) {
        parent.out.println(annotation.getFirst() + ": " + annotation.getSecond());
      }
    }

    @Command(description = "List all possible entities to use")
    void possibleEntities(@Parameters(paramLabel = "regexp", defaultValue = ".*", description = ENTITY_REGEXP) String regexp){
      parent.printList(state.getAnnotatableEntities(regexp));
    }

    @Command(name = "select", description = "Select ${PARENT-COMMAND-NAME}")
    int selectEOrT(@Parameters(paramLabel = "tag or entity", description = "Tag or entity (" + ENTITY_SYNTAX + ")")
        String tagOrEntity, @Parameters(paramLabel = "level", defaultValue = "", description = "Security level") String level){
      if (!select(tagOrEntity, level)){
        List<Pair<String, String>> strings = selectAnnotations(tagOrEntity);
        if (strings.isEmpty()){
          parent.log("No annotation found");
          return ExitCode.SOFTWARE;
        }
        for (Pair<String, String> string : strings) {
          parent.log(string.getFirst() + ": " + string.getSecond());
        }
      }
      return 0;
    }

    @Command(name = "current", description = "List the current ${PARENT-COMMAND-NAME} with their level") static class CurrentCommand
        implements Runnable {
      @ParentCommand SinksCommand parent;

      @Override public void run() {
        for (Pair<String, String> pair : parent.get()) {
          parent.parent.out.println(pair.getFirst() + ": " + pair.getSecond());
        }
      }
    }

    @Override public Integer call() {
      return parent.printUsage(spec);
    }

    @Command(description = "Remove the ${PARENT-COMMAND-NAME} that belong to the selected entities") boolean remove(
        @Parameters(paramLabel = "regexp", description = ENTITY_REGEXP) String regexp) {
      List<Pair<String, String>> removed = this.get().stream().filter(p -> p.getFirst().matches(regexp))
          .collect(Collectors.toList());
      for (Pair<String, String> pair : removed) {
        parent.out.println("Removed " + pair.getFirst());
      }
      return remove(removed.stream().map(Pair::getFirst).collect(Collectors.toList()));
    }

    @Command(name = "reset", description = "Remove all current ${PARENT-COMMAND-NAME}") static class ResetCommand
        implements Callable<Integer> {
      @ParentCommand SinksCommand parent;

      @Override public Integer call() {
        for (Pair<String, String> pair : parent.get()) {
          parent.parent.out.println("Removed " + pair.getFirst());
        }
        return exit(parent.remove(parent.get().stream().map(Pair::getFirst).collect(Collectors.toList())));
      }
    }
  }

  @Command(name = "sources", description = "Annotate sources")
  static class SourcesCommand extends SinksCommand implements Callable<Integer> {

    SourcesCommand(@State SinksAndSourcesEnabled state) {
      super(state);
    }

    @Override List<Pair<String, String>> getAnnotations(String tag){
      return state.getSourceAnnotations(tag);
    }

    @Override List<Pair<String, String>> selectAnnotations(String tag){
      return state.selectSourceAnnotations(tag);
    }

    @Override boolean select(String entity, String level){
      return state.selectSource(entity, level);
    }

    @Override List<Pair<String, String>> get(){
      return state.getSources();
    }

    boolean remove(List<String> programParts){
      return state.removeSources(programParts);
    }
  }

  public interface DeclassificationEnabled {
    /**
     * @return [(entity, level1, level2)]
     */
    List<Triple<String, String, String>> getDeclassAnnotations(String tag);
    List<String> getAnnotatableEntities(String regexp);
    /**
     * @return [(entity, level)]
     */
    default List<Triple<String, String, String>> selectDeclassificationAnnotations(String tag){
      List<Triple<String, String, String>> anns = getDeclassAnnotations(tag);
      anns.forEach(a -> selectDeclassification(a.getLeft(), a.getMiddle(), a.getRight()));
      return anns;
    }

    boolean selectDeclassification(String entity, String fromLevel, String toLevel);
    /**
     * @return [(entity, level1, level2)]
     */
    List<Triple<String, String, String>> getDeclassifications();

    boolean removeDeclassifications(List<String> programParts);
  }

  @Command(name = "declass", description = "Annotate declassificators", subcommands = {
      DeclassificationsCommand.CurrentCommand.class })
  static class DeclassificationsCommand implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand
    CliCommands parent;

    DeclassificationEnabled state;

    DeclassificationsCommand(@State DeclassificationEnabled state){
      this.state = state;
    }

    @Command(description = "List all annotations that contain a specific tag")
    void listAnnotations(@Parameters(paramLabel="tag",
        description = "Tag that has to be included in the @|italic tags|@ attribute in the annotation of each entity")
        String tag){
      for (Triple<String, String, String> annotation : state.getDeclassAnnotations(tag)) {
        parent.out.println(annotation.getLeft() + ": " + annotation.getMiddle() + " -> " + annotation.getRight());
      }
    }

    @Command(description = "List all possible entities to use")
    void possibleEntities(@Parameters(paramLabel = "regexp", defaultValue = ".*", description = ENTITY_REGEXP) String regexp){
      parent.printList(state.getAnnotatableEntities(regexp));
    }

    @Command(name = "select", description = "Select declassifications") int selectEOrT(
        @Parameters(paramLabel = "tag or entity", description = "Tag or entity (" + ENTITY_SYNTAX + ")") String tagOrEntity,
        @Parameters(paramLabel = "level", defaultValue = "", description = "From security level") String fromLevel,
        @Parameters(paramLabel = "level", defaultValue = "", description = "To security level") String toLevel) {
      if (!state.selectDeclassification(tagOrEntity, fromLevel, toLevel)) {
        List<Triple<String, String, String>> strings = state.selectDeclassificationAnnotations(tagOrEntity);
        if (strings.isEmpty()) {
          parent.log("No annotation found");
          return ExitCode.SOFTWARE;
        }
        for (Triple<String, String, String> annotation : strings) {
          parent.out.println(annotation.getLeft() + ": " + annotation.getMiddle() + " -> " + annotation.getRight());
        }
      }
      return 0;
    }

    @Command(name = "current", description = "List the current ${PARENT-COMMAND-NAME} with their level")
    static class CurrentCommand implements Runnable {
      @ParentCommand DeclassificationsCommand parent;

      @Override public void run() {
        for (Triple<String, String, String> annotation : parent.state.getDeclassifications()) {
          parent.parent.out.println(annotation.getLeft() + ": " + annotation.getMiddle() + " -> " + annotation.getRight());
        }
      }
    }

    @Override
    public Integer call() {
      return parent.printUsage(spec);
    }

    @Command(description = "Remove the declassifications that belong to the selected entities")
    boolean remove(@Parameters(paramLabel = "regexp", description = ENTITY_REGEXP) String regexp){
      List<Triple<String, String, String>> removed = this.state.getDeclassifications().stream().filter(p -> p.getLeft().matches(regexp))
          .collect(Collectors.toList());
      for (Triple<String, String, String> pair : removed) {
        parent.out.println("Removed " + pair.getLeft());
      }
      return state.removeDeclassifications(removed.stream().map(Triple::getLeft).collect(Collectors.toList()));
    }
  }


  public interface RunAnalysisEnabled<T> {
    T getMixin(AnalyzeCommand command);
    boolean run(T state);
  }

  @Command(name = "analyze", description = "Run an IFC analysis")
  static class AnalyzeCommand implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand
    CliCommands parent;

    RunAnalysisEnabled state;

    @DynamicMixin
    Object mixin;

    public AnalyzeCommand(@State RunAnalysisEnabled state){
      this.state = state;
      this.mixin = state.getMixin(this);
    }

    @Override public Integer call() {
      return exit(state.run(mixin));
    }
  }

  interface RunEnabled<T> extends SinksAndSourcesEnabled, EntryPointEnabled, RunAnalysisEnabled<T> {
    default boolean addAnnotations(String tag){
      return setEntryPoint(tag) != null && selectSinkAnnotations(tag).size() > 0 && selectSourceAnnotations(tag).size() > 0 &&
          (this instanceof DeclassificationEnabled ?
              ((DeclassificationEnabled) this).selectDeclassificationAnnotations(tag) != null : true) &&
          (this instanceof SetValueEnabled ? ((SetValueEnabled) this).selectSetValueAnnotations(tag) != null : true);
    }

    T getMixin(RunCommand command);

    default boolean resetAnnotations() {
      return resetSinks() && resetSources();
    }
  }

  @Command(name = "run", description = "Use annotations and entry point for a specific tag and run the analysis. "
      + "Does not reset all previously set sinks and sources by default, use the `--reset` option to enforce this.")
  static class RunCommand implements Callable<Integer> {

    RunEnabled state;

    @DynamicMixin
    Object mixin;

    @Parameters
    String tag;

    @Option(names = "--reset",description = "resets all previously set sinks and sources")
    boolean reset;

    public RunCommand(@State RunEnabled state){
      this.state = state;
      this.mixin = state.getMixin(this);
    }

    @Override public Integer call() throws Exception {
      if (reset) {
        if (!state.resetAnnotations()){
          return ExitCode.SOFTWARE;
        }
      }
      if (!state.addAnnotations(tag)){
        return ExitCode.SOFTWARE;
      }
      return exit(state.run(mixin));
    }
  }

  public interface MiscAnnotationsEnabled {
    /**
     * @return [(entity, string representation)]
     */
    List<Pair<String, String>> getMiscAnnotations(String entityRegexp, String typeRegexp);
  }

  @Command(name = "miscAnnotations", description = "Get annotations not used for security levels (might need to build the SDG first)")
  static class MiscAnnotationsCommand implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    @ParentCommand
    CliCommands parent;

    MiscAnnotationsEnabled state;

    MiscAnnotationsCommand(@State MiscAnnotationsEnabled state){
      this.state = state;
    }

    @Parameters(paramLabel = "entityRegexp", defaultValue = ".*", description = "Optional regexp that matches entities", index = "0")
    String entityRegexp;

    @Parameters(paramLabel = "annotationRegexp", defaultValue = ".*", description = "Optional regexp that matches annotation types", index = "1")
    String annotationRegexp;

    @Override
    public Integer call() {
      for (Pair<String, String> annotation : state.getMiscAnnotations(entityRegexp, annotationRegexp)) {
        parent.out.println(annotation.getFirst() + ": " + annotation.getSecond());
      }
      return 0;
    }
  }

  static interface ViewEnabled {
    SDG getSDG();

    default List<SDGNode> getSDGMethods(){
      return SDGUtils.callGraph(getSDG()).vertexSet().stream()
          .filter(n -> !n.getBytecodeName().isEmpty()).collect(Collectors.toList());
    }
  }

  @Command(name = "view", description = "View the current SDG")
  static class ViewCommand implements Callable<Integer> {

    @ParentCommand
    CliCommands parent;

    @State
    ViewEnabled state;

    @Option(names = "--new", description = "Create new viewer instance")
    boolean newInstance = false;

    @Parameters(paramLabel = "method", defaultValue = "", description = "Optional method, opens a tab in the viewer upon start")
    String method;

    @Override public Integer call() throws Exception {
      new Thread(() -> {
        GraphViewer.launch(state.getSDG(), newInstance, () -> {
          if (method.length() > 0){
            if (!show(method)){
              parent.log(String.format("Method %s not found", method));
            }
          }
        }, method.isEmpty());
      }).start();
      return exit(true);
    }

    @Command(description = "List methods from the call graph")
    public void methods(@Parameters(paramLabel = "pattern", description = "Regexp pattern for filtering", defaultValue = ".*")
        String pattern){
      parent.printList(state.getSDGMethods().stream().map(SDGNode::getLabel).filter(s -> s.matches(pattern))
          .collect(Collectors.toList()));
    }

    boolean show(String method){
      GraphViewer viewer = GraphViewer.getInstance();
      Optional<SDGNode> first = state.getSDGMethods().stream().filter(n -> n.getLabel().equals(method) ||
          n.getBytecodeName().equals(method)).findFirst();
      if (first.isPresent()){
        viewer.showMethod(state.getSDG(), first.get().getProc());
        return true;
      }
      return false;
    }
  }

  /**
   * Top-level command that just prints help.
   */
  @Command(name = "", description = "JOANA shell",
      footer = {"", "Press Ctl-D to exit in interactive mode."})
  static class CliCommands implements Callable<Integer> {

    @Spec Model.CommandSpec spec;

    Object state;

    final boolean verbose;
    PrintWriter out = new PrintWriter(System.out);
    LineReaderImpl reader;

    CliCommands(boolean verbose) {
      this.verbose = verbose;
    }

    void setReader(LineReader reader){
      this.reader = (LineReaderImpl)reader;
      out = reader.getTerminal().writer();
    }

    @Override public Integer call() {
      return null;
    }

    public void asserts(boolean successful){
      if (!successful){
        throw new CommandLine.ExecutionException(spec.commandLine(), "Command failed");
      }
    }

    void log(String msg) {
      if (verbose){
        out.println(msg);
      }
    }

    int printUsage(Model.CommandSpec spec){
      out.println(spec.commandLine().getUsageMessage());
      return 0;
    }

    void printPairList(List<Pair<String, String>> pairs){
      for (Pair<String, String> pair : pairs) {
        out.println(pair.getFirst() + ": " + pair.getSecond());
      }
    }

    void printList(List<String> strings){
      strings.forEach(out::println);
    }

    void buildSDGIfPossible(){
      if (state instanceof BuildSDGEnabled){
        ((BuildSDGEnabled) state).buildSDG();
      }
    }
  }

  @Command(name = "help", description = "Print help and search for commands")
  static class HelpCommand implements Callable<Integer> {

    @Parameters(arity = "0..1", description = "Sub command to gather information on or pattern for searching sub commands")
    String subCommand;

    @ParentCommand
    CliCommands parent;

    @State
    Object state;

    @Option(names = "--deep", description = "Use the help of the sub commands of sub commands, …")
    boolean deep;

    @Override public Integer call() {
      if (subCommand == null){
        System.out.println(getHelp(parent.spec.commandLine(), null));
        return 0;
      }
      Map<String, CommandLine> subcommands = parent.spec.commandLine().getSubcommands();
      if (subcommands.containsKey(subCommand)){
        System.out.println(getHelp(parent.spec.commandLine(), subCommand));
        return 0;
      }
      List<String> suggestions = findCommands(subCommand);
      if (suggestions.size() > 0){
        parent.out.println("Found some possibly commands: ");
        for (String suggestion : suggestions) {
          parent.out.println(suggestion);
        }
        return 0;
      }
      return ExitCode.USAGE;
    }

    String getHelp(CommandLine line, String command){
      if (command == null){
        return line.getSubcommands().keySet().stream().map(c -> getHelp(line, c)).collect(Collectors.joining("\n\n"));
      }
      CommandLine subLine = line.getSubcommands().get(command);
      if (deep) {
        return Stream
            .concat(Stream.of(subLine.getUsageMessage()), subLine.getSubcommands().keySet().stream().map(c -> getHelp(subLine, c)))
            .collect(Collectors.joining("\n"));
      }
      return subLine.getUsageMessage();
    }

    List<String> findCommands(String needle){
      List<String> commands = new ArrayList<>();
      java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(needle, java.util.regex.Pattern.CASE_INSENSITIVE);
      for (String cmdName : parent.spec.commandLine().getSubcommands().keySet()) {
        String msg = getHelp(parent.spec.commandLine(), cmdName);
        if (pattern.matcher(msg).find()) {
          commands.add(cmdName);
        }
      }
      return commands;
    }
  }

  /**
   * Outer command
   */
  @Command(name = "", description = "JOANA CLI")
  static class OuterCommand implements Runnable {

    @Option(names = "-i", description = "Run in interactive mode")
    boolean interactive;

    @Option(names = "-v", description = "Enable verbose output")
    boolean verbose;

    @Parameters(arity = "0..*", description = "Commands to execute, equivalent to executing them in interactive mode")
    List<String> commands;

    @Spec Model.CommandSpec spec;

    @Mixin
    HelpOptions helpOptions;

    final Object state;
    private final List<Class<?>> commandClasses;

    OuterCommand(Object state, Class<?>... miscCommandClasses) {
      this.state = state;
      this.commandClasses = Stream.concat(Arrays.stream(miscCommandClasses),
          Arrays.stream(ImprovedCLI.class.getDeclaredClasses()))
          .collect(Collectors.toList());
    }

    public void run() {
      CommandLine cli = buildCommandLine();
      ((CliCommands)cli.getCommand()).state = state;
      if (verbose) {
        Logger.getGlobal().setLevel(Level.INFO);
      }
      if (!interactive && commands == null) {
        System.out.println(spec.commandLine().getUsageMessage());
        System.out.println(cli.getUsageMessage());
      } else {
        if (commands != null) {
          LineReaderImpl reader = null;
          try {
            reader = new LineReaderImpl(TerminalBuilder.builder().dumb(true).build());
          } catch (IOException e) {
            e.printStackTrace();
          }
          for (String cmd : commands) {
            try {
              ParsedLine pl = reader.getParser().parse(cmd, 0);
              String[] arguments = pl.words().toArray(new String[0]);
              cli.execute(arguments);
            } catch (UserInterruptException e) {
              // Ignore
            } catch (EndOfFileException e) {
              break;
            }
          }
        }
        if (interactive) {
          runInteractive(cli);
        }
      }
    }

    /**
     * Builds a command line based on the commands in this class that have a field that is annotated with
     * {@link State} and for which the passed state object is assignable to this field.
     * Commands that have a constructor with a single argument (annotated with {@link State}) are also supported.
     * <p>
     * Mixins from fields annotated with {@link DynamicMixin} are added (for commands) as well as mixins
     * from {@code get[command, capitalized][sub command, capitalized]CommandOptions()} (for sub commands) and
     * from {@code get[command, capitalized]CommandOptions()} of the state.
     * <p>
     * Sets the annotated fields accordingly.
     *
     * @return command line, using the {@link CliCommands} class as a base command
     */
    CommandLine buildCommandLine() {
      CommandLine cli = new CommandLine(new CliCommands(verbose));
      for (Class<?> klass : commandClasses) {
        if (!Runnable.class.isAssignableFrom(klass) && !Callable.class.isAssignableFrom(klass)) {
          continue;
        }
        createForState(klass, state).ifPresent(cmd -> {
          cli.addSubcommand(cmd);
          CommandLine cmdLine = cli.getSubcommands().get(getNameOfCommandObject(cli, cmd));
          Arrays.stream(klass.getDeclaredFields()).filter(f -> f.isAnnotationPresent(DynamicMixin.class)).forEach(f -> {
            try {
              cmdLine.addMixin(f.getName(), f.get(cmd));
            } catch (IllegalAccessException e) {
              e.printStackTrace();
            }
          });
          cmdLine.getSubcommands().forEach((s, subCmdLine) -> {
            addMixinFromMethod(subCmdLine, cmdLine.getCommandName(), subCmdLine.getCommandName());
          });
          addMixinFromMethod(cmdLine, cmdLine.getCommandName());
        });
      }
      cli.getSubcommands().forEach((s, l) -> {
        if (!l.getMixins().containsKey("help")) {
          l.addMixin("help", new HelpOptions());
        }
      });
      return cli;
    }

    void addMixinFromMethod(CommandLine cmdLine, String... methodParts) {
      String methodName = "get" + Arrays.stream(methodParts).map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
          .collect(Collectors.joining("")) + "CommandOptions";
      try {
        cmdLine.addMixin(state.getClass().getSimpleName(), state.getClass().getMethod(methodName).invoke(state));
      } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      }
    }

    String getNameOfCommandObject(CommandLine parent, Object cmd) {
      return parent.getSubcommands().entrySet().stream().filter(e -> e.getValue().getCommand() == cmd).map(Map.Entry::getKey)
          .findFirst().get();
    }

    Optional<Field> getStateField(Class<?> commandClass, Class<?> stateClass) {
      return Arrays.stream(commandClass.getDeclaredFields())
          .filter(f -> f.getAnnotationsByType(State.class).length > 0 && f.getType().isAssignableFrom(stateClass)).findFirst();
    }

    Optional<Constructor<?>> getStateConstructor(Class<?> commandClass, Class<?> stateClass) {
      return Arrays.stream(commandClass.getDeclaredConstructors()).filter(
          c -> c.getParameterCount() == 1 && c.getParameterTypes()[0].isAssignableFrom(stateClass) && c.getParameters()[0]
              .isAnnotationPresent(State.class)).findFirst();
    }

    <T, S> Optional<?> createForState(Class<T> commandClass, S state) {
      Optional<Field> stateField = getStateField(commandClass, state.getClass());
      Optional<Constructor<?>> stateConstructor = getStateConstructor(commandClass, state.getClass());
      assert !stateField.isPresent() || !stateConstructor.isPresent();
      return Optional.ofNullable(stateConstructor.map(s -> {
        try {
          return (Object) s.newInstance(state);
        } catch (InstantiationException | InvocationTargetException | IllegalAccessException e) {
          e.printStackTrace();
        }
        return null;
      }).orElse(stateField.map(f -> {
        try {
          Object cmd = commandClass.newInstance();
          f.set(cmd, state);
          return cmd;
        } catch (InstantiationException | IllegalAccessException e) {
          e.printStackTrace();
        }
        return null;
      }).orElse(null)));
    }

    void runInteractive(CommandLine cli) {
      try {
        // set up the completion
        Level level = Logger.getLogger("org.jline").getLevel();
        Logger.getLogger("org.jline").setLevel(Level.OFF); // disable warnings for dumb terminals
        Terminal terminal = TerminalBuilder.builder().build();
        Logger.getLogger("org.jline").setLevel(level);
        LineReader reader = LineReaderBuilder.builder().terminal(terminal)
            .completer(new PicocliJLineCompleter(cli.getCommandSpec())).parser(new DefaultParser()).build();
        ((CliCommands) cli.getCommand()).setReader(reader);
        String prompt = "joana> ";
        String rightPrompt = null;

        // start the shell and process input until the user quits with Ctl-D
        String line;
        boolean lastSuccessful = true;
        while (true) {
          try {
            line = reader.readLine(
                Ansi.ansi().a(Ansi.Attribute.RESET).fg(lastSuccessful ? Ansi.Color.GREEN : Ansi.Color.RED).boldOff().render(prompt)
                    .toString(), null, (MaskingCallback) null, null);
            ParsedLine pl = reader.getParser().parse(line, 0);
            String[] arguments = pl.words().toArray(new String[0]);
            lastSuccessful = cli.execute(arguments) == 0;
          } catch (UserInterruptException e) {
            // Ignore
          } catch (EndOfFileException e) {
            return;
          }
        }
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

  private static int exit(boolean successful) {
    return successful ? 0 : ExitCode.SOFTWARE;
  }

  /**
   * Command that clears the screen.
   */
  @Command(name = "clear",
      description = "Clears the screen")
  static class ClearScreen implements Callable<Void> {

    @State
    Object state;

    @ParentCommand CliCommands parent;

    public Void call() {
      parent.reader.clearScreen();
      return null;
    }
  }

  @Command(name = "exit",
      description = "Exits the program")
  static class ExitCommand implements Callable<Void> {

    @State
    Object state;

    public Void call() {
      System.exit(0);
      return null;
    }
  }

  @Command(name = "verbose",
      description = "Enable and disable verbose output")
  static class VerboseCommand implements Callable<Void> {

    @State
    Object state;

    @Parameters
    boolean enable;

    public Void call() {
      Logger.getGlobal().setLevel(enable ? Level.INFO : Level.WARNING);
      return null;
    }
  }


  public static void run(String[] args, Object state, boolean interactive) {
    OuterCommand command = new OuterCommand(state);
    if (interactive){
      command.interactive = true;
      command.run();
    }
    System.exit(new CommandLine(command).execute(args));
  }

  static class Dummy
      implements ClassPathEnabled, SetValueEnabled, SinksAndSourcesEnabled, EntryPointEnabled, RunAnalysisEnabled<Object>,
      MiscAnnotationsEnabled {
    @Override public boolean setClassPath(String classPath) {
      return false;
    }

    @Override public String getClassPath() {
      return null;
    }

    @Override public List<Pair<String, String>> getSettableEntities(String regexp) {
      return Collections.emptyList();
    }

    @Override public List<Pair<String, ValueToSet>> getSetValueAnnotations(String tag) {
      return Collections.emptyList();
    }

    @Override public boolean setValueOfEntity(String entity, String value) {
      return false;
    }

    @Override public boolean setRefValueOfEntity(String entity, String ref) {
      return false;
    }

    @Override public List<Pair<String, ValueToSet>> selectSetValueAnnotations(String tag) {
      return Collections.emptyList();
    }

    @Override public List<Pair<String, Pair<String, ValueToSet.Mode>>> getSetValues() {
      return Collections.emptyList();
    }

    @Override public boolean removeSetValues(List<String> programParts) {
      return false;
    }

    @Override public List<String> getPossibleEntryMethods(String regexp) {
      return Collections.emptyList();
    }

    @Override public List<Pair<String, String>> getPossibleEntryPoints() {
      return Collections.emptyList();
    }

    @Override public boolean setEntryMethod(String method) {
      return false;
    }

    @Override public String setEntryPoint(String tag) {
      return "";
    }

    @Override public String getCurrentEntry() {
      return "";
    }

    @Override public List<Pair<String, String>> getSinkAnnotations(String tag) {
      return Collections.emptyList();
    }

    @Override public List<Pair<String, String>> getSourceAnnotations(String tag) {
      return Collections.emptyList();
    }

    @Override public List<String> getAnnotatableEntities(String regexp) {
      return Collections.emptyList();
    }

    @Override public List<Pair<String, String>> selectSourceAnnotations(String tag) {
      return Collections.emptyList();
    }

    @Override public List<Pair<String, String>> selectSinkAnnotations(String tag) {
      return Collections.emptyList();
    }

    @Override public boolean selectSource(String entity, String level) {
      return false;
    }

    @Override public boolean selectSink(String entity, String level) {
      return false;
    }

    @Override public List<Pair<String, String>> getSources() {
      return Collections.emptyList();
    }

    @Override public List<Pair<String, String>> getSinks() {
      return Collections.emptyList();
    }

    @Override public boolean removeSinks(List<String> programParts) {
      return false;
    }

    @Override public boolean removeSources(List<String> programParts) {
      return false;
    }

    @Override public Object getMixin(AnalyzeCommand command) {
      return new Dummy2();
    }

    @Override public boolean run(Object state) {
      return false;
    }

    @Override public List<Pair<String, String>> getMiscAnnotations(String entityRegexp, String typeRegexp) {
      return Collections.emptyList();
    }

    static class Dummy2 {
      @Option(names = "bla") int bla;

      @Command void test() {
      }
    }
  }

  public static void main(String[] args) {
    run(args, new Dummy(), false);
  }

}
