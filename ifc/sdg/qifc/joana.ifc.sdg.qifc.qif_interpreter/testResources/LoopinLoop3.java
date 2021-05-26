import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class LoopinLoop3 {

	public  static void main(String[] args) {

		LoopinLoop3 if_ = new LoopinLoop3();
		if_.f(0, 1);

	}

	public int f(int h1, int h2) {
		int l = 0;
		while (h1 > 0) {
			h1--;
			l++;
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
(int, int) loop_method7_4(int h2, int l){
  int l3; int h23; int l2; int h22; int h21; int l1;
  if ((h2 > 0))
    {
      l1 = (l + 1);
      h21 = (h2 - 1);
      h22, l2 = *loop_method7_4(*(h21, l1));
    }
  h23 = phi(h22, h2);
  l3 = phi(l2, l);
  return (h23, l3);
}

(int, int, int) loop_method4_0(int h2, int h1, int l){
  int l5; int h23; int h14; int l4; int h13; int h22; int l3; int h12; int l2; int h21; int l1; int h11;
  if ((h1 > 0))
    {
      h11 = (h1 - 1);
      l1 = (l + 1);
      h21, l2 = *loop_method7_4(*(h2, l1));
      h12 = (h11 - 1);
      l3 = (l2 + 1);
      h22, h13, l4 = *loop_method4_0(*(h21, h12, l3));
    }
  h14 = phi(h13, h1);
  h23 = phi(h22, h2);
  l5 = phi(l4, l);
  return (h23, h14, l5);
}
int l1; int h21; int h11;
h input int h1 = 0buuu;
h input int h2 = 0buuu;
int l = 0;
h21, h11, l1 = *loop_method4_0(*(h2, h1, l));
l output int o = l1;


 */