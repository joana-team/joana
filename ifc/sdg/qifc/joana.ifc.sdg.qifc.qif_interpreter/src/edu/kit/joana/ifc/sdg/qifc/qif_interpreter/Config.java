package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

/**
 * captures some configurations for program analysis
 * If not specified, the default values are used
 * <p>
 * Configurations can be changed during analysis --> using different settings for different program parts is possible
 */
public class Config {

	// default values
	public static final int DEFAULT_UNWIND = 32;
	public static final boolean DEFAULT_PP = false;
	public static final boolean DEFAULT_HYBRID = false;

	public static boolean usePP = DEFAULT_PP;
	public static boolean useHybrid = DEFAULT_HYBRID;
	public static int bitwidth = 32;
	public static int unwind = DEFAULT_UNWIND;

}