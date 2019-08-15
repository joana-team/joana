import edu.kit.joana.ui.annotations.*;

public class Basic {
	
	public static void main(String[] args) {
		new Basic().blub(4);
	}
	
	@EntryPoint(tag="blub")
    @EntryPoint(tag="bla", pruningPolicy=PruningPolicy.DO_NOT_PRUNE)
    @Source(tags="bla")
	@Source(tags="bla")
	@Declassification(tags="bla")
	public void bla() {
		blub(3);
	}
     
    @Sink(tags="bla")
    public void blub(@Sink int a) {
    	
    }
}
