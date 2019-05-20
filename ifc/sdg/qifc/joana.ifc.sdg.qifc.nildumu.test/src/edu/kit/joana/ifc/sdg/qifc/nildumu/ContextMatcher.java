/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */

package edu.kit.joana.ifc.sdg.qifc.nildumu;

import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.bs;
import static edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.vl;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.function.Executable;

import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.B;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Sec;
import edu.kit.joana.ifc.sdg.qifc.nildumu.Lattices.Value;

/**
 * Matcher for the {@link Context} that is produced in the analysis
 */
public class ContextMatcher {

    public static class TestBuilder {
        List<Executable> testers = new ArrayList<>();


        public TestBuilder add(Executable tester){
            testers.add(tester);
            return this;
        }

        public void run(){
            assertAll(testers.toArray(new Executable[0]));
        }
    }

    private final Context context;
    private final TestBuilder builder = new TestBuilder();

    public ContextMatcher(Context context) {
        this.context = context;
    }

    public ContextMatcher val(String variable, int value){
        Value actual = getValue(variable);
        builder.add(() -> assertTrue(actual.isConstant(), String.format("Variable %s should have an integer val, has %s", variable, actual.repr())));
        builder.add(() -> assertEquals(value, actual.asInt(),
                String.format("Variable %s should have integer val %d", variable, value)));
        return this;
    }

    public ContextMatcher val(String variable, String value){
        Value expected = vl.parse(value);
        Value actual = getValue(variable);
        builder.add(() -> assertEquals(expected.toLiteralString(), actual.toLiteralString(),
                String.format("Variable %s should have val %s, has val %s", variable, expected.repr(), actual.repr())));
        return this;
    }

    private Value getValue(String variable){
        return context.getVariableValue(variable);
    }

    public ContextMatcher hasInput(String variable){
        builder.add(() -> assertTrue(context.isInputValue(getValue(variable)),
                String.format("The val of %s is an input val", variable)));
        return this;
    }

    public ContextMatcher hasInputSecLevel(String variable, Sec<?> expected){
        builder.add(() -> assertEquals(expected, context.getInputSecLevel(getValue(variable)), String.format("Variable %s should be an input variable with level %s", variable, expected)));
        return this;
    }

    public ContextMatcher hasOutput(String variable){
        builder.add(() -> assertTrue(context.output.contains(getValue(variable)),
                String.format("The val of %s is an output val", variable)));
        return this;
    }

    public ContextMatcher hasOutputSecLevel(String variable, Sec<?> expected){
        builder.add(() -> assertEquals(expected, context.output.getSec(getValue(variable)), String.format("Variable %s should be an output variable with level %s", variable, expected)));
        return this;
    }

    public ContextMatcher leakage(Consumer<LeakageMatcher> leakageTests){
        leakageTests.accept(new LeakageMatcher());
        return this;
    }

    public ContextMatcher custom(Consumer<Context> test) {
    	builder.add(() -> test.accept(context));
    	return this;
    }
    
    public class LeakageMatcher {

        public LeakageMatcher leaks(Sec<?> attackerSec, int leakage){
            builder.add(() -> {
                MinCut.ComputationResult comp = MinCut.compute(context, attackerSec);
                assertEquals(leakage, comp.maxFlow, () -> {
                    return String.format("The calculated leakage for an attacker of level %s should be %d, leaking %s", attackerSec, leakage, comp.minCut.stream().map(Lattices.Bit::toString).collect(Collectors.joining(", ")));
                });
            });
            return this;
        }

        public LeakageMatcher leaks(String attackerSec, int leakage){
            return leaks(context.sl.parse(attackerSec), leakage);
        }

        public LeakageMatcher leaksAtLeast(Sec sec, int leakage) {
            builder.add(() -> {
                MinCut.ComputationResult comp = MinCut.compute(context, sec);
                assertTrue(comp.maxFlow >= leakage, String.format("The calculated leakage for an attacker of level %s should be at least %d, leaking %d", sec, leakage, comp.maxFlow));
            });
            return this;
        }
        
        public LeakageMatcher leaksAtMost(Sec sec, int leakage) {
            builder.add(() -> {
                MinCut.ComputationResult comp = MinCut.compute(context, sec);
                assertTrue(comp.maxFlow <= leakage, String.format("The calculated leakage for an attacker of level %s should be at most %d, leaking %d", sec, leakage, comp.maxFlow));
            });
            return this;
        }
    }

    public ContextMatcher val(String var, Consumer<ValueMatcher> test){
        test.accept(new ValueMatcher(context.getVariableValue(var)));
        return this;
    }

    public ContextMatcher leaks(String attackerSec, int leakage){
        return leakage(l -> l.leaks(attackerSec, leakage));
    }

    public ContextMatcher leaks(int leakage){
        return leakage(l -> l.leaks(context.sl.bot(), leakage));
    }

    public ContextMatcher leaksAtLeast(int leakage){
        return leakage(l -> l.leaksAtLeast(context.sl.bot(), leakage));
    }
    
    public ContextMatcher leaksAtMost(int leakage){
        return leakage(l -> l.leaksAtMost(context.sl.bot(), leakage));
    }

    public ContextMatcher bitWidth(int bitWidth){
        builder.add(() -> assertEquals(bitWidth, context.maxBitWidth, "Check of the used maximum bit width"));
        return this;
    }

    /**
     *
     * @param varAndIndex "var[1]"
     * @param val
     * @return
     */
    public ContextMatcher bit(String varAndIndex, String val){
        String var = varAndIndex.split("\\[")[0];
        int i = Integer.parseInt(varAndIndex.split("\\[")[1].split("\\]")[0]);
        builder.add(() -> assertEquals(bs.parse(val), context.getVariableValue(var).get(i).val(), String.format("%s should have the bit val %s", varAndIndex, val)));
        return this;
    }

    /**
     *
     * @param varIndexVals "var[1] = 1; a[3] = 1"
     */
    public ContextMatcher bits(String varIndexVals){
        if (!varIndexVals.contains("=")){
            return this;
        }
        Stream.of(varIndexVals.split(";")).forEach(str -> {
            String[] parts = str.split("=");
            bit(parts[0].trim(), parts[1].trim());
        });
        return this;
    }

    public class ValueMatcher {
        private final Value value;

        public ValueMatcher(Value value) {
            this.value = value;
        }

        public ValueMatcher bit(int i, B val){
            builder.add(() -> assertEquals(val, value.get(i).val(), String.format("The %dth bit of %s should have the bit val %s", i, value, val)));
            return this;
        }
    }

    public void run(){
        builder.run();
    }
}
