/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.wala.sdpn;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.GraphIntegrity.UnsoundGraphException;

import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.graph.slicer.graph.threads.MHPAnalysis;
import edu.kit.joana.ifc.sdg.mhpoptimization.CSDGPreprocessor;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.wala.core.Main;
import edu.kit.joana.wala.core.SDGBuilder;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.core.SDGBuilder.FieldPropagation;
import edu.kit.joana.wala.core.SDGBuilder.PointsToPrecision;

public class SDGCreatorMoJoStyle extends SDGCreator {

	@Override
	public JoanaSDGResult buildSDG(String classPath, String mainClass, String runtimeLib, String outputSDGFile,
			IProgressMonitor progress) throws IllegalArgumentException, CancelException,
			IOException, WalaException, InvalidClassFileException {
		mainClass = mainClass.replace('/', '.').replace('$', '.').substring(1);
		JavaMethodSignature mainMethod = JavaMethodSignature.mainMethodOfClass(mainClass);
		Main.Config cfg = new Main.Config(mainMethod.toBCString(), mainMethod.toBCString(), classPath, FieldPropagation.OBJ_GRAPH);
		cfg.exceptions = ExceptionAnalysis.INTERPROC;
		cfg.pts = PointsToPrecision.OBJECT_SENSITIVE;
		cfg.fieldPropagation = FieldPropagation.OBJ_GRAPH_FIXPOINT_PROPAGATION;
		cfg.accessPath = false;
		cfg.stubs = CSDGwithSDPNBuilder.JRE14_STUBS;
		Pair<SDG, SDGBuilder> p;
		try {
			 p = Main.computeAndKeepBuilder(new PrintStream(new ByteArrayOutputStream()), cfg, true, progress);
		} catch (UnsoundGraphException e) {
			throw new RuntimeException(e);
		}

		MHPAnalysis mhp = doMHP(outputSDGFile, p.fst, true, progress);

		SDGBuilder builder = p.snd;
		return new JoanaSDGResult(p.fst, builder.getWalaCallGraph(), builder.getPointerAnalysis(), mhp);
	}

	private String extractStubsPath(String runtimeLib) {
		String[] parts = runtimeLib.split(",");
		for (String part : parts) {
			if (part.contains("stubs")) {
				return part;
			}
		}
		return null;
	}

	@Override
	protected MHPAnalysis runMHP(SDG joanaSdg, IProgressMonitor progress) throws CancelException {
		return CSDGPreprocessor.runMHP(joanaSdg);
	}

}
