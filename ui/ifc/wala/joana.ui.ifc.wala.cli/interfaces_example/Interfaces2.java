import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class Interfaces2 {

  private Base a;


  @EntryPoint(tag = "1")
  @EntryPoint(tag = "2")
  @Source(tags = {"1", "2"})
  public void main() {
    a.f();
  }

  public static abstract class Base {
    public abstract void f();
  }

  public static class A extends Base {


    @Sink(tags = "1")
    public void f() {
    }

  }
  public static class B extends Base {

    @Sink(tags = "2")
    @Override public void f() {
    }
  }

}


