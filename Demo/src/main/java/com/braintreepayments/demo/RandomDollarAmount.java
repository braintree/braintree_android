package com.braintreepayments.demo;

import java.util.Random;

public class RandomDollarAmount {

    private static Random random = new Random();

    private RandomDollarAmount() {}

    public static String getNext() {
        int dollars = random.nextInt(100);
        return String.format("%d.00", dollars);
    }
}
