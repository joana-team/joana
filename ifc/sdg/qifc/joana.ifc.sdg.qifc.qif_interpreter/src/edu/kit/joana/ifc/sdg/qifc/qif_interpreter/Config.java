package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

/**
 * captures some configurations for program analysis
 * If not specified, the default values are used
 * <p>
 * Configurations can be changed during analysis --> using different settings for different program parts is possible
 */
public class Config {

	public static boolean usePP = false;
	public static boolean useHybrid = false;
	public static int bitwidth = 32;

	// default values
	public static final int DEFAULT_LOOP_UNROLLING_MAX = 32;
	public static final int DEFAULT_RECURSION_DEPTH_MAX = 32;
	public static final int DEFAULT_METHOD_DEPTH_MAX = 2;

	private int loopUnrollingMax;
	private int recDepthMax;
	private int methodDepthMax;

	public Config(int loopMax, int recMax, int methodMax) {
		this.loopUnrollingMax = loopMax;
		this.recDepthMax = recMax;
		this.methodDepthMax = methodMax;
	}

	public static Config defaultConfig() {
		return new Config(DEFAULT_LOOP_UNROLLING_MAX, DEFAULT_RECURSION_DEPTH_MAX, DEFAULT_METHOD_DEPTH_MAX);
	}

	public Config setLoopUnrollingMax(int limit) {
		this.loopUnrollingMax = limit;
		return this;
	}

	public Config setRecursionDepthMax(int limit) {
		this.recDepthMax = limit;
		return this;
	}

	public int loopUnrollingMax() {
		return this.loopUnrollingMax;
	}

	public int recDepthMax() {
		return this.recDepthMax;
	}

	public int methodDepthMax() {
		return this.methodDepthMax;
	}
}