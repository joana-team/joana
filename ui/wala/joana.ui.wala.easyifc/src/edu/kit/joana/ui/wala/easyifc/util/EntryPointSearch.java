package edu.kit.joana.ui.wala.easyifc.util;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMemberValuePair;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.IJavaSearchScope;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchMatch;
import org.eclipse.jdt.core.search.SearchParticipant;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.SearchRequestor;
import org.eclipse.jdt.internal.core.Annotation;
import org.eclipse.jdt.internal.core.SourceMethod;
import org.eclipse.jdt.internal.core.SourceRefElement;

import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.wala.easyifc.model.CheckInformationFlow;
import edu.kit.joana.ui.wala.easyifc.model.CheckInformationFlow.CheckIFCConfig;
import edu.kit.joana.ui.wala.easyifc.util.AnnotationSearch.AnnotationSearchRequestor;

@SuppressWarnings("restriction")
public class EntryPointSearch {

	private static class MainMethodSearchRequestor extends SearchRequestor {
			private List<IMethod> result;

			public MainMethodSearchRequestor() {
				result = new LinkedList<IMethod>();
			}
			
			@Override
			public void acceptSearchMatch(SearchMatch match) throws CoreException {
				IMethod methodElement = (IMethod) match.getElement();
				assert methodElement.isMainMethod();
				result.add(methodElement);
			}
			
			public Collection<IMethod> getResult() {
				return result;
			}
	}

	public static Collection<IMethod> searchMainMethods(IProgressMonitor pm, IJavaSearchScope scope) throws CoreException {
		SearchPattern pattern = SearchPattern.createPattern(
		  "main(String[]) void",
		  IJavaSearchConstants.METHOD,
		  IJavaSearchConstants.DECLARATIONS,
		  SearchPattern.R_EXACT_MATCH | SearchPattern.R_CASE_SENSITIVE);
		MainMethodSearchRequestor requestor = new MainMethodSearchRequestor();
		
		SearchParticipant[] searchParticipants = new SearchParticipant[] { SearchEngine.getDefaultSearchParticipant() };
		new SearchEngine().search(pattern, searchParticipants, scope, requestor, pm);

		return requestor.getResult();
	}
	
	public static Map<IAnnotation, IMethod> searchAnnotatedMethods(IProgressMonitor pm, IJavaSearchScope scope) throws CoreException {
		final AnnotationSearchRequestor<IMethod> requestor = new AnnotationSearchRequestor<IMethod>(IMethod.class) {
        	@Override
        	protected boolean accept(IMethod t) {
        		return true;
        	}
        };
		return AnnotationSearch.findAnnotationsInContainer(
		          IMethod.class,
		          EntryPoint.class,
		          requestor,
                  pm,
                  scope);
	}
	
	public abstract static class EntryPointConfiguration {
		private final IMethod entryPointMethod;
		private final SourceRefElement sourceRef;
		private final SourceRefElement methodSourceRef;
		
		protected EntryPointConfiguration(IMethod entryPointMethod, SourceRefElement sourceRef, SourceRefElement methodSourceRef) {
			this.entryPointMethod = entryPointMethod;
			this.sourceRef = sourceRef;
			this.methodSourceRef = methodSourceRef;
		}
		public IMethod getEntryPointMethod() {
			return entryPointMethod;
		}
		
		public SourceRefElement getEntryPointSourceRef() {
			return methodSourceRef;
		}
		
		public SourceRefElement getSourceRef() {
			return sourceRef;
		}
		
		public abstract SDGConfig getSDGConfigFor(CheckIFCConfig cfc);
		
		// TODO: possibly cache the AST somehow?!?! or find a method to resolve FULLY-QUALIFIED type-names 
		// that works with JDT's JavaModel only? 
		protected CompilationUnit getASTForEntryPoint() {
			ICompilationUnit unit = entryPointMethod.getCompilationUnit();
			ASTParser parser = ASTParser.newParser(AST.JLS4);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource(unit);
			parser.setResolveBindings(true);
			return (CompilationUnit) parser.createAST(null);
		}

		public abstract boolean isDefaultParameters();
		
		protected abstract int priority();

		public final boolean replaces(EntryPointConfiguration other) {
			return this.priority() > other.priority() && this.entryPointMethod.equals(other.entryPointMethod);
		}
	}
	
	public static class DefaultMainEntryPointConfiguration extends EntryPointConfiguration {
		public DefaultMainEntryPointConfiguration(IMethod mainMethod) {
			super(mainMethod, null, (mainMethod instanceof SourceRefElement)? (SourceRefElement) mainMethod : null);
			assert (mainMethod instanceof SourceRefElement) == (mainMethod instanceof SourceMethod); 
		}
		
		@Override
		public SDGConfig getSDGConfigFor(CheckIFCConfig cfc) {
			
			final String s = resolveMethodSignature(this.getEntryPointMethod(), getASTForEntryPoint());
			JavaMethodSignature mainMethodSignature = JavaMethodSignature.fromString(s);
			return CheckInformationFlow.createDefaultConfig(cfc, mainMethodSignature);
		}

		@Override
		public String toString() {
			return "default configuration";
		}

		@Override
		protected int priority() {
			return 0;
		}

		@Override
		public boolean isDefaultParameters() {
			return true;
		}
	}
	
	public static class AnnotationEntryPointConfiguration extends EntryPointConfiguration {
		private final IAnnotation annotation;
		public AnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, (annotation instanceof SourceRefElement)? (SourceRefElement) annotation : null,
			              (method instanceof SourceRefElement)? (SourceRefElement) method : null);
			assert (annotation instanceof SourceRefElement) == (annotation instanceof Annotation);
			assert (method instanceof SourceRefElement) == (method instanceof SourceMethod); 
			this.annotation = annotation;
		}
		
		@Override
		public SDGConfig getSDGConfigFor(CheckIFCConfig cfc) {
			String s = resolveMethodSignature(this.getEntryPointMethod(), getASTForEntryPoint());
			JavaMethodSignature mainMethodSignature = JavaMethodSignature.fromString(s);
			SDGConfig def = CheckInformationFlow.createDefaultConfig(cfc, mainMethodSignature);
			// ....
			return def;
		}
		
		@Override
		public String toString() {
			try {
				final IMemberValuePair mps[] = annotation.getMemberValuePairs();
				if (mps != null && mps.length > 0) {
					final StringBuilder sb = new StringBuilder("configuration: ");
					for (final IMemberValuePair mp : mps) {
						sb.append(mp.getMemberName() + "=" + mp.getValue());
						sb.append(", ");
					}
					sb.delete(sb.length() - 2, sb.length());
					
					return sb.toString();
				}
			} catch (JavaModelException e) {
			}

			return "default configuration";
		}
		
		@Override
		protected int priority() {
			return 100;
		}

		@Override
		public boolean isDefaultParameters() {
			boolean isDefault = true;
			try { 
				final IMemberValuePair[] mvp = annotation.getMemberValuePairs();
				isDefault = (mvp == null || mvp.length == 0);
			} catch (JavaModelException e) {}
			
			return isDefault;
		}
	}
	
	public static Collection<EntryPointConfiguration> findEntryPointsIn(IJavaElement element, IProgressMonitor pm) throws CoreException {
		
		Collection<EntryPointConfiguration> result = new LinkedList<EntryPointSearch.EntryPointConfiguration>();

		final IJavaSearchScope scope = SearchEngine.createJavaSearchScope(new IJavaElement[] {element}, IJavaSearchScope.SOURCES);
		
		// all main methods are valid entry-points
		for (IMethod mainMethod : searchMainMethods(pm, scope)) {
			result.add(new DefaultMainEntryPointConfiguration(mainMethod));
		}
		
		// .. and so are all Methods annotated with EntryPoint
		// TODO: If a main Method is annotated, overwrite thats main-Methods default configurations with some (first?)
		// Annotation Settings
		for (Map.Entry<IAnnotation, IMethod> e : searchAnnotatedMethods(pm, scope).entrySet()) {
			result.add(new AnnotationEntryPointConfiguration(e.getValue(),e.getKey()));
		}
		
		return result;
	}
	
	public static String resolveMethodSignature(
			final IMethod method, final CompilationUnit ast) {
		if (ast == null) return null;

		ASTNode matchNode;
		try {
			matchNode = NodeFinder.perform(ast, method.getNameRange());
		} catch (JavaModelException e) {
			matchNode = null;
		}
		
		if(matchNode == null || matchNode.getParent() == null || matchNode.getParent().getNodeType() != ASTNode.METHOD_DECLARATION) {
			return null;
		}
		
		final MethodDeclaration methodDeclaration = ((MethodDeclaration)matchNode.getParent());
		final IMethodBinding methodBinding = methodDeclaration.resolveBinding();
		if(methodBinding == null) return null;

		
		final String returnedType = methodBinding.getReturnType().getName();
		final StringBuilder stb = new StringBuilder();
		if (returnedType != null) {
			stb.append(returnedType).append(" ");
		} else {
			stb.append("void ");
		}
		
		stb.append(method.getDeclaringType().getFullyQualifiedName());
		stb.append(".");
		stb.append(method.getElementName()).append("(");
		
		
		boolean isFirst = true;
		@SuppressWarnings("unchecked")
		final List<SingleVariableDeclaration> parameters = methodDeclaration.parameters();
		for (SingleVariableDeclaration parameter : parameters ) {
			if (!isFirst) stb.append(", ");
			isFirst = false;
			final IVariableBinding paramBinding = parameter.resolveBinding();
			final String paramTypeName = paramBinding.getType().getQualifiedName();
			stb.append(paramTypeName);
		}
		stb.append(")");	

		return stb.toString();
	}
}
