package edu.kit.joana.wala.core.openapi;

import java.util.Objects;

/**
 * Id of an OpenAPI operation (with associated tag)
 */
public class OperationId {

  public static final String DEFAULT_API = "";

  /** normalized tag */
  public final String tag;

  public final String operationId;

  /** normalizes tag */
  public OperationId(String tag, String operationId) {
    this.tag = normalizeTag(tag);
    this.operationId = operationId;
  }

  @Override public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof OperationId))
      return false;
    OperationId that = (OperationId) o;
    return Objects.equals(tag, that.tag) && Objects.equals(operationId, that.operationId);
  }

  @Override public int hashCode() {
    return Objects.hash(tag, operationId);
  }

  public boolean hasDefaultTag() {
    return tag.isEmpty();
  }

  public static String uncapitalize(String word) {
    return word.substring(0, 1).toLowerCase() + word.substring(1);
  }

  public static String normalizeTag(String tag) {
    if (tag == null || tag.equals("") || tag.equals("DefaultApi") || uncapitalize(tag).equals("defaultApi")) {
      return "";
    }
    return uncapitalize(tag);
  }

  @Override public String toString() {
    return tag + "." + operationId;
  }
}
