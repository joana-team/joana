package edu.kit.joana.ifc.sdg.core.violations;

public interface IUnaryViolation<T,L> extends IViolation<T> {
	public T getNode();
	public L getExpectedLevel();
	public L getActualLevel();
}
