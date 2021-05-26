import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class LoopInIf {

	public  static void main(String[] args) {

		LoopInIf if_ = new LoopInIf();
		if_.f(0, 1);

	}

	public int f(int h1, int h2) {
		int l = 0;
		if (h1 > 0) {
			while (h2 > 0) {
				l++;
				h2--;
			}
			h1 = 1;
		}
		Out.print(l);
		return l;
	}
}

/*
Nildumu AST:

use_sec basic;
bit_width 32;
(int, int) loop_method5_4(int l, int h2){
  int l3; int h23; int h22; int l2; int h21; int l1;
  if ((h2 > 0))
    {
      l1 = (l + 1);
      h21 = (h2 - 1);
      l2, h22 = *loop_method5_4(*(l1, h21));
    }
  h23 = phi(h22, h2);
  l3 = phi(l2, l);
  return (l3, h23);
}
int h11; int h21; int l1; int l2; int h22; int h12;
h input int h1 = 0buuu;
h input int h2 = 0buuu;
int l = 0;
if ((h1 > 0))
  {
    l1, h21 = *loop_method5_4(*(l, h2));
    h11 = 1;
  }
h12 = phi(h11, h1);
h22 = phi(h21, h2);
l2 = phi(l1, l);
l output int o = l2;

 */