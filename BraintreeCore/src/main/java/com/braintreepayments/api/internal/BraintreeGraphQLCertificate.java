package com.braintreepayments.api.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import javax.net.ssl.SSLException;

class BraintreeGraphQLCertificate {

    static InputStream getCertInputStream() throws SSLException {
        try {
            return new ByteArrayInputStream(GraphQLConstants.CERTIFICATE.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new SSLException(e.getMessage());
        }
    }
}
