package edu.kit.joana.ui.wala.easyifc.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import com.google.common.collect.Multimap;

import edu.kit.joana.api.IFCAnalysis;
import edu.kit.joana.api.lattice.BuiltinLattices;
import edu.kit.joana.api.sdg.SDGConfig;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.LatticeUtil;
import edu.kit.joana.ifc.sdg.lattice.LatticeValidator;
import edu.kit.joana.ifc.sdg.lattice.PrecomputedLattice;
import edu.kit.joana.ifc.sdg.lattice.impl.EditableLatticeSimple;
import edu.kit.joana.ifc.sdg.lattice.impl.PowersetLattice;
import edu.kit.joana.ifc.sdg.lattice.impl.ReversedLattice;
import edu.kit.joana.ifc.sdg.util.JavaMethodSignature;
import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ui.wala.easyifc.model.CheckInformationFlow;
import edu.kit.joana.ui.wala.easyifc.model.CheckInformationFlow.CheckIFCConfig;
import edu.kit.joana.ui.wala.easyifc.util.AnnotationSearch.AnnotationSearchRequestor;
import edu.kit.joana.util.Pair;

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
		
		public abstract Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotateSDG(IFCAnalysis analysis);
		
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

		public abstract Collection<String> getErrors();

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

		@Override
		public Collection<String> getErrors() {
			return Collections.emptyList();
		}
		
		@Override
		public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotateSDG(IFCAnalysis analysis) {
			return analysis.addAllJavaSourceAnnotations(this.lattice());
		}
	}
	
	public static class DefaultAnnotationEntryPointConfiguration extends AnnotationEntryPointConfiguration {
		private final IStaticLattice<String> lattice = BuiltinLattices.getBinaryLattice();
		public DefaultAnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, annotation);
		}
		@Override
		public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotateSDG(IFCAnalysis analysis) {
			return analysis.addAllJavaSourceAnnotations(lattice);
		}
		@Override
		public IStaticLattice<String> lattice() {
			return lattice;
		}
	}
	
	public static class InvalidAnnotationEntryPointConfiguration extends AnnotationEntryPointConfiguration {
		public InvalidAnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, annotation);
		}

		@Override
		public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotateSDG(IFCAnalysis analysis) {
			throw new UnsupportedOperationException("Invalid EntryPointConfiguration; cannot annotate SDG");
		}

		@Override
		public IStaticLattice<String> lattice() {
			throw new UnsupportedOperationException("Invalid EntryPointConfiguration; no lattice available");
		}
		
	}
	
	public static class DatasetsAnnotationEntryPointConfiguration extends AnnotationEntryPointConfiguration {
		private final PrecomputedLattice<Set<String>> stringEncodedLattice;
		private final Map<Set<String>, String> fromSet;
		public DatasetsAnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, annotation);
			final Set<String> datasets  = new HashSet<>();
			try {
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if ("datasets".equals(pair.getMemberName())) {
						if  (pair.getValueKind() != IMemberValuePair.K_STRING) {
							errors.add("Illegal datasets specification: " + pair.getValue() + "  - use literal Strings instead (e.g.: { \"address\", \"bankingh\" })");
						}
						Object[] levels = (Object[]) pair.getValue();
						for (Object o : levels) {
							String dataset = (String) o;
							boolean fresh = datasets.add(dataset);
							if (!fresh) errors.add("Duplicate dataset: " + dataset);
						}
					}
				}
				
				this.stringEncodedLattice = new PrecomputedLattice<Set<String>>(new PowersetLattice<String>(datasets));
				this.fromSet = this.stringEncodedLattice.getFromOriginalMap();
				
			} catch (JavaModelException e) {
				// Thrown by IAnnotation.getMemberValuePairs()
				// TODO: better error handling
				throw new RuntimeException(e);
			}

		}
		@Override
		public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotateSDG(IFCAnalysis analysis) {
			return analysis.addAllJavaSourceIncludesAnnotations(this.fromSet, this.stringEncodedLattice);
		}
		@Override
		public IStaticLattice<String> lattice() {
			return stringEncodedLattice;
		}
	}
	
	public static class AdversariesAnnotationEntryPointConfiguration extends AnnotationEntryPointConfiguration {
		private final IStaticLattice<String> stringEncodedLattice;
		private final Map<Set<String>, String> fromSet;
		
		public AdversariesAnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, annotation);
			final Set<String> datasets  = new HashSet<>();
			try {
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if ("adversaries".equals(pair.getMemberName())) {
						if  (pair.getValueKind() != IMemberValuePair.K_STRING) {
							errors.add("Illegal adversaries specification: " + pair.getValue() + "  - use literal Strings instead (e.g.: { \"admin\", \"guest\" })");
						}
						Object[] levels = (Object[]) pair.getValue();
						for (Object o : levels) {
							String adversary = (String) o;
							boolean fresh = datasets.add(adversary);
							if (!fresh) errors.add("Duplicate adversary: " + adversary);
						}
					}
				}
				final PrecomputedLattice<Set<String>> pre = new PrecomputedLattice<Set<String>>(new PowersetLattice<String>(datasets));
				this.fromSet = pre.getFromOriginalMap();
				this.stringEncodedLattice = new ReversedLattice<String>(pre);
			} catch (JavaModelException e) {
				// Thrown by IAnnotation.getMemberValuePairs()
				// TODO: better error handling
				throw new RuntimeException(e);
			}

		}
		@Override
		public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotateSDG(IFCAnalysis analysis) {
			return analysis.addAllJavaSourceMayKnowAnnotations(fromSet, stringEncodedLattice);
		}
		@Override
		public IStaticLattice<String> lattice() {
			return stringEncodedLattice;
		}
		
	}
	
	public static class SpecifiedLatticeAnnotationEntryPointConfiguration extends AnnotationEntryPointConfiguration {
		private final IStaticLattice<String> lattice;
		public SpecifiedLatticeAnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, annotation);
			
			final EditableLatticeSimple<String> specifiedLattice  = new EditableLatticeSimple<String>();
			
			try {
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if ("levels".equals(pair.getMemberName())) {
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
						assert (pair.getValueKind() == IMemberValuePair.K_ANNOTATION);
						Object[] mayflows = (Object[]) pair.getValue();
						for (Object o : mayflows) {
							IAnnotation mayflow = (IAnnotation) o;
							String from = null;
							String to = null;
							for (IMemberValuePair fromto : mayflow.getMemberValuePairs()) {
								if ("from".equals(fromto.getMemberName())) {
									if  (fromto.getValueKind() != IMemberValuePair.K_STRING) {
										errors.add("Illegal from-level : " + pair.getValue() + "  - use literal String instead (e.g.: \"low\")");
									}
									from = (String) fromto.getValue();
								}
								if ("to".equals(fromto.getMemberName())) {
									if  (fromto.getValueKind() != IMemberValuePair.K_STRING) {
										errors.add("Illegal to-level : " + pair.getValue() + "  - use literal String instead (e.g.: \"low\")");
									}
									to = (String) fromto.getValue();
								}
							}
							assert (from != null && to != null);
							if(!specifiedLattice.getElements().contains(from)) {
								errors.add("Unknown from-level: " + from);
							}
							if(!specifiedLattice.getElements().contains(to)) {
								errors.add("Unknown to-level: " + from);
							}

							specifiedLattice.setImmediatelyGreater(from, to);
						}
					}
				}
				final Collection<String> antiSymmetryViolations = LatticeValidator.findAntisymmetryViolations(specifiedLattice);
				if (antiSymmetryViolations.isEmpty()) {
					this.lattice = LatticeUtil.dedekindMcNeilleCompletion(specifiedLattice);
				} else {
					errors.add("Cycle in user-specified lattice. Elements contained in a cycle: " + antiSymmetryViolations);
					this.lattice = null;
				}
			} catch (JavaModelException e) {
				// Thrown by IAnnotation.getMemberValuePairs()
				// TODO: better error handling
				throw new RuntimeException(e);
			}
		}
		@Override
		public Pair<Multimap<SDGProgramPart, Pair<Source, String>>, Multimap<SDGProgramPart, Pair<Sink, String>>> annotateSDG(IFCAnalysis analysis) {
			return analysis.addAllJavaSourceAnnotations(lattice);
		}
		@Override
		public IStaticLattice<String> lattice() {
			return lattice;
		}
	}
	
	public abstract static class AnnotationEntryPointConfiguration extends EntryPointConfiguration {
		protected final List<String> errors = new LinkedList<>();
		protected final IAnnotation annotation;

		
		private AnnotationEntryPointConfiguration(IMethod method, IAnnotation annotation) {
			super(method, (annotation instanceof SourceRefElement)? (SourceRefElement) annotation : null,
			              (method instanceof SourceRefElement)? (SourceRefElement) method : null);
			assert (annotation instanceof SourceRefElement) == (annotation instanceof Annotation);
			assert (method instanceof SourceRefElement) == (method instanceof SourceMethod); 
			this.annotation = annotation;
		}
		
		public static AnnotationEntryPointConfiguration fromAnnotation(IMethod method, IAnnotation annotation) {
			assert (annotation instanceof SourceRefElement) == (annotation instanceof Annotation);
			assert (method instanceof SourceRefElement) == (method instanceof SourceMethod); 

			boolean latticeSpecified = false;
			boolean datasetsSpecified = false;
			boolean adversariesSpecified = false;
			
			try {
				for (IMemberValuePair pair : annotation.getMemberValuePairs()) {
					if ("lattice".equals(pair.getMemberName())) {
						latticeSpecified = ((Object[]) pair.getValue()).length > 0;
					}
					if ("datasets".equals(pair.getMemberName())) {
						datasetsSpecified = ((Object[]) pair.getValue()).length > 0;
					}
					if ("adversaries".equals(pair.getMemberName())) {
						adversariesSpecified = ((Object[]) pair.getValue()).length > 0;
					}
				}
				
				final int nrOfspecifications =
						(latticeSpecified    ? 1 :0) + 
						(datasetsSpecified   ? 1 :0) +
						(adversariesSpecified? 1 :0);
				if (nrOfspecifications == 0) {
					return new DefaultAnnotationEntryPointConfiguration(method, annotation);
				} else if (nrOfspecifications > 1) {
					final AnnotationEntryPointConfiguration entryPointConfig = new InvalidAnnotationEntryPointConfiguration(method, annotation);
					entryPointConfig.errors.add("An EntryPoint specification may at most contain one of: datasets,lattice,levels");
					return entryPointConfig;
				} else {
					if (latticeSpecified) {
						return new SpecifiedLatticeAnnotationEntryPointConfiguration(method, annotation);
					} else if (datasetsSpecified) {
						return new DatasetsAnnotationEntryPointConfiguration(method, annotation);
					} else {
						assert (adversariesSpecified);
						return new AdversariesAnnotationEntryPointConfiguration(method, annotation);
					}
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
							case "datasets": 
							case "levels":
							case "adversaries":
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
		public Collection<String> getErrors() {
			return Collections.unmodifiableCollection(this.errors);
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
			result.add(AnnotationEntryPointConfiguration.fromAnnotation(e.getValue(),e.getKey()));
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
