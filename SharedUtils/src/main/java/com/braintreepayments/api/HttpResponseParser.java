package com.braintreepayments.api;

import java.net.HttpURLConnection;

public interface HttpResponseParser {
    String parse(int responseCode, HttpURLConnection connection) throws Exception;
}
