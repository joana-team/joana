import edu.kit.joana.ui.annotations.*;

public class Basic {
	
	public static void main(String[] args) {
		new Basic().blub(4);
	}
	
	@EntryPoint(tag="bla", pruningPolicy=PruningPolicy.DO_NOT_PRUNE)
	@Source(tags="bla")
	public void bla(@SetValue(value="3",tags="bla") int bla) {
		blub(bla);
	}

    public void blub(@Sink int a) {
    	
    }
}
