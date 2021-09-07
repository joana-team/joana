package reflection;

import com.google.common.reflect.TypeToken;
import edu.kit.joana.ui.annotations.EntryPoint;

import java.lang.reflect.Type;
import java.util.List;

public class Reflection {

  class Pet {

  }

  public List<Pet> func() {
    return null;
  }

  @EntryPoint
  public static void main(String[] args) {
    new Reflection().func();
    Type localVarReturnType = new TypeToken<Pet>(){}.getType();
  }
}
