package edu.kit.joana.wala.eval;

public class Example {

	static class Data {
		Node n1 = new Node();
		Node n2 = new Node();
	}
	
	static class Node {
		int i = 1;
		Node next;
	}
	
	static Node sum(Node n) {
		int sum = 0;
		while (n != null) {
			sum += n.i;
			n = n.next;
		}
		Node res = new Node();
		res.i = sum;
		return res;
	}
	
	static int sumData(Data d) {
		Node sum1 = sum(d.n1);
		Node sum2 = sum(d.n2);
		return sum1.i + sum2.i;
	}
	
	static void append(Node node, int len) {
		for (int i = 0; i < len; i++) {
			node.next = new Node();
			node = node.next;
		}
	}
	
	static void main1() {
		Data d = new Data();
		append(d.n1, 4);
		d.n2 = d.n1;
		int sum = sumData(d);
		println(sum);
	}
	
	static void println(int i) {
		System.out.println("i = " + i);
	}
	
	public static void main(String[] argv) {
		main1();
	}
}
