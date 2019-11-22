package com.braintreepayments.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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

    public static void setField(String fieldName, Object src, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = findField(fieldName, src);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(src, value);
    }
}
