package edu.kit.joana.ifc.sdg.core.violations;

public class UnaryViolation<T,L> implements IUnaryViolation<T,L> {

	private static final String template = "Violation at %s: user-annotated level is %s, computed level is %s";

	private final T node;
	private final L expected;
	private final L actual;

	public UnaryViolation(T node, L expected, L actual) {
		this.node = node;
		this.expected = expected;
		this.actual = actual;
	}

	@Override
	public void accept(IViolationVisitor<T> v) {
		v.visitUnaryViolation(this);
	}

	@Override
	public T getNode() {
		return node;
	}

	@Override
	public L getExpectedLevel() {
		return expected;
	}

	@Override
	public L getActualLevel() {
		return actual;
	}

	@Override
	public String toString() {
		return String.format(template, getNode(), getExpectedLevel(), getActualLevel());
	}
}
