import edu.kit.joana.ui.annotations.*;

/**
 * "classPath annotation_example" "optimization enable" "run bla"
 */
public class Basic2 {
	
	public static void main(String[] args) {
		new Basic2().blub(4);
	}
	
	@EntryPoint(tag="bla", pruningPolicy=PruningPolicy.DO_NOT_PRUNE)
	@Source(tags="bla")
	@JSONAnnotation("{\"name\": \"bla\"}")
	public void bla(@SetValue(value="3",tags="bla") int bla) {
		if (bla == 4) {
			blub(bla);
		}
	}

    public void blub(@Sink(tags="bla") int a) {
    	
    }
}
