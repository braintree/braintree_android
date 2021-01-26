package com.braintreepayments.api;

import java.util.Calendar;

public class ExpirationDateHelper {

    public static String validExpirationYear() {
        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR) + 1);
    }
}
