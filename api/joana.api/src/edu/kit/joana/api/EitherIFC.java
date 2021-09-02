package edu.kit.joana.api;

import edu.kit.joana.ifc.sdg.core.IFC;
import edu.kit.joana.ifc.sdg.core.SecurityNode;
import edu.kit.joana.ifc.sdg.core.violations.IViolation;
import edu.kit.joana.ifc.sdg.graph.SDG;
import edu.kit.joana.ifc.sdg.lattice.IStaticLattice;
import edu.kit.joana.ifc.sdg.lattice.NotInLatticeException;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Wrapper around two different IFC analyses that are chosen depending on a predicate
 */
public class EitherIFC<T> extends IFC<T> {
  private final Predicate<SDG> useFirst;
  private final BiFunction<SDG, IStaticLattice<T>, IFC<T>> first;
  private final BiFunction<SDG, IStaticLattice<T>, IFC<T>> second;
  private IFC<T> ifc;

  public EitherIFC(SDG sdg, IStaticLattice<T> lattice, Predicate<SDG> useFirst,
      BiFunction<SDG, IStaticLattice<T>, IFC<T>> first,
      BiFunction<SDG, IStaticLattice<T>, IFC<T>> second) {
    super(sdg, lattice);
    this.useFirst = useFirst;
    this.first = first;
    this.second = second;
    updateIFC();
  }

  private void updateIFC() {
    ifc = (useFirst.test(getSDG()) ? first : second).apply(getSDG(), getLattice());
    getProgressListeners().forEach(ifc::addProgressListener);
  }

  @Override public void setSDG(SDG sdg) {
    super.setSDG(sdg);
    updateIFC();
  }

  @Override public Collection<? extends IViolation<SecurityNode>> checkIFlow() throws NotInLatticeException {
    return ifc.checkIFlow();
  }

  static boolean containsDeclassifications(SDG sdg) {
    return sdg.vertexSet().stream().anyMatch(n -> ((SecurityNode)n).isDeclassification());
  }
}
