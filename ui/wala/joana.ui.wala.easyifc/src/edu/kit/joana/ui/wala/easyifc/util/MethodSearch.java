/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ui.wala.easyifc.util;

import java.util.LinkedList;
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
import org.eclipse.jdt.internal.core.SourceRefElement;
import org.eclipse.jdt.internal.core.SourceType;

import edu.kit.joana.api.sdg.SDGFormalParameter;
import edu.kit.joana.api.sdg.SDGMethod;
import edu.kit.joana.ifc.sdg.util.JavaType;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.FlowStmtResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.FlowStmtResultPart;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.IFCResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.MethodResult;
import edu.kit.joana.ui.wala.easyifc.model.IFCCheckResultConsumer.SPos;
import edu.kit.joana.wala.core.SDGBuilder.ExceptionAnalysis;
import edu.kit.joana.wala.flowless.spec.FlowLessBuilder.FlowError;
import edu.kit.joana.wala.flowless.spec.ast.IFCStmt;

/**
 *
 * @author Juergen Graf <juergen.graf@gmail.com>
 *
 */
@SuppressWarnings("restriction")
public class MethodSearch {

	private final IJavaProject jp;
	private final IFCResult ifcresult;
	private final SourceMethod jMethod;

	private MethodSearch(final IJavaProject jp, final IFCResult ifcresult, final SourceMethod jMethod) {
		this.jp = jp;
		this.ifcresult = ifcresult;
		this.jMethod = jMethod;
	}

	public static IMarker searchPosition(final IJavaProject jp, final SPos spos) {
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {jp});
		final String classOfFile = spos.sourceFile.substring(0, spos.sourceFile.lastIndexOf('.')).replace('/', '.').replace('$', '.');
		
		final SearchPattern pat = SearchPattern.createPattern(classOfFile,
				IJavaSearchConstants.TYPE, IJavaSearchConstants.DECLARATIONS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_FULL_MATCH);
		final SearchEngine eng = new SearchEngine();
		final SearchParticipant[] defaultParts =
				new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		final LinkedList<SourceType> found = new LinkedList<SourceType>();
		final SearchRequestor req = new SearchRequestor() {
			@Override
			public void acceptSearchMatch(final SearchMatch match) throws CoreException {
				if (match.getElement() instanceof SourceType) {
					final SourceType t = (SourceType) match.getElement();
					if (classOfFile.equals(t.getFullyQualifiedName('.'))) {
						found.add(t);
					}
				}
			}
		};
		
		try {
			eng.search(pat, defaultParts, scope, req, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (!found.isEmpty()) {
			final SourceType t =  found.element();
			final IResource res = t.getResource();
			
			return CheckFlowMarkerAndImageManager.getInstance().createMarkerInferredOk(res, "foo", spos.startLine);
		} else {
			return null;
		}
	}
	
	private static String searchPattern(final IFCResult ifcres) {
		final String m = ifcres.getMainMethod();

		final String clsName = m.substring(0, m.lastIndexOf('.')).replace('$', '.');
		final String methodName = m.substring(m.lastIndexOf('.') + 1, m.indexOf('('));
		
		return clsName + "." + methodName;
	}

//	private static String searchPattern(final MethodResult mres) {
//		final MethodInfo nfo = mres.getInfo();
//
//		return nfo.getClassInfo().getName() + "." + nfo.getName();
//	}

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
//		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {jp});
//		final String mpat = searchPattern(mres);
//		System.out.println("Searching for " + mpat);
//		final SearchPattern pat = SearchPattern.createPattern(mpat,
//				IJavaSearchConstants.METHOD, 1, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_FULL_MATCH);
//		final SearchEngine eng = new SearchEngine();
//		final SearchParticipant[] defaultParts =
//				new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
//		final Requestor req = new Requestor(mres.getInfo());
//		try {
//			eng.search(pat, defaultParts, scope, req, null);
//		} catch (CoreException e) {
//			e.printStackTrace();
//		}
//
//		if (req.methodFound()) {
//			return new MethodSearch(jp, mres, req.getMethod());
//		} else {
			return null;
//		}
	}

	public static MethodSearch searchMethod(final IJavaProject jp, final IFCResult ifcres) {
		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {jp});
		
		final String mpat = searchPattern(ifcres);
		System.out.println("Searching for " + mpat);
		final SearchPattern pat = SearchPattern.createPattern(mpat,
				IJavaSearchConstants.METHOD, IJavaSearchConstants.IMPLEMENTORS, SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_FULL_MATCH);
		final SearchEngine eng = new SearchEngine();
		final SearchParticipant[] defaultParts =
				new SearchParticipant[] {SearchEngine.getDefaultSearchParticipant()};
		final Requestor req = new Requestor(ifcres);
		try {
			eng.search(pat, defaultParts, scope, req, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}

		if (req.methodFound()) {
			return new MethodSearch(jp, ifcres, req.getMethod());
		} else {
			return null;
		}
	}

	
	private static final class Requestor extends SearchRequestor {

		private final IFCResult nfo;
		private SourceMethod found = null;

		private Requestor(final IFCResult nfo) {
			this.nfo = nfo;
		}

		private boolean isBetterResult(final SourceMethod sm) {
			final String[] newParams = sm.getParameterTypes();
			final String[] oldParams = found.getParameterTypes();
			final SDGMethod im = nfo.getMainIMethod();

			final int numParam;
			if (im.parameterIndexValid(0)) {
				// dynamic method with this pointer
				numParam = im.getParameters().size() - 1;
			} else {
				numParam = im.getParameters().size();
			}
			
			if (numParam != newParams.length) {
				return false;
			}
			
			if (newParams.length != oldParams.length) {
				return true;
			}
			
			int newMatch = 0;
			int oldMatch = 0;

			for (int i = 0; i < sm.getNumberOfParameters(); i++) {
				
				final SDGFormalParameter tref = im.getParameter(i + 1);
				if (isMatching(tref,oldParams[i])) {
					oldMatch++;
				}
				if (isMatching(tref,newParams[i])) {
					newMatch++;
				}
			}

			if (newMatch != oldMatch) {
				return (newMatch > oldMatch);
			}

			return false;
		}

		private static boolean isMatching(final SDGFormalParameter tref, final String eType) {
			final JavaType jt = tref.getType();
			jt.toBCString();
			
			//TODO
			
			return true;
		}
		
		@Override
		public void acceptSearchMatch(final SearchMatch match) throws CoreException {
			if (match.getElement() instanceof SourceMethod) {
				final SourceMethod sm = (SourceMethod) match.getElement();
				final SDGMethod im = nfo.getMainIMethod();
				final int paramCount = 
						im.instructionIndexValid(0) ? im.getParameters().size() : im.getParameters().size() - 1;
						
				final String name = im.getSignature().getMethodName(); 
				if (sm.getElementName().equals(name) && sm.getParameterNames().length == paramCount) {
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
