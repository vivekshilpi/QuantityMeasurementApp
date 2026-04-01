package com.app.quantitymeasurement.exception;

/*
 * =========================================================
 * QuantityMeasurementException
 * =========================================================
 *
 * UC15 – Custom Exception Layer
 *
 * Purpose:
 * Represents domain-specific errors in the
 * Quantity Measurement system.
 *
 * Examples:
 * - Invalid unit conversion
 * - Unsupported arithmetic operation
 * - Cross-category operation
 *
 * Using a custom exception allows the application
 * to handle errors consistently across all layers.
 */


public class QuantityMeasurementException extends RuntimeException {

    public QuantityMeasurementException(String message) {
        super(message);
    }

    public QuantityMeasurementException(String message, Throwable cause) {
        super(message, cause);
    }
} 