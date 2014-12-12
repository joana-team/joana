package edu.kit.joana.ui.wala.easyifc.util;

import java.util.Arrays;
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

import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.ifc.sdg.lattice.IEditableLattice;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
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
		
		public abstract IStaticLattice<String> lattice();
		
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
		
		@Override
		public IStaticLattice<String> lattice() {
			return BuiltinLattices.getBinaryLattice();
		}
	}
	
	public static class AnnotationEntryPointConfiguration extends EntryPointConfiguration {
		private final IAnnotation annotation;
		private final IStaticLattice<String> lattice;
		public AnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, (annotation instanceof SourceRefElement)? (SourceRefElement) annotation : null,
			              (method instanceof SourceRefElement)? (SourceRefElement) method : null);
			assert (annotation instanceof SourceRefElement) == (annotation instanceof Annotation);
			assert (method instanceof SourceRefElement) == (method instanceof SourceMethod); 
			this.annotation = annotation;
			
			boolean latticeSpecified = false;
			EditableLatticeSimple<String> specifiedLattice  = new EditableLatticeSimple<String>();
			try {
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if ("levels".equals(pair.getMemberName())) {
						latticeSpecified = true;
						if  (pair.getValueKind() != IMemberValuePair.K_STRING) {
							throw new IllegalArgumentException("Illegal levels specification: " + pair.getValue() + "  - use literal Strings instead (e.g.: { \"low\", \"high\" })");
						}
						Object[] levels = (Object[]) pair.getValue();
						for (Object o : levels) {
							String level = (String) o;
							specifiedLattice.addElement(level);
						}
					}
				}
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if ("lattice".equals(pair.getMemberName())) {
						latticeSpecified = true;
						assert (pair.getValueKind() == IMemberValuePair.K_ANNOTATION);
						Object[] mayflows = (Object[]) pair.getValue();
						for (Object o : mayflows) {
							IAnnotation mayflow = (IAnnotation) o;
							String from = null;
							String to = null;
							for (IMemberValuePair fromto : mayflow.getMemberValuePairs()) {
								if ("from".equals(fromto.getMemberName())) {
									if  (fromto.getValueKind() != IMemberValuePair.K_STRING) {
										throw new IllegalArgumentException("Illegal from-level : " + pair.getValue() + "  - use literal String instead (e.g.: \"low\")");
									}
									from = (String) fromto.getValue();
								}
								if ("to".equals(fromto.getMemberName())) {
									if  (fromto.getValueKind() != IMemberValuePair.K_STRING) {
										throw new IllegalArgumentException("Illegal to-level : " + pair.getValue() + "  - use literal String instead (e.g.: \"low\")");
									}
									to = (String) fromto.getValue();
								}
							}
							assert (from != null && to != null);
							if(!specifiedLattice.getElements().contains(from)) {
								throw new IllegalArgumentException("Unknown from-level: " + from);
							}
							if(!specifiedLattice.getElements().contains(to)) {
								throw new IllegalArgumentException("Unknown to-level: " + from);
							}

							specifiedLattice.setImmediatelyGreater(from, to);
						}
					}
				}
				if (!latticeSpecified) {
					this.lattice = BuiltinLattices.getBinaryLattice();
				} else {
					this.lattice = LatticeUtil.dedekindMcNeilleCompletion(specifiedLattice);
				}
			} catch (JavaModelException e) {
				// Thrown by IAnnotation.getMemberValuePairs()
				// TODO: better error handling
				throw new RuntimeException(e);
			}

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
						sb.append(mp.getMemberName() + "=");
						switch (mp.getMemberName()) {
							case "levels":
								Object[] levels = (Object[]) mp.getValue();
								sb.append(Arrays.toString(levels));
								break;
							case "lattice":
								sb.append("{");
								Object[] flows = (Object[]) mp.getValue();
								for (Object o : flows) {
									IAnnotation flow = (IAnnotation) o;
									sb.append("@MayFlow(");
									for (IMemberValuePair fromto : flow.getMemberValuePairs()) {
										sb.append(fromto.getMemberName());
										sb.append("=");
										sb.append(fromto.getValue());
										sb.append(", ");
									}
									sb.delete(sb.length() - 2, sb.length());
									sb.append("), ");
								}
								sb.delete(sb.length() - 2, sb.length());
								break;
							default: assert (false);
						}
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
		
		@Override
		public IStaticLattice<String> lattice() {
			return lattice;
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
