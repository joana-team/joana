package edu.kit.joana.ui.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface EntryPoint {
	String[] levels() default { Level.LOW, Level.HIGH};
	MayFlow[] lattice() default { @MayFlow(from=Level.LOW, to=Level.HIGH) };
	String[] datasets() default {};
	String[] adversaries() default {};
	EntryPointKind kind() default EntryPointKind.UNKNOWN;
	String file() default "";
	PointsToPrecision pointsToPrecision() default PointsToPrecision.INSTANCE_BASED;
	ChopComputation chops() default ChopComputation.ALL;
	
}
