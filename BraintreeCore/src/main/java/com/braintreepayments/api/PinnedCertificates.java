package com.braintreepayments.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLException;

class PinnedCertificates {

    static InputStream getCertInputStream() throws SSLException {
        return new ByteArrayInputStream(GraphQLConstants.CERTIFICATE.getBytes(StandardCharsets.UTF_8));
    }
}
