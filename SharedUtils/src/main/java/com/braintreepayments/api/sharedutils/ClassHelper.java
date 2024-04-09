package com.braintreepayments.api.sharedutils;

class ClassHelper {

    ClassHelper() {}

    boolean isClassAvailable(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
