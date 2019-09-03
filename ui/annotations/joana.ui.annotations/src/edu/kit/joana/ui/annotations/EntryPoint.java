package edu.kit.joana.ui.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.Repeatable;

/**
 * Denotes an entry point and specifies an analysis.
 */
@Repeatable(EntryPoints.class)
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface EntryPoint {
	
	String[] levels() default { Level.LOW, Level.HIGH};
	MayFlow[] lattice() default { @MayFlow(from=Level.LOW, to=Level.HIGH) };
	String[] datasets() default {};
	String[] adversaries() default {};
	EntryPointKind kind() default EntryPointKind.UNKNOWN;
	/**
   * Used to import SDG files, if the FROMFILE EntryPointKind is used.
   */
  String file() default "";
	PointsToPrecision pointsToPrecision() default PointsToPrecision.INSTANCE_BASED;
	/**
   * Compute chops (traces between source and sink). Currently only supported
   * by easyifc
   */
  ChopComputation chops() default ChopComputation.ALL;
	
  /**
   * Denotes classes that should be used as sink,
   * i.e. all methods of these classes are used as sinks.
   * This is helpful for annotating library classes.
   */
	String[] classSinks() default {};
	
	/**
	 * Tag of the analysis. Allows to identify this entry point an the analysis
	 * that starts with it. Empty tagged EntryPoints match only with empty tagged
   * sources and sinks.
	 * 
	 * Currently only supported by the console commands.
	 */
	String tag() default "";

  /**
   * Discard control flow edges.
   *
   * Currently only supported by the console.
   */
	boolean onlyDirectFlow() default false;

	/**
	 * Policy on pruning (throwing away call graph nodes)
	 */
	PruningPolicy pruningPolicy() default PruningPolicy.APPLICATION;

	/**
	 * Option for generating a class containing fields for all project types that can be used whenever a field of a specific
	 * type is not initialized (because it is initialized in code not called by the entry point)
	 *
	 * @return regular expression that matches all types which fields and method signatures are considered if they match the expression
	 */
	String uninitializedFieldTypeRegexp() default "";
}
