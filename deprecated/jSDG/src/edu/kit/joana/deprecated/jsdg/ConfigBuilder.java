/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.deprecated.jsdg;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import edu.kit.joana.deprecated.jsdg.SDGFactory.Config;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.ObjTreeType;
import edu.kit.joana.deprecated.jsdg.SDGFactory.Config.PointsToType;
import edu.kit.joana.wala.util.CmdLineParser;
import edu.kit.joana.wala.util.CmdLineParser.IllegalOptionValueException;
import edu.kit.joana.wala.util.CmdLineParser.Option;
import edu.kit.joana.wala.util.CmdLineParser.UnknownOptionException;


/**
 * Creates a set of configuration files from a single config file. Used for the
 * evaluation of all thinkable variants of the analysis.
 *
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public class ConfigBuilder {

	private final String className;
	private final String prefix;
	private final Config cfg;
	private String basePath;
	private boolean toggleExceptions;
	private boolean toggleOptimizeExceptions;
	private final Set<ObjTreeType> heapVariants;
	private final Set<PointsToType> ptsVariants;

	private ConfigBuilder(final Config cfg, final String className, final String prefix) {
		this.className = className;
		this.prefix = prefix;
		this.cfg = cfg;
		this.heapVariants = new HashSet<ObjTreeType>();
		this.ptsVariants = new HashSet<PointsToType>();
	}

	/**
	 * @param args
	 * @throws UnknownOptionException
	 * @throws IllegalOptionValueException
	 * @throws IOException
	 * @throws CloneNotSupportedException
	 */
	public static void main(String[] args) throws IllegalOptionValueException, UnknownOptionException, IOException, CloneNotSupportedException {
		File f = new File(".");
		System.out.printf("Running in %1s\n", f.getCanonicalPath());
		CmdLineParser cmdLine = new CmdLineParser();
		Option cfg = cmdLine.addStringOption('c', "config");
		Option path = cmdLine.addStringOption('t', "path");
		Option pts = cmdLine.addStringOption('p', "points-to");
		Option heap = cmdLine.addStringOption('h', "heap-model");
//		Option prefix = cmdLine.addStringOption('n', "name");
		Option exc = cmdLine.addBooleanOption('e', "exception");
//		Option optImmutables = cmdLine.addBooleanOption("opt-immutables");
		Option optExceptions = cmdLine.addBooleanOption('o', "opt-exc");
		cmdLine.parse(args);

		String basePath = (String) cmdLine.getOptionValue(path);
		if (basePath == null) {
			throw new IllegalArgumentException("A output path has to be provided.");
		}
		File to = new File(basePath);
		if (to.exists()) {
			if (!to.isDirectory()) {
				throw new IllegalArgumentException(basePath + " is a file. It should be a directory.");
			}
			if (!to.canWrite()) {
				throw new IllegalArgumentException(basePath + " is not writable.");
			}
		} else {
			to.mkdirs();
		}

		String ptsVariants = (String) cmdLine.getOptionValue(pts);
		Set<PointsToType> ptsVars = new HashSet<PointsToType>();
		if (ptsVariants != null) {
			String [] toks = ptsVariants.split(",");

			for (int i = 0; i < toks.length; i++) {
				SDGFactory.Config.PointsToType pt = SDGFactory.Config.PointsToType.valueOf(toks[i]);
				ptsVars.add(pt);
			}
		}

		String heapVariants = (String) cmdLine.getOptionValue(heap);
		Set<ObjTreeType> heapVars = new HashSet<ObjTreeType>();
		if (heapVariants != null) {
			String [] toks = heapVariants.split(",");

			for (int i = 0; i < toks.length; i++) {
				SDGFactory.Config.ObjTreeType ott = SDGFactory.Config.ObjTreeType.valueOf(toks[i]);
				heapVars.add(ott);
			}
		}

		Boolean tmp = (Boolean) cmdLine.getOptionValue(exc);
		final boolean exceptions = (tmp != null) && tmp;

		tmp = (Boolean) cmdLine.getOptionValue(optExceptions);
		final boolean optimizeExc = (tmp != null) && tmp;

		String baseCfg = (String) cmdLine.getOptionValue(cfg);

		File cfgFile = new File(baseCfg);
		if (cfgFile.isDirectory()) {
			File[] cfgs = cfgFile.listFiles(new FileFilter() {
				public boolean accept(File pathname) {
					return pathname.isFile() && pathname.getName().endsWith(".cfg");
				}});

			for (File file : cfgs) {
				runWithFile(file, basePath, ptsVars, heapVars, exceptions, optimizeExc);
			}
		} else {
			runWithFile(cfgFile, basePath, ptsVars, heapVars, exceptions, optimizeExc);
		}
	}

	private static void runWithFile(final File config, final String basePath,
			final Set<PointsToType> ptsVars, final Set<ObjTreeType> heapVars,
			final boolean exceptions, final boolean optExceptions)
	throws IOException, CloneNotSupportedException {
		String baseCfg = config.getAbsolutePath();
		System.out.println("Read in configuration from " + baseCfg);
		FileInputStream fIn = new FileInputStream(config);
		SDGFactory.Config base = SDGFactory.Config.readFrom(fIn);

		// remove first char 'L' and convert '/' to '.' - this converts a bytecode name to a more readable class name
		String name = base.mainClass.replace('/', '.').substring(1);
		String prefixName = extractName(baseCfg);
		ConfigBuilder builder = new ConfigBuilder(base, name, prefixName);

		builder.setBasePath(basePath);

		if (ptsVars.isEmpty()) {
			ptsVars.add(base.pointsTo);
		}

		for (PointsToType pts : ptsVars) {
			builder.addPtsVariant(pts);
		}

		if (heapVars.isEmpty()) {
			heapVars.add(base.objTree);
		}

		for (ObjTreeType ott : heapVars) {
			builder.addHeapVariant(ott);
		}

		builder.setToggleExceptions(exceptions);

		builder.setToggleOptimizeExceptions(optExceptions);

		builder.createConfigFiles();
	}

	private static String extractName(String str) {
		if (str.contains(".")) {
			str = str.substring(0, str.lastIndexOf('.'));
		}
		if (str.contains("/")) {
			str = str.substring(str.lastIndexOf('/'));
		}

		return str;
	}

	public void createConfigFiles() throws CloneNotSupportedException, FileNotFoundException {
		File baseFile = new File(basePath);
		if (!baseFile.exists()) {
			baseFile.mkdirs();
		}

		for (final ObjTreeType ott : heapVariants) {
			for (final PointsToType ptt : ptsVariants) {
				if (toggleExceptions && toggleOptimizeExceptions) {
					makeCfg(ott, ptt, true, false);
					makeCfg(ott, ptt, false, true);
					makeCfg(ott, ptt, false, false);
				} else if (toggleExceptions) {
					makeCfg(ott, ptt, true, cfg.optimizeExceptions);
					makeCfg(ott, ptt, false, cfg.optimizeExceptions);
				} else if (toggleOptimizeExceptions) {
					makeCfg(ott, ptt, cfg.ignoreExceptions, true);
					makeCfg(ott, ptt, cfg.ignoreExceptions, false);
				} else {
					makeCfg(ott, ptt, cfg.ignoreExceptions, cfg.optimizeExceptions);
				}
			}
		}
	}

	private void setToggleExceptions(boolean exceptions) {
		this.toggleExceptions = exceptions;
	}

	private void setToggleOptimizeExceptions(boolean optimize) {
		this.toggleOptimizeExceptions = optimize;
	}

	private void addHeapVariant(ObjTreeType pt) {
		heapVariants.add(pt);
	}

	private void addPtsVariant(PointsToType pt) {
		ptsVariants.add(pt);
	}

	private void setBasePath(String basePath) {
		if (!basePath.endsWith("/")) {
			this.basePath = basePath + "/";
		}

		this.basePath = basePath;
	}

	private void makeCfg(ObjTreeType ott, PointsToType ptt, boolean exc, boolean optimizeExc)
	throws CloneNotSupportedException, FileNotFoundException {
		String suffix = suffix(ptt, ott, exc, optimizeExc);
		Config newCfg = cfg.clone();
		newCfg.objTree = ott;
		newCfg.useWalaSdg = (ott == ObjTreeType.WALA);
		newCfg.pointsTo = ptt;
		newCfg.ignoreExceptions = exc;
		newCfg.optimizeExceptions = optimizeExc;
		newCfg.outputSDGfile = basePath + prefix + suffix + "/" + className + ".pdg";
		newCfg.outputDir = basePath + prefix + suffix + "/";
		newCfg.logFile = basePath + prefix + suffix + "/" + className + ".log";
		System.out.printf("Building configuration for %1s with %2s\n", className, suffix);
		String cfgFileName = prefix + suffix + ".cfg";
		FileOutputStream fOut = new FileOutputStream(basePath + cfgFileName);
		newCfg.writeTo(fOut);
	}

	private static String suffix(Config.PointsToType pts, Config.ObjTreeType heap, boolean exc, boolean optExc) {
		String suffix = "";

		switch (heap) {
		case DIRECT_CONNECTIONS:
			suffix += "-direct";
			break;
		case K1_LIMIT:
			suffix += "-tree_k1";
			break;
		case K2_LIMIT:
			suffix += "-tree_k2";
			break;
		case K3_LIMIT:
			suffix += "-tree_k3";
			break;
		case PTS_GRAPH:
			suffix += "-graph";
			break;
		case PTS_GRAPH_NO_FIELD:
			suffix += "-graph_no_field";
			break;
		case PTS_GRAPH_NO_FIELD_NO_REFINE:
			suffix += "-graph_no_field_no_refine";
			break;
		case PTS_GRAPH_NO_REFINE:
			suffix += "-graph_no_refine";
			break;
		case PTS_LIMIT:
			suffix += "-tree";
			break;
		case WALA:
			suffix += "-wala";
			break;
		case ZERO:
			suffix += "-none";
			break;
		}

		switch (pts) {
		case n0CFA:
			suffix += "-pts_n0";
			break;
		case n1CFA:
			suffix += "-pts_n1";
			break;
		case n2CFA:
			suffix += "-pts_n2";
			break;
		case n3CFA:
			suffix += "-pts_n3";
			break;
		case OBJ_SENS:
			suffix += "-pts_obj";
			break;
		case VANILLA_ZERO_ONE_CFA:
			suffix += "-pts_v01";
			break;
		case VANILLA_ZERO_ONE_CONTAINER_CFA:
			suffix += "-pts_v01c";
			break;
		case ZERO_CFA:
			suffix += "-pts_0";
			break;
		case ZERO_ONE_CFA:
			suffix += "-pts_01";
			break;
		}

		if (exc) {
			suffix += "-no_exc";
		} else {
			suffix += "-exc";
		}

		if (optExc) {
			suffix += "-opt_exc";
		} else {
			suffix += "-no_opt_exc";
		}

		return suffix;
	}

}
