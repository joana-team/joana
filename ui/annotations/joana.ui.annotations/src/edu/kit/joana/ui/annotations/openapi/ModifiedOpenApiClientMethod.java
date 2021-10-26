package edu.kit.joana.ui.annotations.openapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tag methods that are modified OpenAPI client methods.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.METHOD })
public @interface ModifiedOpenApiClientMethod {
}