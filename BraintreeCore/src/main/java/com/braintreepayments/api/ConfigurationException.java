package com.braintreepayments.api;

/**
 * Error class thrown when a configuration value is invalid
 */
public class ConfigurationException extends BraintreeException {
    
    ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
    
    ConfigurationException(String message) {
        super(message);
    }
}
