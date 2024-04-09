package com.braintreepayments.api.sharedutils;

import androidx.annotation.RestrictTo;

import java.net.HttpURLConnection;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface HttpResponseParser {
    String parse(int responseCode, HttpURLConnection connection) throws Exception;
}
