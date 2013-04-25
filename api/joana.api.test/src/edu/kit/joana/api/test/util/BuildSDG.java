package edu.kit.joana.api.test.util;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgram;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.SDGSerializer;
import edu.kit.joana.ifc.sdg.mhpoptimization.MHPType;
import edu.kit.joana.ifc.sdg.mhpoptimization.PruneInterferences;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.util.Stubs;
import edu.kit.joana.wala.core.NullProgressMonitor;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;

public class BuildSDG {

	private final String classPath;
	private final String entryMethod;
	private final Stubs stubsPath;
	private final String fileName;
	private final boolean computeInterference = true;
	private final ExceptionAnalysis exceptionAnalysis;
	private final FieldPropagation fieldPropagation;
	private final MHPType mhpType = MHPType.NONE;

	private SDG sdg = null;

	public BuildSDG(String classPath, String entryMethod, Stubs stubsPath, String fileName) {
		this(classPath, entryMethod, stubsPath, ExceptionAnalysis.IGNORE_ALL, FieldPropagation.OBJ_GRAPH, fileName);
	}

	public BuildSDG(String classPath, String entryMethod, Stubs stubsPath, ExceptionAnalysis ea, FieldPropagation fp,
			String fileName) {
		this.classPath = classPath;
		this.entryMethod = entryMethod;
		this.stubsPath = stubsPath;
		this.exceptionAnalysis = ea;
		this.fieldPropagation = fp;
		this.fileName = fileName;
	}

	private void saveSDGProgram(SDG sdg, String path) throws FileNotFoundException {
		SDGSerializer.toPDGFormat(sdg, new FileOutputStream(path));
	}

	public void run() {
		try {
			sdg = buildSDG();
			PruneInterferences.preprocessAndPruneCSDG(sdg, MHPType.PRECISE);
		} catch (ClassHierarchyException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (UnsoundGraphException e) {
			throw new RuntimeException(e);
		} catch (CancelException e) {
			throw new RuntimeException(e);
		}

		try {
			saveSDGProgram(sdg, fileName);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public SDG getSDG() {
		if (sdg == null) {
			run();
		}
		return sdg;
	}

	private SDG buildSDG() throws ClassHierarchyException, IOException, UnsoundGraphException, CancelException {
		SDGConfig cfg = new SDGConfig(classPath, entryMethod, stubsPath);
		cfg.setComputeInterferences(computeInterference);
		cfg.setExceptionAnalysis(exceptionAnalysis);
		cfg.setMhpType(mhpType);
		cfg.setFieldPropagation(fieldPropagation);
		SDGProgram p = SDGProgram.createSDGProgram(cfg, new PrintStream(new ByteArrayOutputStream()),
				NullProgressMonitor.INSTANCE);
		return p.getSDG();
	}

	public static BuildSDG standardConcSetup(String classPath, JavaMethodSignature entryMethod, String saveAs) {
		return new BuildSDG(classPath, entryMethod.toBCString(), Stubs.JRE_14,
				ExceptionAnalysis.IGNORE_ALL, FieldPropagation.OBJ_GRAPH, saveAs);
	}
	
	public static BuildSDG standardConcSetup(String classPath, String mainClass, String saveAs) {
		return standardConcSetup(classPath, JavaMethodSignature.mainMethodOfClass(mainClass), saveAs);
	}
}
