package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.SimpleLogger;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SDGBuilder {

	// some default settings
	private static final String DEFAULT_MAIN_CLASS = "Main";
	private static final JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(DEFAULT_MAIN_CLASS);
	private static final Stubs DEFAULT_STUBS = Stubs.JRE_15;
	private static final String SDG_FILE_SUFFIX = ".pdg";

	private final File classFile;
	private SDGProgram sdgProg;
	private SDGConfig config;

	public SDGBuilder(String classFilePath) {
		this.classFile = new File(classFilePath);
	}

	public void useDefaultConfiguration() {
		// TODO: find out what all these configurations do
		this.config = new SDGConfig(classFile.getAbsolutePath(), mainMethod.toBCString(), DEFAULT_STUBS);
		config.setComputeInterferences(false);
		config.setExceptionAnalysis(edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis.INTERPROC);
		config.setFieldPropagation(edu.kit.joana.wala.core.SDGBuilder.FieldPropagation.OBJ_GRAPH);
		config.setPointsToPrecision(edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision.OBJECT_SENSITIVE);
		config.setDefaultExceptionMethodState(MethodState.DEFAULT); // TODO: idk check what theyre doing in RunDemoIFC
	}

	public void build() {
		if (config == null) throw new IllegalStateException("Error: Missing Configuration");
		try {
			this.sdgProg = SDGProgram.createSDGProgram(config);
		} catch (ClassHierarchyException | GraphIntegrity.UnsoundGraphException | CancelException | IOException e) {
			e.printStackTrace();
		}
	}

	public void dumpGraph(String path) {
		SDG sdg = sdgProg.getSDG();
		String fileName = path + "/" + sdg.getName() + SDG_FILE_SUFFIX;
		try {
			SDGSerializer.toPDGFormat(sdg, new FileOutputStream(fileName));
			SimpleLogger.log("Dumping graph to " + fileName);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

}
