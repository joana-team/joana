package edu.kit.joana.ifc.sdg.qifc.qif_interpreter.stat;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.TestUtils;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Method;
import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.ir.Program;
import org.junit.Test;

import java.io.IOException;
import java.util.Optional;

class RecursiveInvocationHandlerTest {

	@Test public void constructorTest() throws IOException, InterruptedException {
		Program p = TestUtils.build("SimpleRecursion");
		Method entry = p.getEntryMethod();

		Optional<SSAInstruction> call = entry.getCFG().getBlock(1).instructions().stream()
				.filter(i -> i instanceof SSAInvokeInstruction).findFirst();
		assert (call.isPresent());
	}

}