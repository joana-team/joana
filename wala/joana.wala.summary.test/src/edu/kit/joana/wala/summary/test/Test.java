package edu.kit.joana.wala.summary.test;

import de.uni.trier.infsec.core.Setup;
import edu.kit.joana.wala.summary.parex.PreprocessKt;
import edu.kit.joana.wala.summary.parex.SequentialAnalysis;
import edu.kit.joana.wala.summary.parex.SequentialAnalysis2;
import joana.api.testdata.seq.*;
import joana.api.testdata.toy.declass.Declass1;
import joana.api.testdata.toy.demo.Demo1;
import joana.api.testdata.toy.demo.NonNullFieldParameter;
import joana.api.testdata.toy.pw.PasswordFile;
import joana.api.testdata.toy.rec.MyList;
import joana.api.testdata.toy.rec.MyList2;
import joana.api.testdata.toy.rec.PrimitiveEndlessRecursion;
import joana.api.testdata.toy.rec.PrimitiveEndlessRecursion2;
import joana.api.testdata.toy.sensitivity.FlowSens;
import joana.api.testdata.toy.simp.*;
import joana.api.testdata.toy.test.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static edu.kit.joana.wala.summary.test.Util.assertAnalysis;
import static edu.kit.joana.wala.summary.test.Util.withDisabledGraphExport;

public class Test {

    private static Stream<Class<?>> testCases(){
        return Stream.of(
            TinyExample.class,
            BasicTestClass.class,
            FlowSens.class,
            AssChain.class,
            MicroExample.class,
            UnconnectedGraph.class,
            Nested.class,
            NestedWithException.class,
            Sick2.class,
            MathRound.class,
            ControlDep.class,
            Independent.class,
            ObjSens.class,
            SystemCallsTest.class,
            VeryImplictFlow.class,
            MyList.class,
            PrimitiveEndlessRecursion.class,
            PrimitiveEndlessRecursion2.class,
            MyList2.class,
            DynamicDispatch.class,
            OutFlush.class,
            PasswordFile.class,
            Demo1.class,
            NonNullFieldParameter.class,
            Declass1.class,
            ExampleLeakage.class,
            ArrayAccess.class,
            ArrayOverwrite.class,
            FieldAccess.class,
            FieldAccess.class,
            FieldAccess2.class,
            FieldAccess3.class,
            Constants1.class,
            Constants2.class,
            Reflection.class,
            DynamicDispatch.class,
            StrangeTryCatchFinallyWalaBug.class,
            StrangeTryCatchFinallyWalaBugComplex.class,
            MartinMohrsStrangeTryCatchFinallyWalaBug.class,
            StaticField.class,
            Setup.class,
            JLex.Main.class
        );
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysis(Class<?> klass){
        assertAnalysis(klass, "", new SequentialAnalysis(), "tmp");
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysis2(Class<?> klass){
        assertAnalysis(klass, "", new SequentialAnalysis2(), "tmp");
    }

    @ParameterizedTest
    @MethodSource("testCases")
    public void testAnalysis2WithPreproc(Class<?> klass){
        assertAnalysis(klass, "", new SequentialAnalysis2(), "tmp", PreprocessKt::removeNormalNodes);
    }

    @org.junit.jupiter.api.Test
    public void test2(){
        assertAnalysis(TinyExample.class, "", new SequentialAnalysis2(), "tmp");
    }

    @org.junit.jupiter.api.Test
    public void testOutFlush2() throws ClassNotFoundException {
        assertAnalysis(Class.forName("OutFlush2"), "", new SequentialAnalysis2(), "tmp");
    }

    public static void main(String[] args) {
        withDisabledGraphExport(() -> {
            try {
                try {
                    assertAnalysis(Class.forName(args[0]), "", new SequentialAnalysis2(), "tmp");
                } catch (AssertionError e){
                    e.printStackTrace();
                    System.exit(0);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        });
        System.exit(1);
    }
}