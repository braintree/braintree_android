package com.braintreepayments.testutils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class ReflectionHelper {

    public static Object getField(Class clazz, String fieldName, Object src)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(src);
    }

    public static void setField(Class clazz, String fieldName, Object src, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(src, value);
    }
}
