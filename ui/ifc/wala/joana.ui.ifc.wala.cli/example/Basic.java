import edu.kit.joana.ui.annotations.EntryPoint;
import edu.kit.joana.ui.annotations.Level;
import edu.kit.joana.ui.annotations.Sink;
import edu.kit.joana.ui.annotations.Source;

public class Basic {
	
	public static void main(String[] args) {
		new Basic().blub(4);
	}
	
	@EntryPoint(tag="blub")
    @EntryPoint(tag="bla")
    @Source(tags="bla")
	public void bla() {
		blub(3);
	}
     
    @Sink(tags="bla")
    public void blub(@Sink int a) {
    	
    }
}
