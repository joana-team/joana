/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package joana.api.testdata.seq;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.SECRET;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.leak;
import static edu.kit.joana.api.annotations.ToyTestsDefaultSourcesAndSinks.toggle;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;



/**
 * @author Martin Hecker <martin.hecker@kit.edu>
 */
public class ReflectiveMethodCall {
	static interface I {
		public void foo();
	}

	static class AA implements I {
		private final int x;
		AA(Integer x) {
			this.x = x;
		}
		@Override
		public void foo() {
			leak(toggle(SECRET));
		}
	}


	static class BB implements I {
		private final int y;
		BB(Integer y) {
			this.y = y;
		}
		
		@Override
		public void foo() {
		}
	}

	
	public static void main(String[] args) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		final boolean unknown = args.length > 0;
		
		final Class<? extends I> clazz;
		//if (unknown) {
			clazz = AA.class;
		//} else {
//			clazz = BB.class;
		//}
			
		
		final Method method  = clazz.getMethod("foo", new Class[0]);
		
		final AA aa = new AA(7);
		method.invoke(aa);
	}
}

