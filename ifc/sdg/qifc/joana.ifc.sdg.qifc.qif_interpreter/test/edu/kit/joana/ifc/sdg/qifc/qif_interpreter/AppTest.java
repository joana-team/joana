package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.graph.GraphIntegrity;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.BBlock;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.CFG;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {

	private static final String JARPATH = "../../../../dist/joana.ifc.sdg.qifc.qif_interpreter.jar";
	private static final String testFilePath = "examples/";
	private static final String IF = "If";
	private static final String AND = "And";
	private static final String OR = "Or";
	private static final String LOOP = "Loop";

	private String path(String className) {
		return testFilePath + className+ ".class";
	}

	private void compile(String file) throws IOException, InterruptedException {
		String cmd = String.format("javac -target 1.8 -source 1.8  %s -classpath %s", file, JARPATH);
		Process compilation = Runtime.getRuntime().exec(cmd);
		compilation.waitFor();
	}

	@Test
	public void recognizeCondHead() throws IOException, InterruptedException {
		Program p = build(IF);
		p.getEntryMethod().getCFG().print();

		CFG g = p.getEntryMethod().getCFG();
		assertEquals(1, g.getBlocks().stream().filter(BBlock::isCondHeader).count());
		assertTrue(g.getBlocks().stream().filter(BBlock::isCondHeader).findFirst().get().getWalaBasicBLock().getLastInstruction().toString().contains("conditional branch"));
	}

	@Test
	public void recognizeLoopHead() throws IOException, InterruptedException {
		Program p = build(LOOP);
		p.getEntryMethod().getCFG().print();

		CFG g = p.getEntryMethod().getCFG();
		assertEquals(1, g.getBlocks().stream().filter(BBlock::isLoopHeader).count());
		assertTrue(g.getBlocks().stream().filter(BBlock::isLoopHeader).findFirst().get().getWalaBasicBLock().getLastInstruction().toString().contains("conditional branch"));
	}

	private Program build(String fileName) throws IOException, InterruptedException {
		compile(testFilePath+fileName+".java");

		// create SDG
		IRBuilder builder = new IRBuilder(path(fileName), fileName);
		builder.createBaseSDGConfig();
		try {
			builder.buildAndKeepBuilder();
		} catch (IOException | CancelException | ClassHierarchyException | GraphIntegrity.UnsoundGraphException e) {
			e.printStackTrace();
		}
		return builder.getProgram();
	}
}