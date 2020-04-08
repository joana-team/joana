package edu.kit.joana.ui.annotations;

import java.lang.annotation.*;

/**
 * Can be placed on program part to give them an id
 */
@Retention(RetentionPolicy.CLASS)
@Target({ ElementType.FIELD,ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE})
public @interface Id {
  /**
   * Id of the annotated program part
   */
  String value();
}
