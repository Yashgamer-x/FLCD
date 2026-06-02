package com.yashgamerx.flcd.service.angular;

public class Angle180Calculator implements AngularCalculator {
    @Override
    public double calculate(int total) {
        return Math.PI / total;
    }
}
