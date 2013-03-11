/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.flowless.util;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.SourceMethod;

import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResult;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.wala.dictionary.accesspath.FlowCheckResultConsumer.MethodResult;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo;
import edu.kit.joana.wala.flowless.spec.java.ast.MethodInfo.ParamInfo;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class MethodSearch {

//	private final IJavaProject jp;
//	private final MethodResult m;
	private final SourceMethod jMethod;

	private MethodSearch(final IJavaProject jp, final MethodResult m, final SourceMethod jMethod) {
//		this.jp = jp;
//		this.m = m;
		this.jMethod = jMethod;
	}

	private static String searchPattern(final MethodResult mres) {
		final MethodInfo nfo = mres.getInfo();

		return nfo.getClassInfo().getName() + "." + nfo.getName();
	}

	public SourceMethod getMethod() {
		return jMethod;
	}

	public IMarker findIFCStmt(final IFCStmt stmt) {
		try {
			IMarker m = jMethod.getResource().createMarker(IMarker.TEXT);
			m.setAttribute(IMarker.LINE_NUMBER, new Integer(stmt.getLineNr()));
			return m;
		} catch (CoreException e) {
		}

		return null;
	}

	public IMarker findFlowError(final FlowError ferr) {
		try {
			IMarker m = jMethod.getResource().createMarker(IMarker.TEXT);
			m.setAttribute(IMarker.LINE_NUMBER, new Integer(ferr.lineNr));
			return m;
		} catch (CoreException e) {
		}

		return null;
	}

	public IMarker makeSideMarker(final FlowError ferr) {
		final IResource res = jMethod.getResource();
		return CheckFlowMarkerAndImageManager.getInstance().createMarkerInferredOk(res, ferr.exc.getMessage(),
				ferr.lineNr);
	}

	public IMarker makeSideMarker(final FlowStmtResult stmt) {
		final IResource res = jMethod.getResource();
		final int lineNr = stmt.getStmt().getLineNr();

		if (stmt.isAlwaysSatisfied()) {
			final String msg = "'" + stmt.getStmt().toString() + "' is always satisfied.";
			return CheckFlowMarkerAndImageManager.getInstance().createMarkerOk(res, msg, lineNr);
		} else if (stmt.isNeverSatisfied()) {
			final String msg = "'" + stmt.getStmt().toString() + "' could not be validated.";
			return CheckFlowMarkerAndImageManager.getInstance().createMarkerIllegal(res, msg, lineNr);
		} else if (stmt.isNoExceptionSatisfied()) {
			final String msg = "'" + stmt.getStmt().toString() + "' is not satisfied in general, "
					+ "but it has been validated without the effect of control flow through exceptions.";
			return CheckFlowMarkerAndImageManager.getInstance().createMarkerNoExcOk(res, msg, lineNr);
		} else if (stmt.isInferredSatisfied()) {
			String desc = "";
			for (final FlowStmtResultPart part : stmt.getParts()) {
				if (part.isInferred() && part.isSatisfied() && part.getExceptionConfig() != ExceptionAnalysis.IGNORE_ALL) {
					desc += (desc.length() > 0 ? " | " + part.getDescription() : part.getDescription());
				}
			}
			final String msg = "'" + stmt.getStmt().toString()
					+ "' is always satisfied under the inferred context: " + desc;
			return CheckFlowMarkerAndImageManager.getInstance().createMarkerInferredOk(res, msg, lineNr);
		} else if (stmt.isInferredNoExcSatisfied()) {
			String desc = "";
			for (final FlowStmtResultPart part : stmt.getParts()) {
				if (part.isInferred() && part.isSatisfied()) {
					desc += (desc.length() > 0 ? " | " + part.getDescription() : part.getDescription());
				}
			}
			final String msg = "'" + stmt.getStmt().toString() + "' is not satisfied in general, but it has been "
					+ "validated without the effect of control flow through exceptions and "
					+ "under the inferred context: " + desc;
			return CheckFlowMarkerAndImageManager.getInstance().createMarkerInferredNoExcOk(res, msg, lineNr);
		} else {
			final String msg = "'" + stmt.getStmt().toString() + "' could not be validated.";
			return CheckFlowMarkerAndImageManager.getInstance().createMarkerIllegal(res, msg, lineNr);
		}
	}

	public ILocalVariable findParam(final int num) {
		if (num >= 0 && jMethod.getNumberOfParameters() < num) {
			try {
				return jMethod.getParameters()[num];
			} catch (JavaModelException e) {
				return null;
			}
		}

		return null;
	}

	public static MethodSearch searchMethod(final IJavaProject jp, final MethodResult mres) {
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {jp});
		final String mpat = searchPattern(mres);
		System.out.println("Searching for " + mpat);
		final SearchPattern pat = SearchPattern.createPattern(mpat,
				IJavaSearchConstants.METHOD, 1, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_FULL_MATCH);
		final SearchEngine eng = new SearchEngine();
		final SearchParticipant[] defaultParts =
				new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		final Requestor req = new Requestor(mres.getInfo());
		try {
			eng.search(pat, defaultParts, scope, req, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (req.methodFound()) {
			return new MethodSearch(jp, mres, req.getMethod());
		} else {
			return null;
		}
	}

	private static final class Requestor extends SearchRequestor {

		private final MethodInfo nfo;
		private SourceMethod found = null;

		private Requestor(final MethodInfo nfo) {
			this.nfo = nfo;
		}

		private boolean isBetterResult(final SourceMethod sm) {
			try {
				final String newDoc = sm.getAttachedJavadoc(null);
				final String oldDoc = found.getAttachedJavadoc(null);

				if (oldDoc.contains("@ifc:") && !newDoc.contains("@ifc:")) {
					return false;
				} else if (newDoc.contains("@ifc:") && !oldDoc.contains("@ifc:")) {
					return true;
				}
			} catch (JavaModelException e) {}

			final String[] newParams = sm.getParameterTypes();
			final String[] oldParams = found.getParameterTypes();
			final List<ParamInfo> ps = nfo.getParameters();
			if (!nfo.isStatic()) {
				// remove this pointer before check
				ps.remove(0);
			}

			int newMatch = 0;
			int oldMatch = 0;

			for (int i = 0; i < sm.getNumberOfParameters(); i++) {
				final ParamInfo pi = ps.get(i);
				if (pi.type.equals(oldParams[i])) {
					oldMatch++;
				}
				if (pi.type.equals(newParams[i])) {
					newMatch++;
				}
			}

			if (newMatch != oldMatch) {
				return (newMatch > oldMatch);
			}

			return false;
		}

		@Override
		public void acceptSearchMatch(final SearchMatch match) throws CoreException {
			if (match.getElement() instanceof SourceMethod) {
				final SourceMethod sm = (SourceMethod) match.getElement();
				final int paramCount = nfo.isStatic() ? nfo.getParameters().size() : nfo.getParameters().size() - 1;
				if (sm.getElementName().equals(nfo.getName()) && sm.getParameterNames().length == paramCount) {
					if (found == null || isBetterResult(sm)) {
						found = sm;
					}
				}
			}
		}

		public SourceMethod getMethod() {
			return found;
		}

		public boolean methodFound() {
			return found != null;
		}
	}

}
