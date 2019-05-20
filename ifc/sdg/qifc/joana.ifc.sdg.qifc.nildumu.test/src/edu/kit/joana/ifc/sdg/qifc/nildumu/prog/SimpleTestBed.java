package edu.kit.joana.ifc.sdg.qifc.nildumu.prog;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.MethodInvocationHandlersToUse;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.ShouldLeak;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI.leak;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI.output;

import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.Config;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.EntryPoint;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.Value;

public class SimpleTestBed {

	public static void main(String[] args) {
		simpleAdd(10);
		simple2(10);
		simple3(10, 10);
		simple4(10, 10);
		simpleIf(true);
		whileLoop(10);
		whileWithBreak(10);
		mod(10);
		mod2(10);
		advancedIf(10, 10);
		simpleFuncTest(10);
		basicFunctionCalls1(10);
		basicFib(10);
		basicDepsOnFunction(10);
		weirdLoopFunctionTermination(10, 10);
		nestedFunctionCalls(10);
		nestedFunctionCalls2(10);
		conditionalRecursion(10);
		unreachableLeak(10);
		testBasicLoop(10);
		testBasicLoopNested(10, true);
		simpleIfWithFuncCall(10);
		purseReduced(10);
		concLoopCond(1);
		loopWithShifts(10);
		weirdLoopFunctionTermination2(1);
		weirdLoopFunctionTermination3(1);
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=1)
	public static void testBasicLoop(@Source @Value("0b0u") int h) {
       int x = 0;
       while (h == 0){
            x = x + 1;
       }
       leak(x);
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=1)
	public static void testBasicLoopNested(@Source @Value("0b0u") int h, @Source(level="l") @Value("0bu") boolean l) {
	    int x = 0; 
		while (l){
	        while (l){
	            x = x + h;
	        }
	     }
	     leak(x);
	}
	
	@EntryPoint
	@Config(intWidth=1)
	@ShouldLeak(exactly=1)
	public static void simpleAdd(@Source int h) {
		output(h + 1, "l");
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=1)
	public static void simple2(@Source int h) {
		output(h | 1, "l");
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=2)
	public static void simple3(@Source int h, @Source int h2) {
		output(h | h2, "l");
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=2)
	public static void simple4(@Source int h, @Source int h2) {
		int a = 0b0 | h;
		output(h | a, "l");
	}
	
	@EntryPoint
	@ShouldLeak(exactly=1)
	public static void simpleIf(@Source boolean h) {
		int o;
		if (h) {
			o = 1;
		} else {
			o = 0;
		}
		leak(o);
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=1, bits="0b0u")
	public static void advancedIf(@Source int h, @Source int h2) {
		int a = 1;
		if (h > 0) {
			a = (h | a) & 0;
		}
		output(a, "l");
	}
	
	@EntryPoint
	@Config(intWidth=1)
	@ShouldLeak(exactly=1)
	public static void whileLoop(@Source int h) {
		int o = 0;
		while (h > 0) {
			o++;
		}
		leak(o);
	}
	
	@EntryPoint
	@Config(intWidth=3)
	@ShouldLeak(exactly=0)
	public static void whileWithBreak(@Source int h) {
		int o = 0;
		while (o >= 0) {
			if (1 == 1) {
				break;
			}
			o += h;
		}
		leak(o);
	}
	
	@EntryPoint
	@Config(intWidth=3)
	@ShouldLeak(exactly=3)
	public static void mod(@Source int h) {
		leak(3 % h);
	}
	
	@EntryPoint
	@Config(intWidth=3)
	@ShouldLeak(exactly=1)
	public static void mod2(@Source int h) {
		leak(h % 2);
	}
	
	@EntryPoint
	@Config(intWidth=1)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=1)
	public static void simpleFuncTest(@Source(level=Level.HIGH) int h) {
		output(run(h, h | 0) | 0, "l");
	}
	
	public static int run(int a, int b) {
		return a | b;
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=2)
	public static void basicFunctionCalls1(@Source int h) {
		leak(_1_bla(h));
	}
	
	public static int _1_bla(int a) {
		return a + 1;
	}
	
	@EntryPoint
	@Config(intWidth=5)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=4)
	public static void basicFib(@Source @Value("0b0uuuu") int h) {
		leak(fib(h));
	}
	
	public static int fib(int a) {
		int r = 1;
		if (a > 1){
			r = fib(a - 1) + fib(a - 2);
		}
		return r;
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@MethodInvocationHandlersToUse({"inlining"})
	@ShouldLeak(exactly=1)
	public static void basicDepsOnFunction(@Source @Value("0buu") int h) {
		leak(_2_bla(h));
	}
	
	public static int _2_bla(int a) {
		return _1_bla(a) | 1;
	}
	
	
	@EntryPoint
	@Config(intWidth=3)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=1)
	public static void weirdLoopFunctionTermination(@Source @Value("0b0u") int h, @Source(level="l") int l) {
	    int res = 0; 
		while (l != 0) {
			res = res + _3_bla(h);
	    }
		leak(_3_bla(h));
	}
	
	public static int _3_bla(int a) {
		int r = 1;
        while (a > 0){
            if (a > 1){
               r = r + _3_bla(a - 1);
            }
        }
        return r;
    }
	
	@EntryPoint
	@Config(intWidth=32)
	@MethodInvocationHandlersToUse
	@ShouldLeak(atLeast=1)
	public static void weirdLoopFunctionTermination2(@Source int h) {
	    int res = 0; 
		while (h < 1000) {
			while (h < 100 + func(h)) {
				res = res + func(h);
				h += fib(func(h));
			}
	    }
		leak(func(h) | fib(3) | _4_f2(100));
	}
	
	public static int func(int a) {
		int r = 1;
        while (a > 0){
            if (a > 1){
               r = r + _3_bla(a - 1);
            }
        }
        return r | _fib(100);
    }
	
	public static int _fib(int a) {
		int r = 1;
		if (a > 1){
			r = fib(a - 1) + fib(a - 2);
		}
		return r;
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=2)
	public static void nestedFunctionCalls(@Source @Value("0buu") int h) {
		leak(_4_f(h));
	}
	
	public static int _4_f(int a) {
		return _4_h(a);
	}
	
	public static int _4_h(int a) {
		return _4_g(a);
	}
	
	public static int _4_g(int a) {
		return a;
	}
	
	@EntryPoint
	@Config(intWidth=3)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=3)
	public static void conditionalRecursion(@Source @Value("0buuu") int h) {
		leak(_5_f(h, 0, 0, 0, 0, 4));
	}
	
	public static int _5_f(int x, int y, int z, int w, int v, int l) {
         int r = 0;
         if (l == 0) {
            r = v;
         } else {
            r = _5_f(0, x, y, z, w, l+0b111);
         }
         return r;
     }
	
	@EntryPoint
	@Config(intWidth=2)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=2)
	public static void nestedFunctionCalls2(@Source @Value("0buu") int h) {
		leak(_4_f2(h));
	}
	
	public static int _4_f2(int a) {
		return _4_h2(a) & _4_h2(a) & _4_h2(a);
	}
	
	public static int _4_h2(int a) {
		return _4_g2(a) & _4_g2(a) & _4_g2(a);
	}
	
	public static int _4_g2(int a) {
		return _4_i2(a) & _4_i2(a) & _4_i2(a);
	}
	
	public static int _4_i2(int a) {
		return a;
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@MethodInvocationHandlersToUse({"inlining"})
	@ShouldLeak(exactly=0,bits="1")
	public static void unreachableLeak(@Source @Value("0b0u") int h) {
		int r = 0;
		if (1 != fib(1)) {
			if (1 == fib(h)) {
				r = fib(h);
			}
		}
		leak(fib(1));
	}
	
	@EntryPoint
	@Config(intWidth=5)
	@MethodInvocationHandlersToUse("all")
	@ShouldLeak(exactly=4)
	public static void simpleIfWithFuncCall(@Source @Value("0b0uuuu") int h) {
		int r = 1;
		if (h > 1){
			r = _5_f(h);
		}
		leak(r);
	}
	
	public static int _5_f(int a) {
		return a;
	}
	
	@EntryPoint
	@Config(intWidth=32)
	@ShouldLeak(exactly=4)
	public static void purseReduced(@Source @Value("0b0uuuu") int h) {
		int O = 0;
		while (h < 100){
		    h = h + 1;
		    O = O + 1;
		}
		leak(O);
	}
	
	@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=2)
	public static void concLoopCond(@Source @Value("0buu") int h) {
		int O = 0;
		while (h == 0 | h == 0){
		    O = O + 1;
		}
		leak(O);
	}
	
	@EntryPoint 
	@Config(intWidth=32)
	@ShouldLeak(atLeast=10)
	public static void loopWithShifts(@Source int I){
	int BITS = 16;
	int O = 0;
	int m = 0;
	int i = 0;
	while  ((i < BITS)) {
		m = (1 << (30 - i));
		if (((O + m) <= I)) {
			O = (O + m);
		} else {

		}
		i = (i + 1);
	}
	leak(O);
	}
	
	@EntryPoint
	@Config(intWidth=32)
	@MethodInvocationHandlersToUse("all")
	@ShouldLeak(exactly=0)
	public static void weirdLoopFunctionTermination3(@Source int h) {
	    int res = 0; 
		while (h < fib(h) || h == 0) {
			res = res + 1;
		}
	}
}
