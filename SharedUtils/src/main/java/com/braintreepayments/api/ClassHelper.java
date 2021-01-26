package com.braintreepayments.api;

import java.lang.reflect.Field;

public class ClassHelper {

    public static boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * @param className The name of the class we need a field from
     * @param fieldName The name of the field we need a value from
     * @param <FIELD_TYPE> The type of the field value
     * @return the value of a field on a class, or null.
     */
    public static <FIELD_TYPE> FIELD_TYPE getFieldValue(String className, String fieldName) {
        FIELD_TYPE value = null;

        try {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getField(fieldName);

            field.setAccessible(true);
            value = (FIELD_TYPE) field.get(Object.class);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException ignored) {}

        return value;
    }
}
