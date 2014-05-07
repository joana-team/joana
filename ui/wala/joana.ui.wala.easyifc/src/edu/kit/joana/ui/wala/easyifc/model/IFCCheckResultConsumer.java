/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.FlowLessSimplifier.BasicIFCStmt;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;

public interface IFCCheckResultConsumer {

	/**
	 * Handle information flow check results.
	 * @param ifcres information flow check results.
	 */
	public void consume(IFCResult ifcres);
	
	public static IFCCheckResultConsumer DEFAULT = new IFCCheckResultConsumer() {

		@Override
		public void consume(final IFCResult ifcres) {
			// do nothing
		}
	};

	public static IFCCheckResultConsumer STDOUT = new IFCCheckResultConsumer() {

		@Override
		public void consume(final IFCResult ifcres) {
			System.out.println(ifcres);
		}
	};
	
	public final class IFCResult {
		
		private final String tmpDir;
		
		private final SortedSet<SLeak> excLeaks = new TreeSet<SLeak>();  
		private final SortedSet<SLeak> noExcLeaks = new TreeSet<SLeak>();
		
		public IFCResult(final String tmpDir) {
			this.tmpDir = tmpDir;
		}
		
		public boolean hasLeaks() {
			return !(excLeaks.isEmpty() && noExcLeaks.isEmpty());
		}
		
		public String getTmpDir() {
			return tmpDir;
		}
		
		public void addExcLeak(final SLeak leak) {
			excLeaks.add(leak);
		}

		public void addNoExcLeak(final SLeak leak) {
			noExcLeaks.add(leak);
		}
		
		public String toString() {
			final int excl = excLeaks.size();
			final int total = excl + noExcLeaks.size();
			
			if (total > 0) {
				return "UNSAFE: " + total + " leak" + (total > 1 ? "s" : "") + " found."
						+ (excLeaks.size() > 0 ? " " + excLeaks.size() + " due to implicit flow caused by exceptions." : "");
			} else {
				return "SECURE.";
			}
		}
		
		public SortedSet<SLeak> getNoExcLeaks() {
			return Collections.unmodifiableSortedSet(noExcLeaks);
		}

		public SortedSet<SLeak> getExcLeaks() {
			return Collections.unmodifiableSortedSet(excLeaks);
		}
	}

	public static enum Reason { 
		DIRECT_FLOW(1), INDIRECT_FLOW(2), BOTH_FLOW(3), EXCEPTION(4), THREAD(5), THREAD_EXCEPTION(6);

		public final int importance;
	
		private Reason(final int importance) {
			this.importance = importance;
		}
	
	}

	public static class SLeak implements Comparable<SLeak> {
		private final SPos source;
		private final SPos sink;
		private final Reason reason;
		private final Set<SPos> slice;
		
		public SLeak(final SPos source, final SPos sink, final Reason reason, final Set<SPos> slice) {
			this.source = source;
			this.sink = sink;
			this.reason = reason;
			this.slice = slice;
		}
		
		public Reason getReason() {
			return reason;
		}
		
		public int hashCode() {
			return source.hashCode() + 23 * sink.hashCode();
		}
		
		public boolean equals(Object o) {
			if (o instanceof SLeak) {
				final SLeak l = (SLeak) o;
				return source.equals(l.source) && sink.equals(l.sink);
			}
			
			return false;
		}
		
		public String toString() {
			String info = "";
			switch (reason) {
			case BOTH_FLOW:
				info = "explicit and implicit flow ";
				break;
			case DIRECT_FLOW:
				info = "explicit flow ";
				break;
			case INDIRECT_FLOW:
				info = "implicit flow ";
				break;
			case EXCEPTION:
				info = "flow caused by exceptions ";
				break;
			case THREAD:
				info = "critical thread interference ";
				break;
			case THREAD_EXCEPTION:
				info = "critical thread interference caused by exceptions ";
				break;
			}
			
			return info + "from '" + source.toString() + "' to '" + sink.toString() + "'";
		}
		
		public String toString(final File srcFile) {
			StringBuffer sbuf = new StringBuffer();
			switch (reason) {
			case DIRECT_FLOW:
				sbuf.append("explicit flow:\n");
				break;
			case INDIRECT_FLOW:
				sbuf.append("implicit flow:\n");
				break;
			case BOTH_FLOW:
				sbuf.append("explicit and implicit flow:\n");
				break;
			case EXCEPTION:
				sbuf.append("implicit flow due to exceptions:\n");
				break;
			case THREAD:
				sbuf.append("possibilistic or probabilistic flow:\n");
				break;
			case THREAD_EXCEPTION:
				sbuf.append("possibilistic or probabilistic flow caused by exceptions:\n");
				break;
			default:
				sbuf.append("reason: " + reason + "\n");
			}
			
			sbuf.append("from '" + source.toString() + "' to '" + sink.toString() + "'\n");
			
			for (final SPos pos : slice) {
				sbuf.append(pos.toString() + "\t");
				final String code = pos.getSourceCode(srcFile);
				sbuf.append(code + "\n");
			}
			
			return sbuf.toString();
		}
	
		@Override
		public int compareTo(final SLeak o) {
			if (o == this || this.equals(o)) {
				return 0;
			}
	
			if (!source.equals(o.source)) {
				return source.compareTo(o.source);
			}
			
			if (!sink.equals(o.sink)) {
				return sink.compareTo(o.sink);
			}
			
			return 0;
		}
		
	}
	
	public static class SPos implements Comparable<SPos> {
		private final String sourceFile;
		private final int startChar;
		private final int endChar;
		private final int startLine;
		private final int endLine;
		
		public SPos(final String sourceFile, final int startLine, final int endLine, final int startChar,
				final int endChar) {
			this.sourceFile = sourceFile;
			this.startLine = startLine;
			this.endLine = endLine;
			this.startChar = startChar;
			this.endChar = endChar;
		}
		
		public int hashCode() {
			return sourceFile.hashCode() + 13 * startLine;
		}
		
		public boolean isAllZero() {
			return startLine == 0 && endLine == 0 && startChar == 0 && endChar == 0;
		}
		
		public boolean hasCharPos() {
			return !(startChar == 0 && startChar == endChar);
		}
		
		public boolean isMultipleLines() {
			return startLine != endLine;
		}
		
		public boolean equals(Object o) {
			if (o instanceof SPos) {
				final SPos spos = (SPos) o;
				return sourceFile.equals(spos.sourceFile) && startLine == spos.startLine && endLine == spos.endLine
						&& startChar == spos.startChar && endChar == spos.endChar;
			}
			
			return false;
		}
	
		@Override
		public int compareTo(SPos o) {
			if (sourceFile.compareTo(o.sourceFile) != 0) {
				return sourceFile.compareTo(o.sourceFile);
			}
			
			if (startLine != o.startLine) {
				return startLine - o.startLine;
			}
			
			if (endLine != o.endLine) {
				return endLine - o.endLine;
			}
			
			if (startChar != o.startChar) {
				return startChar - o.startChar;
			}
			
			if (endChar != o.endChar) {
				return endChar - o.endChar;
			}
			
			return 0;
		}
		
		public String toString() {
			if (hasCharPos() && isMultipleLines()) {
				return sourceFile + ":(" + startLine + "," + startChar + ")-(" + endLine + "," + endChar +")"; 
			} else if (hasCharPos()) {
				return sourceFile + ":(" + startLine + "," + startChar + "-" + endChar +")"; 
			} else if (isMultipleLines()) {
				return sourceFile + ":" + startLine + "-" + endLine; 
			} else {
				return sourceFile + ":" + startLine; 
			}
		}
		
		public String getSourceCode(final File sourceFile) {
			final File f = sourceFile;
			try {
				String code = "";
				final BufferedReader read = new BufferedReader(new FileReader(f));
				for (int i = 0; i < startLine-1; i++) {
					read.readLine();
				}
	
				if (!isMultipleLines()) {
					final String line = read.readLine();
					if (hasCharPos()) {
						code = line.substring(startChar, endChar);
					} else {
						code = line;
					}
				} else {
					{
						final String line = read.readLine();
						if (hasCharPos()) {
							code = line.substring(startChar);
						} else {
							code = line;
						}
					}
					
					for (int i = startLine; i < endLine-1; i++) {
						code += read.readLine();
					}
					
					{
						final String line = read.readLine();
						if (hasCharPos()) {
							code += line.substring(0, endChar);
						} else {
							code += line;
						}
					}
				}
	
				read.close();
				
				return code;
			} catch (IOException e) {}
			
			return  "error getting source";
		}
	}

	
	// OLD STUFF BELOW
	
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
