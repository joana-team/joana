package reflection;

import com.google.common.reflect.TypeToken;
import edu.kit.joana.ui.annotations.EntryPoint;

import java.lang.reflect.Type;

public class Reflection {

  class Pet {

  }

  @EntryPoint
  public static void main(String[] args) {
    Type localVarReturnType = new TypeToken<Pet>(){}.getType();
  }
}
