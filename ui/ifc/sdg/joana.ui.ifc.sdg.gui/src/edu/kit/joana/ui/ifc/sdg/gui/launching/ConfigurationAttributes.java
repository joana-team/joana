/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.ifc.sdg.gui.launching;

public class ConfigurationAttributes {
	/**
	 * Use this sdg if 'never rebuild' is enabled.
	 */
	public static final String SDG_LOCATION = "SDG_LOCATION";
	/**
	 * Rebuild SDG everytime/always
	 */
	public static final String SDG_REB_ALW = "SDG_REB_ALW";
	/**
	 * Rebuild SDG only if none available/ if necessary(should include rebuild after source-change)
	 */
	public static final String SDG_REB_NEC = "SDG_REB_NEC";
	/**
	 * Never Rebuild sdg. Use given sdg.
	 */
	public static final String SDG_REB_NEV = "SDG_REB_NEV";
	/**
	 * Name of project the configuration corresponds to
	 */
	public static final String PROJECT_NAME = "PROJECT_NAME";
	/**
	 * lattice-path
	 */
	public static final String LATTICE_LOCATION = "LATTICE_LOCATION";

	/**
	 * main class thats used to generate sdg
	 */
	public static final String MAIN_CLASS_NAME = "MAIN_CLASS_NAME";

	/**
	 * Tells, if launching configuration is standard configuration for project
	 * (That means its attributes are used for annotation in gui)
	 */
	public static final String IS_PROJECT_STANDARD = "IS_PROJECT_STANDARD";

	/**
	 * tells if simple chop should be generated for every found violation
	 */
	public static final String GEN_SIMPLE_CHOP = "GEN_SIMPLE_CHOP";
	public static final String USE_JOANA_COMPILER = "USE_JOANA_COMPILER";


    // symbolic constants for SDG path configurations
    public static final String MEMORY_XMX = "MEMORY_XMX";
    public static final String JAVA_HOME = "JAVA_HOME";
    public static final String STUBS = "STUBS";
    public static final String SDG_LIB = "SDG_LIB";

    // symbolic constants for SDG generator options
    public static final String WHOLE = "WHOLE";
    public static final String SUMMARY = "SUMMARY";
    public static final String FINE_GRAINED = "FINE_GRAINED";
    public static final String INTERFERENCE = "INTERFERENCE";
    public static final String CFG = "CFG";
    public static final String CONC = "CONC";
    public static final String FILTER = "FILTER";

    // kinds of IFC
    public static final String CLASS_NI = "CLASS_NI";
    public static final String CLASS_NI_TS = "CLASS_NI_TS";
    public static final String KRINKE_NI = "KRINKE_NI";
    public static final String PROB_NI = "PROB_NI";
    public static final String PROB_NI_TS = "PROB_NI_TS";
    public static final String POSS_NI = "POSS_NI";
    public static final String POSS_NI_TS = "POSS_NI_TS";

    //Marker
    public static final String MARKER = "MARKER";
}
