package edu.kit.joana.api.annotations;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.ibm.wala.shrikeCT.AnnotationsReader;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.annotations.Annotation;
import edu.kit.joana.api.sdg.SDGProgramPart;
import edu.kit.joana.ifc.sdg.util.JavaType;

/**
 * Manage the mapping of identified program parts.
 *
 * Important: requires that [program part ↔ id] is a bisection
 */
public class IdManager {
  private final BiMap<String, SDGProgramPart> identifiedProgramParts;

  private final TypeName idTypeName;

  public IdManager(){
    this.identifiedProgramParts = HashBiMap.create();
    this.idTypeName = TypeName.findOrCreate(JavaType.parseSingleTypeFromString("edu.kit.joana.ui.annotations.Id").toBCString(false));
  }

  public void clear(){
    identifiedProgramParts.clear();
  }

  /**
   * @return is annotation an {@link edu.kit.joana.ui.annotations.Id} annotation?
   */
  public boolean put(SDGProgramPart programPart, Annotation annotation){
    if (annotation.getType().getName().equals(idTypeName)){
      String value;
      if (annotation.getUnnamedArguments() != null){
        value = (String) annotation.getUnnamedArguments()[0].snd;
      } else {
        value = (String) ((AnnotationsReader.ConstantElementValue)annotation.getNamedArguments().get("value")).val;
      }
      put(programPart, value);
      return true;
    }
    return false;
  }

  /**
   * @throws AssertionError if a program part is added again with a differing id (or vise versa)
   */
  public void put(SDGProgramPart programPart, String id){
    if (contains(id) && !get(id).equals(programPart)){
      throw new AssertionError(String.format("id %s is already associated with %s", id, get(id)));
    }
    if (contains(programPart) && !get(programPart).equals(id)){
      throw new AssertionError(String.format("%s is already associated with id %s", id, get(programPart)));
    }
    identifiedProgramParts.put(id, programPart);
  }

  public SDGProgramPart get(String id){
    return identifiedProgramParts.get(id);
  }

  public String get(SDGProgramPart programPart){
    return identifiedProgramParts.inverse().get(programPart);
  }

  public boolean contains(String id){
    return identifiedProgramParts.containsKey(id);
  }

  public boolean contains(SDGProgramPart programPart){
    return identifiedProgramParts.containsValue(programPart);
  }

  public IdManager immutable(){
    return new IdManager(){
      public SDGProgramPart get(String id){
        return IdManager.this.get(id);
      }

      public String get(SDGProgramPart programPart){
        return IdManager.this.get(programPart);
      }

      public boolean contains(String id){
        return IdManager.this.contains(id);
      }

      public boolean contains(SDGProgramPart programPart){
        return IdManager.this.contains(programPart);
      }
    };
  }
}
