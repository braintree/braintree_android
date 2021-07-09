package com.braintreepayments.api;

import java.net.HttpURLConnection;

interface HttpResponseParser {
    String parse(int responseCode, HttpURLConnection connection) throws Exception;
}
