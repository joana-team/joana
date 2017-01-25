/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.accesspath;

import java.util.LinkedList;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

import edu.kit.joana.ifc.sdg.graph.SDGNode;
import edu.kit.joana.wala.core.accesspath.APIntraProc.MergeOp;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.TIntSet;

/**
 * Result of the accesspath and merge info computation. Contains info for each PDG in the SDG.
 * 
 * @author Juergen Graf <juergen.graf@gmail.com>
 */
public class APResult {
	
	private final TIntObjectMap<APIntraprocContextManager> pdgId2ctx = new TIntObjectHashMap<>();
	private int numOfAliasEdges = 0;
	private final int rootPdgId;
	private final IClassHierarchy cha;
	
	public APResult(final int rootPdgId, final IClassHierarchy cha) {
		this.rootPdgId = rootPdgId;
		this.cha = cha;
	}
	
	void add(final APIntraprocContextManager ctx, final int numOfCurAliasEdges) {
		pdgId2ctx.put(ctx.getPdgId(), ctx);
		numOfAliasEdges += numOfCurAliasEdges;
	}
	
	public APContextManagerView get(final int pdgId) {
		return pdgId2ctx.get(pdgId);
	}
	
	public APContextManagerView getRoot() {
		return get(rootPdgId);
	}

	private final ClassLoaderReference findClassLoader(final SDGNode n) {
		for (final IClassLoader cl : cha.getLoaders()) {
			final ClassLoaderReference clr = cl.getReference();
			if (clr.toString().equals(n.getClassLoader())) {
				return clr;
			}
		}
		
		// default to application loader if nothing is provided
		return ClassLoaderReference.Application;
	}
	
	private TypeReference findType(final SDGNode node) {
		final ClassLoaderReference clr = findClassLoader(node);
		final String typeName = node.getType();
		return TypeReference.find(clr, typeName);
	}
	
	public boolean typesMayAlias(final SDGNode n1, final SDGNode n2) {
		final TypeReference t1 = findType(n1);
		final TypeReference t2 = findType(n2);
		if (t1.isReferenceType() && t2.isReferenceType()) {
			if (t1.isArrayType() && t2.isArrayType()) {
				if (t1.getDimensionality() == t2.getDimensionality()) {
					final TypeReference int1 = t1.getInnermostElementType();
					final TypeReference int2 = t2.getInnermostElementType();
					
					return typesMayAlias(int1, int2);
				} else {
					return false;
				}
			} else if (!t1.isArrayType() && !t2.isArrayType()) {
				return typesMayAlias(t1, t2);
			} else {
				return false;
			}
		}
		
		return t1.equals(t2);
	}
	
	private boolean typesMayAlias(final TypeReference t1, final TypeReference t2) {
		if (t1.equals(t2)) {
			return true;
		} else if (t1.isReferenceType() && t2.isReferenceType()) {
			final IClass c1 = cha.lookupClass(t1);
			final IClass c2 = cha.lookupClass(t2);
			
			return cha.isAssignableFrom(c1, c2) || cha.isAssignableFrom(c2, c1);
		}
		
		return false;
	}
	
	public int getNumOfAliasEdges() {
		return numOfAliasEdges;
	}

	public boolean propagateMaxInitialContextToCalls(final int startId) {
		final APIntraprocContextManager root = pdgId2ctx.get(startId);
		final Set<MergeOp> initial = root.computeMaxInitialContext();
		root.setInitialAlias(initial);

		return propagateInitialContextToCalls(root);
	}
	
	public boolean propagateMinInitialContextToCalls(final int startId) {
		final APIntraprocContextManager root = pdgId2ctx.get(startId);
		final Set<MergeOp> initial = root.computeMinInitialContext();
		root.setInitialAlias(initial);
		
		return propagateInitialContextToCalls(root);
	}
	
	private boolean propagateInitialContextToCalls(final APIntraprocContextManager root) {
		boolean changed = false;
		final LinkedList<APIntraprocContextManager> work = new LinkedList<>();
		work.add(root);
		
		while (!work.isEmpty()) {
			final APIntraprocContextManager cur = work.removeLast();
			final TIntSet called = cur.getCalledMethods();
			final TIntIterator it = called.iterator();
			
			while (it.hasNext()) {
				final int curM = it.next();
				final APIntraprocContextManager callee = pdgId2ctx.get(curM);
				final APContext ctxCallee = cur.computeContextForAllCallsTo(callee);
				
				if (ctxCallee != null) {
					callee.setInitialAlias(ctxCallee);
					changed = true;
					if (!work.contains(callee)) {
						work.add(callee);
					}
				}
			}
		}

		return changed;
	}

	public void reset() {
		for (final APIntraprocContextManager ctx : pdgId2ctx.valueCollection()) {
			ctx.reset();
		}
	}
}
