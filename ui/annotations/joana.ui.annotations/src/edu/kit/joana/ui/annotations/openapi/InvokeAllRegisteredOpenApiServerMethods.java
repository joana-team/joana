package edu.kit.joana.ui.annotations.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an auto generated method that calls registered open api methods
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface InvokeAllRegisteredOpenApiServerMethods {
}
