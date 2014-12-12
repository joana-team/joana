package edu.kit.joana.ifc.sdg.lattice.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.LatticeValidator;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;

public class DedekindMcNeilleCompletionTest {

	@Test
	public void testSmartHome() {
		IEditableLattice<String> smartHomeLattice = new EditableLatticeSimple<>();
		String[] levels  = { "customer", "provider", "application",
		                     "customer provider", "application customer", "application provider"
		};
		for (String level : levels) {
			smartHomeLattice.addElement(level);
		}
		smartHomeLattice.setImmediatelyGreater("customer provider", "provider");
		smartHomeLattice.setImmediatelyGreater("customer provider",  "customer");
		smartHomeLattice.setImmediatelyGreater("application customer", "customer");
		smartHomeLattice.setImmediatelyGreater("application customer", "application");
		smartHomeLattice.setImmediatelyGreater("application provider", "application");
		smartHomeLattice.setImmediatelyGreater("application provider", "provider");
		
		IEditableLattice<String> completion = LatticeUtil.dedekindMcNeilleCompletion(smartHomeLattice);
		assertNull(LatticeValidator.validateIncremental(completion));
		
		String top = completion.getTop();
		String bot = completion.getBottom();
		assertTrue(top.startsWith("newElement-"));
		assertTrue(top.startsWith("newElement-"));
		assertNotEquals(top, bot);

	}
	
	@Test
	public void test1() {
		IEditableLattice<String> simple = new EditableLatticeSimple<>();
		String[] levels  = { "a", "b", "t" };
		for (String level : levels) {
			simple.addElement(level);
		}
		simple.setImmediatelyGreater("a", "t");
		simple.setImmediatelyGreater("b", "t");
		
		IEditableLattice<String> completion = LatticeUtil.dedekindMcNeilleCompletion(simple);
		assertNull(LatticeValidator.validateIncremental(completion));
		
		String top = completion.getTop();
		String bot = completion.getBottom();
		assertEquals("t", top);
		assertTrue(bot.startsWith("newElement-"));

	}
	

	enum Block {LL,LR,UL,UR};
	@Test
	public void testWiki() {
		class BlockPos {
			final Block block;
			final int nr;
			
			public BlockPos(Block block, int nr) {
				this.block = block;
				this.nr = nr;
			}
			
			@Override
			public String toString() {
				return block + "_" + nr;
			}
		}
		
		IEditableLattice<String> simple = new EditableLatticeSimple<>();

		for (Block b : Block.values()) {
			for(int nr = 1; nr <=4 ; nr ++) {
				simple.addElement(new BlockPos(b,nr).toString());
			}
		}
		
		for (Block b : Block.values()) {
			simple.setImmediatelyGreater(new BlockPos(b,1).toString(), new BlockPos(b,3).toString());
			simple.setImmediatelyGreater(new BlockPos(b,2).toString(), new BlockPos(b,3).toString());
			simple.setImmediatelyGreater(new BlockPos(b,2).toString(), new BlockPos(b,4).toString());
			simple.setImmediatelyGreater(new BlockPos(b,1).toString(), new BlockPos(b,4).toString());
		}
		
		for (Block u : new Block[] {Block.UL,Block.UR} ) {
			for (Block l : new Block[] {Block.LL,Block.LR} ) {
				simple.setImmediatelyLower(new BlockPos(u,1).toString(), new BlockPos(l,3).toString());
				simple.setImmediatelyLower(new BlockPos(u,2).toString(), new BlockPos(l,3).toString());
				simple.setImmediatelyLower(new BlockPos(u,2).toString(), new BlockPos(l,4).toString());
				simple.setImmediatelyLower(new BlockPos(u,1).toString(), new BlockPos(l,4).toString());
			}
		}

		assertEquals(16, simple.getElements().size());
		IEditableLattice<String> completion = LatticeUtil.dedekindMcNeilleCompletion(simple);
		assertEquals(16 + 7, completion.getElements().size());
		assertNull(LatticeValidator.validateIncremental(completion));

		String top = completion.getTop();
		String bot = completion.getBottom();
		assertTrue(top.startsWith("newElement-"));
		assertTrue(top.startsWith("newElement-"));
		assertNotEquals(top, bot);
		
	}

	
	public static void main(String[] args) {
		new DedekindMcNeilleCompletionTest().testWiki();
	}
}
