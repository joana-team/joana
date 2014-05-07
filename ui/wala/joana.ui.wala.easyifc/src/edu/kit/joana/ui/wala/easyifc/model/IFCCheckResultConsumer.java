/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;

public interface IFCCheckResultConsumer {

	/**
	 * Handle flow check results.
	 * @param mres flow check results.
	 */
	public void consume(MethodResult mres);

	public static IFCCheckResultConsumer DEFAULT = new IFCCheckResultConsumer() {

		@Override
		public void consume(final MethodResult mres) {
			// do nothing
		}
	};

	public static IFCCheckResultConsumer STDOUT = new IFCCheckResultConsumer() {

		@Override
		public void consume(final MethodResult mres) {
			System.out.println(mres);
		}
	};

	public final class MethodResult {
		private final MethodInfo mInfo;
		private final String tmpDir;
		private final Map<IFCStmt, FlowStmtResult> stmts = new HashMap<IFCStmt, FlowStmtResult>();

		public MethodResult(final MethodInfo mInfo, final String tmpDir) {
			if (mInfo == null) {
				throw new IllegalArgumentException();
			}

			this.mInfo = mInfo;
			this.tmpDir = tmpDir;
		}

		public String getTmpDir() {
			return tmpDir;
		}

		public MethodInfo getInfo() {
			return mInfo;
		}

		public FlowStmtResult findOrCreateStmtResult(final IFCStmt ifcst) {
			if (!stmts.containsKey(ifcst)) {
				final FlowStmtResult fsr = new FlowStmtResult(ifcst);
				stmts.put(ifcst, fsr);

				return fsr;
			}

			return stmts.get(ifcst);
 		}

		public boolean hasErrors() {
			return mInfo.hasErrors();
		}

		public List<FlowError> getErrors() {
			return mInfo.getErrors();
		}

		public List<FlowStmtResult> getStmtResults() {
			return Collections.unmodifiableList(new LinkedList<FlowStmtResult>(stmts.values()));
		}

		public FlowStmtResult getStmtResult(final IFCStmt stmt) {
			return stmts.get(stmt);
		}

		public FlowStmtResult getStmtResult(final int index) {
			final List<IFCStmt> ifcStmts = mInfo.getIFCStmts();
			if (index < 0 || index >= ifcStmts.size()) {
				throw new IllegalArgumentException("index out of range: " + index);
			}

			final IFCStmt ifcst = ifcStmts.get(index);

			return getStmtResult(ifcst);
		}

		public String toString() {
			final StringBuilder sb = new StringBuilder();

			sb.append("ifc check results '" + mInfo + "'\n");

			if (hasErrors()) {
				for (final FlowError err : getErrors()) {
					sb.append("\tERROR: "+ err.exc.getMessage() + "\n");
				}
			} else {
				for (final FlowStmtResult stres : stmts.values()) {
					sb.append(stres.toString());
				}
			}

			return sb.toString();
		}

		public boolean isAllValid() {
			boolean allValid = true;

			for (final FlowStmtResult res : getStmtResults()) {
				allValid &= res.isAlwaysSatisfied();
			}

			return allValid;
		}
	}

	public final class FlowStmtResult {
		private final IFCStmt stmt;
		private final List<FlowStmtResultPart> parts = new LinkedList<FlowStmtResultPart>();

		private FlowStmtResult(final IFCStmt stmt) {
			if (stmt == null) {
				throw new IllegalArgumentException();
			}

			this.stmt = stmt;
		}

		public IFCStmt getStmt() {
			return stmt;
		}

		public String toString() {
			final StringBuilder sb = new StringBuilder();

			sb.append("check '" + stmt + "'\n");

			for (final FlowStmtResultPart part : parts) {
				sb.append("\t" + part + "\n");
			}

			return sb.toString();
		}

		public void addPart(final FlowStmtResultPart part) {
			if (part == null) {
				throw new IllegalArgumentException();
			}

			this.parts.add(part);
		}

		public List<FlowStmtResultPart> getParts() {
			return Collections.unmodifiableList(parts);
		}

		public boolean containsInferredParts() {
			for (final FlowStmtResultPart part : parts) {
				if (part.isInferred()) {
					return true;
				}
			}

			return false;
		}

		public boolean isInferredSatisfied() {
			boolean ok = containsInferredParts();

			for (final FlowStmtResultPart part : parts) {
				if (part.isInferred()) {
					ok &= part.isSatisfied();
				}
			}

			return ok;
		}

		public boolean isInferredNoExcSatisfied() {
			boolean ok = containsInferredParts();

			for (final FlowStmtResultPart part : parts) {
				if (part.isInferred() && part.getExceptionConfig() == ExceptionAnalysis.IGNORE_ALL) {
					ok &= part.isSatisfied();
				}
			}

			return ok;
		}

		public boolean isNoExceptionSatisfied() {
			boolean ok = true;

			for (final FlowStmtResultPart part : parts) {
				if (part.getExceptionConfig() == ExceptionAnalysis.IGNORE_ALL) {
					ok &= part.isSatisfied();
				}
			}

			return ok;
		}

		public boolean isAlwaysSatisfied() {
			boolean satisfied = true;

			for (final FlowStmtResultPart part : parts) {
				satisfied &= part.isSatisfied();
			}

			return satisfied;
		}

		public boolean isNeverSatisfied() {
			boolean satisfied = false;

			for (final FlowStmtResultPart part : parts) {
				satisfied |= part.isSatisfied();
			}

			return !satisfied;
		}

		public boolean isSometimesSatisfied() {
			return !isAlwaysSatisfied() && !isNeverSatisfied();
		}

	}

	public final class FlowStmtResultPart {
		private final String desc;
		private final String sdgFilename;
		private final BasicIFCStmt bifc;
		private final boolean statisfied;
		private final boolean inferred;
		private final ExceptionAnalysis excConfig;
		private Alias alias;

		public FlowStmtResultPart(final BasicIFCStmt bifc, final String desc, final boolean isSatisfied,
				final boolean isInferred, final ExceptionAnalysis excConfig, final String sdgFilename) {
			if (desc == null) {
				throw new IllegalArgumentException();
			} else if (excConfig == null) {
				throw new IllegalArgumentException();
			}

			this.bifc = bifc;
			this.desc = desc;
			this.statisfied = isSatisfied;
			this.inferred = isInferred;
			this.excConfig = excConfig;
			this.sdgFilename = sdgFilename;
		}

		public void setAlias(final Alias alias) {
			this.alias = alias;
		}

		public boolean hasAlias() {
			return this.alias != null;
		}

		public Alias getAlias() {
			return alias;
		}

		public String getSDGFilename() {
			return sdgFilename;
		}

		public boolean hasPathToSDG() {
			return false;
		}

		public BasicIFCStmt getBasicStmt() {
			return bifc;
		}

		public boolean isInferred() {
			return inferred;
		}

		public boolean isSatisfied() {
			return statisfied;
		}

		public String getDescription() {
			return desc;
		}

		public ExceptionAnalysis getExceptionConfig() {
			return excConfig;
		}

		public String toString() {
			final StringBuffer sb = new StringBuffer();
			sb.append(statisfied ? "FLOW OK(" : "ILLEGAL(");
			switch (excConfig) {
			case ALL_NO_ANALYSIS:
//				sb.append("imprecise exceptions)                : ");
				sb.append("with exceptions)    : ");
				break;
			case IGNORE_ALL:
//				sb.append("ignoring exceptions)                 : ");
				sb.append("ignoring exceptions): ");
				break;
			case INTERPROC:
//				sb.append("interprocedural optimized exceptions): ");
				sb.append("with exceptions)    : ");
				break;
			case INTRAPROC:
//				sb.append("intraprocedural optimized exceptions): ");
				sb.append("with exceptions)    : ");
				break;
			}

			if (inferred) {
				sb.append("inferred: ");
			}

			sb.append(desc);

			return sb.toString();
		}
	}
}
