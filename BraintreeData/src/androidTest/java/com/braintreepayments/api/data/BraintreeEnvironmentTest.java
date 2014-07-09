package com.braintreepayments.api.data;

import com.braintreepayments.api.data.BraintreeEnvironment;

import junit.framework.TestCase;

public class BraintreeEnvironmentTest extends TestCase {

    public void testGetMerchantIdReturnsCorrectMerchantId() {
        assertEquals("600000", BraintreeEnvironment.QA.getMerchantId());
        assertEquals("600000", BraintreeEnvironment.SANDBOX.getMerchantId());
        assertEquals("600000", BraintreeEnvironment.PRODUCTION.getMerchantId());
    }

    public void testGetCollectorUrlReturnsCorrectUrl() {
        assertEquals("https://assets.qa.braintreegateway.com/data/logo.htm", BraintreeEnvironment.QA.getCollectorUrl());
        assertEquals("https://assets.braintreegateway.com/sandbox/data/logo.htm", BraintreeEnvironment.SANDBOX.getCollectorUrl());
        assertEquals("https://assets.braintreegateway.com/data/logo.htm", BraintreeEnvironment.PRODUCTION.getCollectorUrl());
    }
}
