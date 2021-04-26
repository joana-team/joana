package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

/**
 * captures some configurations for program analysis
 * If not specified, the default values are used
 * <p>
 * Configurations can be changed during analysis --> using different settings for different program parts is possible
 */
public class Config {

	// default values
	private static final int DEFAULT_LOOP_UNROLLING_MAX = 4;
	private static final int DEFAULT_RECURSION_DEPTH_MAX = 4;

	private int loopUnrollingMax;
	private int recDepthMax;

	private Config() {
		this.loopUnrollingMax = DEFAULT_LOOP_UNROLLING_MAX;
		this.recDepthMax = DEFAULT_RECURSION_DEPTH_MAX;
	}

	public static Config defaultConfig() {
		return new Config();
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

}
