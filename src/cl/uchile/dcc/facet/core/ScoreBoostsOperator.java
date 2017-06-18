package cl.uchile.dcc.facet.core;

import java.util.function.DoubleUnaryOperator;

public class ScoreBoostsOperator implements DoubleUnaryOperator{

    @Override
    public double applyAsDouble(double operand) {
        double min = 6.641535269908322E-9;
        double factor = 1d / min;
        return Math.log(operand * factor) + 1;
    }
}
