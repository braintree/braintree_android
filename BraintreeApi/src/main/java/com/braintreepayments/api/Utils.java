package com.braintreepayments.api;

import com.google.gson.Gson;

public class Utils {
    private Utils() {
        throw new IllegalStateException("Non-instantiable class.");
    }

    private static Gson gson;

    public static Gson getGson() {
        if (gson == null) {
            gson = new Gson();
        }

        return gson;
    }
}
