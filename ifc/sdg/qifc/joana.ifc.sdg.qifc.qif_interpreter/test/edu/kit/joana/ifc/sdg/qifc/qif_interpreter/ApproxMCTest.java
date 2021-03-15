package edu.kit.joana.ifc.sdg.qifc.qif_interpreter;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ApproxMCTest {

	@Test void patternTest() {
		ApproxMC approxMC = new ApproxMC();
		assertTrue(approxMC.isResult("s mc 3\n"));
		assertTrue(approxMC.isResult("s mc 123455\n"));
		assertFalse(approxMC.isResult("sdasfh"));
		assertFalse(approxMC.isResult("s mc "));
	}

	@Test void invokeTest() throws IOException, InterruptedException {
		ApproxMC approxMC = new ApproxMC();
		int res = approxMC.invokeApproxMC("testResources/mc/1.cnf", null);
		assertEquals(3, res);
		res = approxMC.invokeApproxMC("testResources/mc/2.cnf", null);
		assertEquals(5, res);
	}

}