package edu.kit.joana.ifc.sdg.qifc.nildumu.prog;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Source;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.Expect;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.MethodInvocationHandlersToUse;
import edu.kit.joana.ifc.sdg.qifc.nildumu.annotations.ShouldLeak;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI.leak;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.ui.CodeUI.output;

import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.Config;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.EntryPoint;
import edu.kit.joana.ifc.sdg.qifc.nildumu.ui.Value;

public class SimpleTestBed2 {

	public static void main(String[] args) {
		//		simpleAdd(10);
		/*simple2(10);
//		simple3(10, 10);
//		simple4(10, 10);
//		simpleIf(true);
		whileLoop(10);
//		whileWithBreak(10);
//		mod(10);
//		mod2(10);
//		advancedIf(10, 10);
//		simpleFuncTest(10);
		basicFunctionCalls1(10);
		basicFib(10);
		basicFibReduced(10);
		basicDepsOnFunction(10);
		weirdLoopFunctionTermination(10, 10);
//		nestedFunctionCalls(10);
//		nestedFunctionCalls2(10);
//		conditionalRecursion(10);
		test(10, 10);
		test2(10, 10);
		simpleIfWithFuncCall(10);
		testBasicLoopNested(10, true);
		basicFibConst();*/
		//purseReduced(10);
		/*testBasicLoop(true);
		concLoopCond(10);
		maskedCopy(10);
		loopWithShifts(10);
		binsearch16(1);*/
		//binsearch16reduced(1);
		//test2(1);
		//test5(1);
		//fib_3_p(1);
		weirdLoopFunctionTermination2(1);
		program(10);
	}
	
	//@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=1)
	public static void testBasicLoop(@Source @Value("0bu") boolean h) {
       int x = 0;
       while (h){
            x = 1;
       }
       leak(x);
	}
	
	//@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=2)
	public static void test(@Source int h1, @Source int h2) {
		int r = 0;
		int o = 0;
		if (h1 == 0) {
			o = 1;
			if (h2 == 1) {
				r = h1;
			} else {
				r = 0;
			}
		}
		leak(r);
		leak(o);
	}
	
	@Config(intWidth=2)
	@ShouldLeak(exactly=2)
	public static void testIfChain2(@Source int h1, @Source int h2) {
		int r = 0;
		int o = 0;
		if (h1 == 0) {
			o = 1;
			if (h2 == 1) {
				r = h1;
			} else {
				r = 0;
			}
		}
		leak(r);
		leak(o);
	}
	
	@Config(intWidth=2)
	@ShouldLeak(exactly=1)
	public static void testSimpleIfChain(@Source int h1, @Source int h2) {
		int r = 0;
		int o = 0;
		if (h1 == 0) {
			o = 1;
			if (h1 == 1) {
				r = 1;
			} else {
				r = 0;
			}
		}
		leak(r);
		leak(o);
	}
	
	//@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=2)
	public static void test2(@Source int h1, @Source int h2) {
		int r = 0;
		if (h1 == 0) {
			r = h2;
		}
		leak(r);
	}
	
	//@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=0)
	public static void test3(@Source int h1, @Source int h2) {
		int r = 0;
		if (h1 == 0) {
			r = h1;
		} else {
			r = 0;
		}
		leak(r);
	}
	
//	@EntryPoint
//	@Config(intWidth=1)
//	@ShouldLeak(exactly=1, bits="0bu")
//	@Expect(bitWidth=1)
	public static void simpleAdd(@Source int h) {
		output(h + 1, "l");
	}
//	
	//@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=1)
	public static void simple2(@Source int h) {
		output(h | 1, "l");
	}
//	
//	@EntryPoint
//	@Config(intWidth=2)
//	@ShouldLeak(exactly=2)
//	public static void simple3(@Source int h, @Source int h2) {
//		output(h | h2, "l");
//	}
//	
//	@EntryPoint
//	@Config(intWidth=2)
//	@ShouldLeak(exactly=2)
//	public static void simple4(@Source int h, @Source int h2) {
//		int a = 0b0 | h;
//		output(h | a, "l");
//	}
//	
//	@EntryPoint
//	@ShouldLeak(exactly=1)
//	public static void simpleIf(@Source boolean h) {
//		int o;
//		if (h) {
//			o = 1;
//		} else {
//			o = 0;
//		}
//		leak(o);
//	}
//	
//	@EntryPoint
//	@Config(intWidth=2)
//	@ShouldLeak(exactly=1, bits="0b0u")
	public static void advancedIf(@Source int h, @Source int h2) {
		int a = 1;
		if (h > 0) {
			a = (h | a) & 0;
		}
		output(a, "l");
	}
//	
	//@EntryPoint
	@Config(intWidth=1)
	@ShouldLeak(exactly=1)
	public static void whileLoop(@Source int h) {
		int o = 0;
		while (h != 0) {
			o = o | 1;
		}
		leak(o);
	}
//	
//	@EntryPoint
//	@Config(intWidth=3)
//	@ShouldLeak(exactly=0)
//	public static void whileWithBreak(@Source int h) {
//		int o = 0;
//		while (o >= 0) {
//			if (1 == 1) {
//				break;
//			}
//			o += h;
//		}
//		leak(o);
//	}
//	
//	@EntryPoint
//	@Config(intWidth=3)
//	@ShouldLeak(exactly=3)
//	public static void mod(@Source int h) {
//		leak(3 % h);
//	}
//	
//	@EntryPoint
//	@Config(intWidth=3)
//	@ShouldLeak(exactly=1)
	public static void mod2(@Source int h) {
		leak(h % 2);
	}
//	
//	@EntryPoint
//	@Config(intWidth=1)
//	@MethodInvocationHandlersToUse(useDefaultHandlers=true)
//	@ShouldLeak(exactly=1)
//	public static void simpleFuncTest(@Source(level=Level.HIGH) int h) {
//		output(run(h, h | 0) | 0, "l");
//	}
//	
//	public static int run(int a, int b) {
//		return a | b;
//	}
//	
	//@EntryPoint
	@Config(intWidth=2)
	@MethodInvocationHandlersToUse("summary")
	@ShouldLeak(exactly=2, bits="0buu")
	public static void basicFunctionCalls1(@Source int h) {
		leak(_1_bla(h));
	}
	
	public static int _1_bla(int a) {
		return a;
	}
////	
	//@EntryPoint
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
	
	//@EntryPoint
	@Config(intWidth=5)
	@MethodInvocationHandlersToUse("handler=inlining;maxrec=2")
	@ShouldLeak(bits="1")
	public static void basicFibConst() {
		leak(__f(1));
	}
	
	public static int __f(int a) {
		int r = 1;
		if (a > 1){
			r = __f(a - 1);
		}
		return r;
	}
	
//	@EntryPoint
//	@Config(intWidth=5)
//	@MethodInvocationHandlersToUse(exclude= {"all"})
//	@ShouldLeak(exactly=4)
//	@Expect(bitWidth=5) // TODO: new test case
	public static void basicFibReduced(@Source @Value("0b0uuuu") int h) {
		leak(fib_r(h));
	}
	
	public static int fib_r(int a) {
		int r = 1;
		if (a == 1){
			r = a;
		}
		return r;
	}
	
	//@EntryPoint
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
//	
//	@EntryPoint
//	@Config(intWidth=2)
//	@MethodInvocationHandlersToUse({"call_string"})
//	@ShouldLeak(exactly=1)
	public static void basicDepsOnFunction(@Source @Value("0buu") int h) {
		leak(_2_bla(h));
	}
//	
	public static int _2_bla(int a) {
		return _2_bla(a) | 1;
	}
//	
//	
	//@EntryPoint
	@Config(intWidth=3)
	@MethodInvocationHandlersToUse("handler=all")
	@ShouldLeak(exactly=1,bits="0b0u")
	public static void weirdLoopFunctionTermination(@Source @Value("0b0u") int h, @Source(level="l") int l) {
		int r = 1;
        while (h != 0){
            r = _3_bla(h);
            h = 0;
        }
        leak(r);
	}
	
	public static int _3_bla(int a) {
        return a;
    }
//	
//	@EntryPoint
//	@Config(intWidth=2)
//	@MethodInvocationHandlersToUse
//	@ShouldLeak(exactly=2)
//	public static void nestedFunctionCalls(@Source @Value("0buu") int h) {
//		leak(_4_f(h));
//	}
//	
//	public static int _4_f(int a) {
//		return _4_h(a);
//	}
//	
//	public static int _4_h(int a) {
//		return _4_g(a);
//	}
//	
//	public static int _4_g(int a) {
//		return a;
//	}
//	
//	@EntryPoint
//	@Config(intWidth=3)
//	@MethodInvocationHandlersToUse
//	@ShouldLeak(exactly=3)
//	public static void conditionalRecursion(@Source @Value("0buuu") int h) {
//		leak(_5_f(h, 0, 0, 0, 0, 4));
//	}
//	
//	public static int _5_f(int x, int y, int z, int w, int v, int l) {
//         int r = 0;
//         if (l == 0) {
//            r = v;
//         } else {
//            r = _5_f(0, x, y, z, w, l+0b111);
//         }
//         return r;
//     }
//	
//	@EntryPoint
//	@Config(intWidth=2)
//	@MethodInvocationHandlersToUse
//	@ShouldLeak(exactly=2)
//	public static void nestedFunctionCalls2(@Source @Value("0buu") int h) {
//		leak(_4_f2(h));
//	}
//	
//	public static int _4_f2(int a) {
//		return _4_h2(a) & _4_h2(a) & _4_h2(a);
//	}
//	
//	public static int _4_h2(int a) {
//		return _4_g2(a) & _4_g2(a) & _4_g2(a);
//	}
//	
//	public static int _4_g2(int a) {
//		return _4_i2(a) & _4_i2(a) & _4_i2(a);
//	}
//	
//	public static int _4_i2(int a) {
//		return a;
//	}
	
	//@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=1, bits="0buu")
	public static void testBasicLoopNested(@Source @Value("0b0u") int h, @Source(level="l") @Value("0b0u") boolean l) {
	    int x = 0; 
		while (l){
	        while (l){
	            x = h;
	        }
	     }
	     leak(x);
	}
	
	
	//@EntryPoint
	@Config(intWidth=32)
	@ShouldLeak(exactly=7)
	public static void purseReduced2(@Source @Value("0b0uuuuuuu") int h) {
		int O = 0;
		while (h < 100){
		    h = h + 1;
		    O = O + 1;
		}
		leak(O);
	}
	
	
	//@EntryPoint
	@Config(intWidth=32)
	@ShouldLeak(exactly=7)
	public static void purseReduced(@Source @Value("0b0uuuuuuu") int h) {
		int O = 0;
		while (h <= 100){
		    h = h + 1;
		    O = O + 1;
		}
		leak(O);
	}
	
	//@EntryPoint
	@Config(intWidth=2)
	@ShouldLeak(exactly=2)
	public static void concLoopCond(@Source @Value("0buu") int h) {
		int O = 0;
		while (h == 0 | h == 0){
		    O = O + 1;
		}
		leak(O);
	}
	
	//@EntryPoint
	@Config(intWidth=10)
	@ShouldLeak(exactly=5)
	public static void maskedCopy(@Source int h) {
		leak(h | 0b11111);
	}
	
	//@EntryPoint 
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

	//@EntryPoint 
	@Config(intWidth=32)
	@ShouldLeak(atLeast=1)
	public static void binsearch16(@Source int I){
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
	
	//@EntryPoint 
	@Config(intWidth=32)
	@ShouldLeak(atLeast=1)
	public static void binsearch16reduced(@Source int I){
		int O = 0;
		int i = 0;
		while  (i < 2) {
			if (O != I) {
				O =  1;
			}
			i = i+1;
		}
		leak(O);
	}

	//@EntryPoint 
	@Config(intWidth=32)
	@ShouldLeak(exactly=2)
	public static void test2(@Source(level=Level.HIGH) int h){
	int om = (int)(0b00000000000000000000000000000001);
	if ((h==0b00000000000000000000000000000001)) {
	om = (int)0b00000000000000000000000000000001;
	} else {

	}

	if ((h==0b00000000000000000000000000000010)) {
	om = (int)0b00000000000000000000000000000010;
	} else {

	}

	if ((h==0b00000000000000000000000000000011)) {
	om = (int)0b00000000000000000000000000000011;
	} else {

	}

	if ((h==0b00000000000000000000000000000100)) {
	om = (int)0b00000000000000000000000000000100;
	} else {

	}

	leak(om);}
	//@EntryPoint 
	@Config(intWidth=32)
	@ShouldLeak(exactly=5)
	public static void test5(@Source(level=Level.HIGH) int h){
		int om = (int)(0b00000000000000000000000000000001);
		if ((h==0b00000000000000000000000000000001)) {
		om = (int)0b00000000000000000000000000000000;
		} else {

		}

		if ((h==0b00000000000000000000000000000010)) {
		om = (int)0b00000000000000000000000000000001;
		} else {

		}

		if ((h==0b00000000000000000000000000000011)) {
		om = (int)0b00000000000000000000000000000010;
		} else {

		}

		if ((h==0b00000000000000000000000000000100)) {
		om = (int)0b00000000000000000000000000000011;
		} else {

		}

		if ((h==0b00000000000000000000000000000101)) {
		om = (int)0b00000000000000000000000000000100;
		} else {

		}

		if ((h==0b00000000000000000000000000000110)) {
		om = (int)0b00000000000000000000000000000101;
		} else {

		}

		if ((h==0b00000000000000000000000000000111)) {
		om = (int)0b00000000000000000000000000000110;
		} else {

		}

		if ((h==0b00000000000000000000000000001000)) {
		om = (int)0b00000000000000000000000000000111;
		} else {

		}

		if ((h==0b00000000000000000000000000001001)) {
		om = (int)0b00000000000000000000000000001000;
		} else {

		}

		if ((h==0b00000000000000000000000000001010)) {
		om = (int)0b00000000000000000000000000001001;
		} else {

		}

		if ((h==0b00000000000000000000000000001011)) {
		om = (int)0b00000000000000000000000000001010;
		} else {

		}

		if ((h==0b00000000000000000000000000001100)) {
		om = (int)0b00000000000000000000000000001011;
		} else {

		}

		if ((h==0b00000000000000000000000000001101)) {
		om = (int)0b00000000000000000000000000001100;
		} else {

		}

		if ((h==0b00000000000000000000000000001110)) {
		om = (int)0b00000000000000000000000000001101;
		} else {

		}

		if ((h==0b00000000000000000000000000001111)) {
		om = (int)0b00000000000000000000000000001110;
		} else {

		}

		if ((h==0b00000000000000000000000000010000)) {
		om = (int)0b00000000000000000000000000001111;
		} else {

		}

		if ((h==0b00000000000000000000000000010001)) {
		om = (int)0b00000000000000000000000000010000;
		} else {

		}

		if ((h==0b00000000000000000000000000010010)) {
		om = (int)0b00000000000000000000000000010001;
		} else {

		}

		if ((h==0b00000000000000000000000000010011)) {
		om = (int)0b00000000000000000000000000010010;
		} else {

		}

		if ((h==0b00000000000000000000000000010100)) {
		om = (int)0b00000000000000000000000000010011;
		} else {

		}

		if ((h==0b00000000000000000000000000010101)) {
		om = (int)0b00000000000000000000000000010100;
		} else {

		}

		if ((h==0b00000000000000000000000000010110)) {
		om = (int)0b00000000000000000000000000010101;
		} else {

		}

		if ((h==0b00000000000000000000000000010111)) {
		om = (int)0b00000000000000000000000000010110;
		} else {

		}

		if ((h==0b00000000000000000000000000011000)) {
		om = (int)0b00000000000000000000000000010111;
		} else {

		}

		if ((h==0b00000000000000000000000000011001)) {
		om = (int)0b00000000000000000000000000011000;
		} else {

		}

		if ((h==0b00000000000000000000000000011010)) {
		om = (int)0b00000000000000000000000000011001;
		} else {

		}

		if ((h==0b00000000000000000000000000011011)) {
		om = (int)0b00000000000000000000000000011010;
		} else {

		}

		if ((h==0b00000000000000000000000000011100)) {
		om = (int)0b00000000000000000000000000011011;
		} else {

		}

		if ((h==0b00000000000000000000000000011101)) {
		om = (int)0b00000000000000000000000000011100;
		} else {

		}

		if ((h==0b00000000000000000000000000011110)) {
		om = (int)0b00000000000000000000000000011101;
		} else {

		}

		if ((h==0b00000000000000000000000000011111)) {
		om = (int)0b00000000000000000000000000011110;
		} else {

		}

		if ((h==0b00000000000000000000000000100000)) {
		om = (int)0b00000000000000000000000000011111;
		} else {

		}

		leak(om);}
	
	//@EntryPoint @Config(intWidth=6)
	@MethodInvocationHandlersToUse
	@ShouldLeak(exactly=1)
	public static void fib_3_p(@Source(level=Level.HIGH) int h){
		int z = 0;
		if (((h & 0b00100)>2)) {
		z = 2;
		} else {

		}
	leak(z);}
	
	public static int _fib_3(int num){
	int r = (int)(1);
	if ((num>2)) {
	r = 2;
	} else {

	}

	return (int)(r);
	}
	
	//@EntryPoint
	@Config(intWidth=32)
	@MethodInvocationHandlersToUse("all")
	@ShouldLeak(exactly=0)
	public static void weirdLoopFunctionTermination2(@Source int h) {
	    int res = 0; 
		while (h < func(h) || h == 0) {
			res = res + 1;
		}
	}
	
	public static int func(int a) {
		return a;
    }
	
	@EntryPoint @Config(intWidth=32)
	@MethodInvocationHandlersToUse("inlining")
	@ShouldLeak(exactly=0)
	public static void program(@Source(level=Level.HIGH) int h){
	int z = (int)(fib22((h & 31)));
	leak(z);}
	public static int fib22(int num){
	int r = (int)(0b00000000000000000000000000000001);
	if ((r>0b00000000000000000000000000000010)) {
	r = (int)(fib((num - 1))+fib22((num - 2)));
	} else {

	}

	return (int)(r);
	}

}
