package com.braintreepayments.api;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

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
            gson = new GsonBuilder()
                    .addSerializationExclusionStrategy(new ExclusionStrategy() {
                        @Override
                        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                            final Expose expose = fieldAttributes.getAnnotation(Expose.class);
                            return expose != null && !expose.serialize();
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> aClass) {
                            return false;
                        }
                    })
                    .addDeserializationExclusionStrategy(new ExclusionStrategy() {
                        @Override
                        public boolean shouldSkipField(FieldAttributes fieldAttributes) {
                            final Expose expose = fieldAttributes.getAnnotation(Expose.class);
                            return expose != null && !expose.deserialize();
                        }

                        @Override
                        public boolean shouldSkipClass(Class<?> aClass) {
                            return false;
                        }
                    })
                    .create();
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
