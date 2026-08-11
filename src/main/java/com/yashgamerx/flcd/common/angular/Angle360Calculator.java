package com.yashgamerx.flcd.common.angular;

public class Angle360Calculator implements AngularCalculator {
    @Override
    public double calculate(int total) {
        return (2.0 * Math.PI) / total;
    }
}
