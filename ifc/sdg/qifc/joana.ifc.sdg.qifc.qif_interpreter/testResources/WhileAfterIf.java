import edu.kit.joana.ifc.sdg.qifc.qif_interpreter.input.Out;

public class WhileAfterIf {

	public  static void main(String[] args) {

		WhileAfterIf if_ = new WhileAfterIf();
		if_.f(0);

	}

	public int f(int h) {
		int l = 0;
		if (h > 1) {
			l = 1;
		}
		while (h > 0) {
			l++;
			h--;
		}
		Out.print(l);
		return l;
	}
}

/*
Nildumu AST:

use_sec basic;
bit_width 32;
(int, int) loop_method6_0(int h, int l){
  int l3; int h3; int h2; int l2; int h1; int l1;
  if ((h > 0))
    {
      l1 = (l + 1);
      h1 = (h - 1);
      l2, h2 = *loop_method6_0(*(h1, l1));
    }
  h3 = phi(h2, h);
  l3 = phi(l2, l);
  return (l3, h3);
}
int l2; int l1; int l3; int h1;
h input int h = 0buuu;
int l = 0;
if ((h > 1))
  {
    l1 = 1;
  }
l2 = phi(l1, l);
l3, h1 = *loop_method6_0(*(h, l2));
l output int o = l3;

 */