package com.braintreepayments.api;

import java.net.HttpURLConnection;

interface HTTPResponseParser {
    String parse(int responseCode, HttpURLConnection connection) throws Exception;
}
