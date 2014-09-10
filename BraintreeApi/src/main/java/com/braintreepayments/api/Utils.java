package com.braintreepayments.api;

import com.google.gson.Gson;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

    protected static <T> Set<T> newHashSet() {
        return new HashSet<T>();
    }

    protected static <T> List<T> newLinkedList() {
        return new LinkedList<T>();
    }
}
