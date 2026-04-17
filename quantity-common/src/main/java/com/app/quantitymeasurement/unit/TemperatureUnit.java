package com.app.quantitymeasurement.unit;

public enum TemperatureUnit implements IMeasurable {

    CELSIUS(false),
    FAHRENHEIT(false),
    KELVIN(false);

    private final boolean supportsArithmeticFlag;

    TemperatureUnit(boolean supportsArithmeticFlag) {
        this.supportsArithmeticFlag = supportsArithmeticFlag;
    }

    @Override
    public String getUnitName() {
        return this.name();
    }

    @Override
    public double getConversionFactor() {
        return 1.0;
    }

    @Override
    public double convertToBaseUnit(double value) {
        return switch (this) {
            case CELSIUS -> value;
            case FAHRENHEIT -> (value - 32) * 5 / 9;
            case KELVIN -> value - 273.15;
        };
    }

    @Override
    public double convertFromBaseUnit(double baseValue) {
        return switch (this) {
            case CELSIUS -> baseValue;
            case FAHRENHEIT -> (baseValue * 9 / 5) + 32;
            case KELVIN -> baseValue + 273.15;
        };
    }

    @Override
    public boolean supportsArithmetic() {
        return supportsArithmeticFlag;
    }

    @Override
    public void validateOperationSupport(String operation) {
        throw new UnsupportedOperationException(this.name() + " does not support " + operation + " operation.");
    }
}
