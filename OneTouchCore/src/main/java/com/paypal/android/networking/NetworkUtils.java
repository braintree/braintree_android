package com.paypal.android.networking;

import org.json.JSONException;

import java.util.Map;
import java.util.Map.Entry;

public class NetworkUtils {

    /**
     * Turns a set of params into a parameter string. Does NOT encode.
     *
     * @param body
     * @return
     * @throws JSONException
     */
    public static String urlFormat(Map<String, String> body) {
        StringBuilder sb = new StringBuilder();
        boolean isFirst = true;
        for (Entry<String, String> entry : body.entrySet()) {
            if (!isFirst) {
                sb.append("&");
            } else {
                isFirst = false;
            }
            sb.append(entry.getKey() + "=" + entry.getValue());
        }
        return sb.toString();
    }
}
