package edu.kit.joana.graph.dominators.slca;

public class Interval {
	private int l;
	private int r;
	public Interval(int l, int r) {
		assert l <= l;
		this.l = l;
		this.r = r;
	}
	public int getLeft() {
		return l;
	}
	public int getRight() {
		return r;
	}
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Interval)) return false;
		Interval i = (Interval) o;
		return this.l == i.l && this.r == i.r;
	}
	@Override
	public int hashCode() {
		int res = 1;
		res = 31 * res + this.l;
		res = 31 * res + this.r;
		return res;
	}
	@Override
	public String toString() {
		return String.format("[%d,%d]", l, r);
	}
	public boolean isContainedIn(Interval i) {
		return this.l >= i.l && this.r <= i.r;
	}
}
