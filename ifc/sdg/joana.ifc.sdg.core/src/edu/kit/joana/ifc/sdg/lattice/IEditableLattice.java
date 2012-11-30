/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.ifc.sdg.lattice;


/**
 * Interface for a editable lattice. Extends <code>IStaticLattice</code> and
 * <code>ILatticeOperations</code>.
 *
 * @param <ElementType>
 *            the type of the elements contained in the lattice.
 */
public interface IEditableLattice<ElementType> extends IStaticLattice<ElementType>, ILatticeOperations<ElementType> {
	/**
	 * Adds a element to the lattice.
	 *
	 * @param element
	 *            the element to add.
	 */
	public void addElement(ElementType element);

	/**
	 * Removes a element from the lattice along with all its relations to other
	 * elements.
	 *
	 * @param element
	 *            the element to remove.
	 * @throws NotInLatticeException
	 *             if <code>element</code> is not in the lattice.
	 */
	public void removeElement(ElementType element) throws NotInLatticeException;

	/**
	 * Sets one element immediately lower as another.
	 *
	 * @param element
	 *            the element to set the immediately lower for.
	 * @param lower
	 *            the immediately lower element of <code>element</code>
	 * @throws NotInLatticeException
	 *             if either <code>element</code> or <code>lower</code> are
	 *             not in the lattice.
	 */
	public void setImmediatelyLower(ElementType element, ElementType lower) throws NotInLatticeException;

	/**
	 * Unsets a immediately-lower relation for two elements
	 *
	 * @param element
	 *            the element to remove a immediately-lower relation from
	 * @param lower
	 *            the immediately lower element to remove the relation for
	 * @throws NotInLatticeException
	 *             if either <code>element</code> or <code>lower</code> are
	 *             not in the lattice.
	 */
	public void unsetImmediatelyLower(ElementType element, ElementType lower) throws NotInLatticeException;

	/**
	 * Sets one element immediately greater as another.
	 *
	 * @param element
	 *            the element to set the immediately greater for.
	 * @param greater
	 *            the immediately greater element of <code>element</code>
	 * @throws NotInLatticeException
	 *             if either <code>element</code> or <code>greater</code>
	 *             are not in the lattice.
	 */
	public void setImmediatelyGreater(ElementType element, ElementType greater) throws NotInLatticeException;

	/**
	 * Unsets a immediately-greater relation for two elements
	 *
	 * @param element
	 *            the element to remove a immediately-greater relation from
	 * @param greater
	 *            the immediately greater element to remove the relation for
	 * @throws NotInLatticeException
	 *             if either <code>element</code> or <code>greater</code>
	 *             are not in the lattice.
	 */
	public void unsetImmediatelyGreater(ElementType element, ElementType greater) throws NotInLatticeException;

}
