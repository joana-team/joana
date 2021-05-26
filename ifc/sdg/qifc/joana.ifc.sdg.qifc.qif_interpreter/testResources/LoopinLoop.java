import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class LoopinLoop {

	public  static void main(String[] args) {

		LoopinLoop if_ = new LoopinLoop();
		if_.f(0, 1);

	}

	public int f(int h1, int h2) {
		int l = 0;
		while (h1 > 0) {
			while (h2 > 0) {
				l++;
				h2--;
			}
			h1--;
			l++;
		}
		Out.print(l);
		return l;
	}

}

/*
Nildumu AST:

use_sec basic;
bit_width 32;
(int, int) loop_method5_4(int h2, int l){
  int l3; int h23; int l2; int h22; int h21; int l1;
  if ((h2 > 0))
    {
      l1 = (l + 1);
      h21 = (h2 - 1);
      h22, l2 = *loop_method5_4(*(h21, l1));
    }
  h23 = phi(h22, h2);
  l3 = phi(l2, l);
  return (h23, l3);
}

(int, int, int) loop_method4_0(int h1, int h2, int l){
  int l4; int h23; int h13; int l3; int h22; int h12; int l2; int h11; int l1; int h21;
  if ((h1 > 0))
    {
      h21, l1 = *loop_method5_4(*(h2, l));
      h11 = (h1 - 1);
      l2 = (l1 + 1);
      h12, h22, l3 = *loop_method4_0(*(h11, h21, l2));
    }
  h13 = phi(h12, h1);
  h23 = phi(h22, h2);
  l4 = phi(l3, l);
  return (h13, h23, l4);
}
int l1; int h21; int h11;
h input int h1 = 0buuu;
h input int h2 = 0buuu;
int l = 0;
h11, h21, l1 = *loop_method4_0(*(h1, h2, l));
l output int o = l1;

 */