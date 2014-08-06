package com.braintreepayments.api.dropin.utils;

import junit.framework.TestCase;

import java.util.Calendar;

public class DateValidatorTest extends TestCase {

    private DateValidator mValidator;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        // Tests assume the current date is May 7th, 2014, unless otherwise stated.
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, 2014);
        calendar.set(Calendar.MONTH, Calendar.MAY);
        calendar.set(Calendar.DAY_OF_MONTH, 7);

        mValidator = new DateValidator(calendar);
    }

    public void testSlashIsNecessary() {
        assertInvalid("0918");
    }

    public void testOnlyOneSlashCanAppear() {
        assertInvalid("01/01/2018");
    }

    public void testMonthIsRequired() {
        assertInvalid("/18");
        assertInvalid("/2018");
    }

    public void testYearIsRequired() {
        assertInvalid("01/");
    }

    public void testLeadingZeroNotRequiredForMonth() {
        assertValid("7/18");
    }

    public void testLeadingZeroRequiredForYear() {
        assertInvalid("11/7");
    }

    public void testMonthEdgeCases() {
        assertInvalid("00/18");
        assertInvalid("13/18");

        assertValid("01/18");
        assertValid("12/18");
    }

    public void testPastMonthInCurrentYearIsInvalid() {
        assertInvalid("04/14");
    }

    public void testCurrentMonthAndYearIsValid() {
        assertValid("05/14");
    }

    public void testFutureMonthInCurrentYearIsValid() {
        assertValid("06/14");
    }

    public void testYearInPastIsInvalid() {
        assertInvalid("05/13");
    }

    public void testYearWrapping() {
        Calendar endOfCenturyCalendar = Calendar.getInstance();
        endOfCenturyCalendar.set(Calendar.YEAR, 2095);
        endOfCenturyCalendar.set(Calendar.MONTH, Calendar.NOVEMBER);
        DateValidator endOfCenturyValidator = new DateValidator(endOfCenturyCalendar);

        assertTrue(endOfCenturyValidator.isValidHelper("01/01"));
        assertTrue(endOfCenturyValidator.isValidHelper("01/05"));
        assertTrue(endOfCenturyValidator.isValidHelper("01/96"));
        assertFalse(endOfCenturyValidator.isValidHelper("01/94"));

        // the following assertions use the regular validator, where the year is set to 2014. Years
        // with prefix-zeros should now fail.
        assertInvalid("01/01");
        assertInvalid("01/05");
        assertValid("01/94");
    }

    private void assertValid(String str) {
        assertTrue(mValidator.isValidHelper(str));
    }

    private void assertInvalid(String str) {
        assertFalse(mValidator.isValidHelper(str));
    }

}
