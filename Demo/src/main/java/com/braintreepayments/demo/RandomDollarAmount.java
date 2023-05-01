package com.braintreepayments.demo;

import java.util.Locale;
import java.util.Random;

public class RandomDollarAmount {

    private static Random random = new Random();

    private RandomDollarAmount() {}

    public static String getNext() {
        int dollars = random.nextInt(100);
        return String.format(Locale.getDefault(), "%d.00", dollars);
    }
}
