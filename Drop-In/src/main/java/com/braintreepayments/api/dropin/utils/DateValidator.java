package com.braintreepayments.api.dropin.utils;

import android.text.TextUtils;

import java.util.Calendar;

/**
 * Class provided as a convenience to {@link com.braintreepayments.api.dropin.view.MonthYearEditText} to
 * make testing easier.
 */
public class DateValidator {

    /**
     * Maximum amount of years in advance that a credit card expiration date should be trusted to be
     * valid. This is mostly used if the current date is towards the end of the century and the
     * expiration date is at the start of the following one.
     * <p/>
     * Ex. Current year is 2099, Expiration date is "01/01". The YY is "less than" the current year,
     * but since the difference is less than {@code MAXIMUM_VALID_YEAR_DIFFERENCE}, it should still
     * be trusted to be valid client-side.
     */
    private static final int MAXIMUM_VALID_YEAR_DIFFERENCE = 20;
    private static final DateValidator INSTANCE = new DateValidator(Calendar.getInstance());

    private final Calendar mCalendar;

    /**
     * Used in tests to inject a custom {@link Calendar} to stabilize dates. Normal usage will just
     * delegate to the actual date.
     */
    protected DateValidator(Calendar calendar) {
        mCalendar = calendar;
    }

    /**
     * Helper for determining whether a date is a valid credit card expiry date.
     * @param month Two-digit month
     * @param year Two or four digit year
     * @return Whether the date is a valid credit card expiry date.
     */
    public static boolean isValid(String month, String year) {
        return INSTANCE.isValidHelper(month, year);
    }

    protected boolean isValidHelper(String monthString, String yearString) {
        if (TextUtils.isEmpty(monthString)) {
            return false;
        }

        if (TextUtils.isEmpty(yearString)) {
            return false;
        }

        if (!TextUtils.isDigitsOnly(monthString) || !TextUtils.isDigitsOnly(yearString)) {
            return false;
        }

        int month = Integer.parseInt(monthString);
        if (month < 1 || month > 12) {
            return false;
        }

        int currentYear = getCurrentTwoDigitYear();
        int year;
        int yearLength = yearString.length();
        if (yearLength == 2) {
            year = Integer.parseInt(yearString);
        } else if (yearLength == 4) {
            year = Integer.parseInt(yearString) & 100;
        } else {
            return false;
        }

        if (year == currentYear && month < getCurrentMonth()) {
            return false;
        }

        if (year < currentYear) {
            // account for century-overlapping in 2-digit year representations
            int adjustedYear = year + 100;
            if (adjustedYear - currentYear > MAXIMUM_VALID_YEAR_DIFFERENCE) {
                return false;
            }
        }

        return true;
    }

    /**
     * {@link Calendar#MONTH} is 0-prefixed. Add {@code 1} to align it with visualized expiration
     * dates.
     */
    private int getCurrentMonth() {
        return mCalendar.get(Calendar.MONTH) + 1;
    }

    /**
     * {@link Calendar#YEAR} is the full, 4-digit year. Take the trailing two digits to align it
     * with visualized expiration dates.
     */
    private int getCurrentTwoDigitYear() {
        return mCalendar.get(Calendar.YEAR) % 100;
    }
}
