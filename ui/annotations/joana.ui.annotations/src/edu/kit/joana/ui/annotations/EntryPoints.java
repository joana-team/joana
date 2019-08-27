package edu.kit.joana.ui.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Collection of {@link EntryPoint} annotations.
 * 
 * <b>Don't use this annotation, just repeat the {@link EntryPoint} annotation.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD})
public @interface EntryPoints {
  
  EntryPoint[] value();

}
