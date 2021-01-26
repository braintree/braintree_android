package com.braintreepayments.api;

/**
 * Error class thrown when a configuration value is invalid
 */
public class ConfigurationException extends BraintreeException {
    
    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public ConfigurationException(String message) {
        super(message);
    }
}
