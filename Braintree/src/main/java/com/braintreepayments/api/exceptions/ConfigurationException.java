package com.braintreepayments.api.exceptions;

/**
 * Error class thrown when a configuration value is invalid
 */
public class ConfigurationException extends BraintreeException {
    public ConfigurationException(String message) {
        super(message);
    }
}
