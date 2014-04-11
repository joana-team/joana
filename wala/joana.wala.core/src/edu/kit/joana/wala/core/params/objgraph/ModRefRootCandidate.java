/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.params.objgraph;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.OrdinalSet;

import edu.kit.joana.ifc.sdg.util.BytecodeLocation;
import edu.kit.joana.wala.core.PDGField;
import edu.kit.joana.wala.core.PDGNode;
import edu.kit.joana.wala.core.params.objgraph.TVL.V;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
public abstract class ModRefRootCandidate extends ModRefCandidate {

	private final OrdinalSet<InstanceKey> pts;

	private ModRefRootCandidate(final boolean isMod, final boolean isRef, final OrdinalSet<InstanceKey> pts) {
		super(isMod, isRef);
		this.pts = pts;
	}

	public abstract PDGNode getNode();
	public abstract boolean isException();

	public static ModRefRootCandidate createMod(final PDGField f, final OrdinalSet<InstanceKey> pts) {
		final ModRefRootCandidate mref = new ModRefFieldRootCandidate(true, false, f, pts);

		return mref;
	}

	public static ModRefRootCandidate createRef(final PDGField f, final OrdinalSet<InstanceKey> pts) {
		final ModRefRootCandidate mref = new ModRefFieldRootCandidate(false, true, f, pts);

		return mref;
	}

	public static ModRefRootCandidate createMod(final PDGNode n, final OrdinalSet<InstanceKey> pts) {
		final ModRefRootCandidate mref = new ModRefNodeRootCandidate(true, false, n, pts);

		return mref;
	}

	public static ModRefRootCandidate createRef(final PDGNode n, final OrdinalSet<InstanceKey> pts) {
		final ModRefRootCandidate mref = new ModRefNodeRootCandidate(false, true, n, pts);

		return mref;
	}

	private static final class ModRefNodeRootCandidate extends ModRefRootCandidate {

		private final PDGNode n;

		private ModRefNodeRootCandidate(final boolean isMod, final boolean isRef, final PDGNode n,
				final OrdinalSet<InstanceKey> pts) {
			super(isMod, isRef, pts);

			if (n == null) {
				throw new IllegalArgumentException();
			}

			this.n = n;
		}

		@Override
		public PDGNode getNode() {
			return n;
		}

		@Override
		public boolean isException() {
			return BytecodeLocation.EXCEPTION_PARAM.equals(n.getBytecodeName());
		}

		@Override
		public V isStatic() {
			return V.NO;
		}

		@Override
		public V isPrimitive() {
			return (n.getTypeRef().isPrimitiveType() ? V.YES : V.NO);
		}

		@Override
		public TypeReference getType() {
			return n.getTypeRef();
		}

		@Override
		public int getBytecodeIndex() {
			return n.getBytecodeIndex();
		}

		@Override
		public String getBytecodeName() {
			return n.getBytecodeName();
		}
		
		@Override
		public int hashCode() {
			return n.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof ModRefNodeRootCandidate) {
				final ModRefNodeRootCandidate other = (ModRefNodeRootCandidate) obj;

				return n.equals(other.n);
			}

			return false;
		}

	}

	private static final class ModRefFieldRootCandidate extends ModRefRootCandidate {

		private final PDGField f;

		private ModRefFieldRootCandidate(final boolean isMod, final boolean isRef, final PDGField f,
				final OrdinalSet<InstanceKey> pts) {
			super(isMod, isRef, pts);

			if (f == null) {
				throw new IllegalArgumentException();
			}

			this.f = f;
		}

		@Override
		public PDGNode getNode() {
			return f.node;
		}

		@Override
		public boolean isException() {
			return false;
		}

		@Override
		public V isStatic() {
			return (f.field.isStatic() ? V.YES : V.NO);
		}

		@Override
		public V isPrimitive() {
			return (f.field.isPrimitiveType() ? V.YES : V.NO);
		}

		@Override
		public TypeReference getType() {
			return f.field.getType();
		}

		@Override
		public int getBytecodeIndex() {
			return f.node.getBytecodeIndex();
		}

		@Override
		public String getBytecodeName() {
			return f.node.getBytecodeName();
		}

		@Override
		public int hashCode() {
			return f.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}

			if (obj instanceof ModRefFieldRootCandidate) {
				final ModRefFieldRootCandidate other = (ModRefFieldRootCandidate) obj;

				return f.equals(other.f);
			}

			return false;
		}

	}

	@Override
	public V isRoot() {
		return V.YES;
	}

	@Override
	public boolean isPotentialParentOf(final ModRefFieldCandidate other) {
		return isPrimitive() != V.YES && pts != null && other.isBaseAliased(pts);
	}

}
