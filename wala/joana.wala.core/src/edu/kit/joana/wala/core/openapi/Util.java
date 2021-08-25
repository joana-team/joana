package edu.kit.joana.wala.core.openapi;

/**
 * Utility class for working with open api
 */
public class Util {

  public static OpenApiClientDetector getDetector(String openApiPackage) {
    return new OpenApiClientDetector(openApiPackage);
  }

}
