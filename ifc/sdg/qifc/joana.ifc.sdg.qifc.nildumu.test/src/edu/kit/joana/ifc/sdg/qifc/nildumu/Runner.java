/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.params.provider.Arguments;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.Expect;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.MethodInvocationHandlersToUse;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.ShouldLeak;
import edu.kit.joana.ifc.sdg.qifc.nildumu.interproc.MethodInvocationHandler;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.EntryPoint;

/**
 * Runner of simple tests based on the annotations in the annotations folder.
 * 
 */
public class Runner {
	
	public static List<String> DEFAULT_MIHS = MethodInvocationHandler.getExamplePropLines();

	private static class Tmp {
		@MethodInvocationHandlersToUse
		@ShouldLeak
		@Expect
		public void program() {}
	}
	
	public static class TestCase {
		public final Class<?> klass;
		public final Method mainMethod;
		
		public TestCase(Class<?> klass, Method mainMethod) {
			this.klass = klass;
			this.mainMethod = mainMethod;
		}
		
		public String description() {
			String descr = mainMethod.getName();
			if (mainMethod.getAnnotation(EntryPoint.class).description().length() > 0) {
				descr += "[" + mainMethod.getAnnotation(EntryPoint.class).description() + "]";
			}
			return descr;
		}
		
		@Override
		public String toString() {
			return description();
		}
		
		public List<String> applicableMethodInvocationHandlerProps() {
			if (!mainMethod.isAnnotationPresent(MethodInvocationHandlersToUse.class)) {
				return Collections.emptyList();
			}
			MethodInvocationHandlersToUse anno = mainMethod.getAnnotation(MethodInvocationHandlersToUse.class);
			List<String> handlerProps = new ArrayList<>();
			if (anno.value().length == 0 || Arrays.asList(anno.value()).contains("default")) {
				handlerProps = DEFAULT_MIHS.stream()
						.filter(m -> !Arrays.asList(anno.exclude()).contains(MethodInvocationHandler.parse(m).getName()))
						.collect(Collectors.toList());
			} else {
				Stream.of(anno.value()).filter(p -> !p.equals("default")).forEach(handlerProps::add);
			}
			return handlerProps;
		}
		
		private static <T extends Annotation> T getAnnotation(Class<T> annotationClass, Method method) {
			if (method.isAnnotationPresent(annotationClass)) {
				return method.getAnnotation(annotationClass);
			} else {
				try {
					return Tmp.class.getDeclaredMethods()[0].getAnnotation(annotationClass);
				} catch (SecurityException e) {
					e.printStackTrace();
				}
			}
			return null;
		}
		
		public void testContext(Context context) {
			ContextMatcher matcher = new ContextMatcher(context);
			ShouldLeak anno = getAnnotation(ShouldLeak.class, mainMethod);
			Expect exp = getAnnotation(Expect.class, mainMethod);
			if (anno.atLeast() >= 0) {
				matcher.leaksAtLeast(anno.atLeast());
			}
			if (anno.exactly() >= 0) {
				matcher.leaks(anno.exactly());
			}
			if (anno.atMost() >= 0) {
				matcher.leaksAtMost(anno.atMost());
			}
			if (anno.bits().length() > 0) {
				matcher.custom(c -> assertEquals(Lattices.vl.parse(anno.bits()).toLiteralString(), 
						c.output.getValues().get(0).second.toLiteralString(),
						"The leaked value should be " + anno.bits()));
			}
			if (exp.bitWidth() > 0) {
				matcher.custom(c -> assertEquals(exp.bitWidth(), c.maxBitWidth,
						"Bit width"));
			}
			matcher.run();
		}
	}

	public static Stream<TestCase> testCasesForClass(Class<?> klass){
		return Stream.of(klass.getDeclaredMethods())
				.filter(m -> Modifier.isStatic(m.getModifiers()) && m.isAnnotationPresent(EntryPoint.class))
				.map(m -> new TestCase(klass, m));
	}
	
	public static Stream<TestCase> testCasesForClassAndInnerClasses(Class<?> klass){
		return Stream.concat(testCasesForClass(klass), 
				Stream.of(klass.getDeclaredClasses())
				.flatMap(Runner::testCasesForClassAndInnerClasses));
	}
	
	public static Stream<Arguments> testCasesToTestClassMIHStream(Stream<TestCase> testCases){
		return testCases.flatMap(t -> {
			List<String> handlerProps = t.applicableMethodInvocationHandlerProps();
			if (handlerProps.isEmpty()) {
				return Stream.of(Arguments.of(t, null));
			}
			return handlerProps.stream().map(p -> Arguments.of(t, p));
		});
	}
	
	public static Stream<Arguments> testCases(Class<?> baseClass){
		List<TestCase> bla = testCasesForClass(baseClass).collect(Collectors.toList());
		return testCasesToTestClassMIHStream(testCasesForClassAndInnerClasses(baseClass));
	}
	
	public static void test(TestCase testCase, String handlerProp, boolean verbose) {
		Builder builder = new Builder();
		if (System.getenv("NILDUMU_GRAPGHS") != null && System.getenv("NILDUMU_GRAPGHS").equals("1")) {
			builder.dumpDir("test_dump/" + testCase.klass.getCanonicalName() + "/" + testCase.mainMethod.getName() + "_" + handlerProp);
		}
		builder.methodInvocationHandler(handlerProp)
			   .entry(testCase.klass)
			   .entryMethod(testCase.mainMethod);
		if (verbose) {
			builder.enableDumpAfterBuild();
		}
		Program program = builder.buildProgramOrDie();
		testCase.testContext(program.analyze());
	}
}