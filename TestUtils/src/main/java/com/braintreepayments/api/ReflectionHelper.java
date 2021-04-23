package com.braintreepayments.api;

import java.lang.reflect.Field;

public class ReflectionHelper {

    private static Field findField(String fieldName, Object src) {
        Field foundField = null;
        Class cls = src.getClass();
        while (foundField == null && cls != null) {
            try {
                foundField = cls.getDeclaredField(fieldName);
            } catch (Exception ignored) {}

            cls = cls.getSuperclass();
        }
        return foundField;
    }

    public static Object getField(String fieldName, Object src)
            throws IllegalAccessException {
        Field field = findField(fieldName, src);
        field.setAccessible(true);
        return field.get(src);
    }
}
