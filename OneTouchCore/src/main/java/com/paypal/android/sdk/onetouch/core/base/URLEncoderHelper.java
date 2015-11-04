package com.paypal.android.sdk.onetouch.core.base;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class URLEncoderHelper {
    public static String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "unable_to_encode:" + s;
        }
    }
}
