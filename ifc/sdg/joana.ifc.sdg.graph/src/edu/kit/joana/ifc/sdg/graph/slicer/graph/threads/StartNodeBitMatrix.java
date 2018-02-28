/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.graph.slicer.graph.threads;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.util.Pair;

/**
 * TODO: @author Add your name here.
 */
public class StartNodeBitMatrix implements IBitMatrix<ThreadRegion> {

	
	private final SymmetricBitMatrix<Pair<SDGNode, Boolean>> startNodesMatrix;
	private final Map<Pair<SDGNode, Boolean>, Integer> startNodesToNumber;
	private final Map<Integer, MutableIntSet> numberToRegions;
	private final ThreadRegions threadRegions;

	
	public StartNodeBitMatrix(
			SymmetricBitMatrix<Pair<SDGNode, Boolean>> startNodesMatrix,
			final Map<Pair<SDGNode, Boolean>, Integer> startNodesToNumber,
			Map<Integer, MutableIntSet> numberToRegions,
			ThreadRegions threadRegions) {
		this.startNodesMatrix = startNodesMatrix;
		this.startNodesToNumber = startNodesToNumber;
		this.numberToRegions = numberToRegions;
		this.threadRegions = threadRegions;
		
	}
	
	@Override
	public boolean get(int i, int j) {
		final ThreadRegion regionI = threadRegions.getThreadRegion(i);
		final ThreadRegion regionJ = threadRegions.getThreadRegion(j);
		
		final SDGNode startI = regionI.getStart();
		final SDGNode startJ = regionJ.getStart();
		
		final boolean isDynamicI = regionI.isDynamic();
		final boolean isDynamicJ = regionJ.isDynamic();
		
		return startNodesMatrix.get(
			startNodesToNumber.get(Pair.pair(startI, isDynamicI)),
			startNodesToNumber.get(Pair.pair(startJ, isDynamicJ))
		);
	}

	@Override
	public int getDimension() {
		return threadRegions.size();
	}
	
	@Override
	public IntIterator onCol(int j) {
		final ThreadRegion regionJ = threadRegions.getThreadRegion(j);
		final SDGNode startJ = regionJ.getStart();
		final boolean isDynamicJ = regionJ.isDynamic();
		final IntIterator rowIterator = startNodesMatrix.onCol(startNodesToNumber.get(Pair.pair(startJ, isDynamicJ)));
		
		if (!rowIterator.hasNext()) {
			return new IntIterator() {
				public int next() { 
					throw new NoSuchElementException();
				}
				
				@Override
				public boolean hasNext() {
					return false;
				}
			};
		}
		
		return new IntIterator() {
			
			int i = rowIterator.next();
			IntIterator regionIterator = numberToRegions.get(i).intIterator();
			
			private boolean findNext() {
				if (regionIterator == null) return false;
				if (regionIterator.hasNext()) return true;
				
				if (rowIterator.hasNext()) {
					i = rowIterator.next();
					regionIterator = numberToRegions.get(i).intIterator();
					return true;
				} else {
					regionIterator = null;
					return false;
				}
			}
			
			@Override
			public int next() {
				if (!findNext()) throw new NoSuchElementException();
				return regionIterator.next();
			}
			
			@Override
			public boolean hasNext() {
				return findNext();
			}
		};
	}
	
	SymmetricBitMatrix<Pair<SDGNode, Boolean>> getStartNodesMatrix() {
		return startNodesMatrix;
	}
	
	Map<Pair<SDGNode, Boolean>, Integer> getStartNodesToNumber() {
		return startNodesToNumber;
	}
}

