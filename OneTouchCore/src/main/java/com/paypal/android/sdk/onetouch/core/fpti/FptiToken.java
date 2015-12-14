package com.paypal.android.sdk.onetouch.core.fpti;

import java.util.Random;

public class FptiToken {

    public static final int FPTI_TOKEN_VALIDITY_IN_HOURS = 30;

    public String mToken;

    /**
     * Java Date as a long
     */
    private long mValidUntil;

    /**
     * Creates a token, good for 30 hours
     */
    public FptiToken() {
        final long now = System.currentTimeMillis();
        if (mToken == null) {
            mValidUntil = now; // force the below if to be true
        }

        if (((mValidUntil + (FPTI_TOKEN_VALIDITY_IN_HOURS * 60 * 1000)) > now)) {
            mValidUntil = now + (FPTI_TOKEN_VALIDITY_IN_HOURS * 60 * 1000);

            final Random r = new Random(mValidUntil);
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; ++i) {
                sb.append((char) ('0' + (Math.abs(r.nextInt()) % 10)));
            }
            mToken = sb.toString();
        }
    }

    /**
     * @return {@code true} if the token is valid (not expired), otherwise {@code false}.
     */
    public boolean isValid() {
        return mValidUntil > System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return FptiToken.class.getSimpleName() + "[mToken=" + mToken + ", mValidUntil=" +
                mValidUntil + "]";
    }
}
