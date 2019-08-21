package edu.kit.joana.ui.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to set the value of a parameter, the method return value (when annotating a method)
 * or a field.
 */
@Target({ElementType.PARAMETER, ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.CLASS)
public @interface SetValue {
    
    String[] tags() default {};

    /**
     * Supported are currently primitive data types and strings.
     */
    String value() default "";

    /**
     * Use the value of a field of a class.
     *
     * Accessing non static fields is only supported non static fields and methods, the field
     * syntax is just the field name.
     *
     * Accessing static fields is supported via <code>pkg1/pkg2/…/className[$innerClass]#fieldName</code>,
     * static fields of the own class can be accessed via <code>.#fieldName</code>
     */
    String field() default "";
}
