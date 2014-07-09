package com.braintreepayments.api.dropin.view;

import android.test.AndroidTestCase;
import android.text.Editable;
import android.text.Spanned;

public class MonthYearEditTextTest extends AndroidTestCase {

    private MonthYearEditText view;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        view = new MonthYearEditText(getContext());
    }

    public void testTyping_2_through_9_addsPrefix_0() {
        for (int i = 2; i <= 9; i++) {
            setText(String.valueOf(i));
            assertTextIs("0" + i + "/");
        }
    }

    public void testTyping_0_or_1_doesntAddPrefix_0() {
        setText("0");
        assertTextIs("0");

        setText("1");
        assertTextIs("1");
    }

    public void testSlashIsTypedForYouWhenYouType_MM() {
        for (int i = 1; i <= 12; i++) {
            String monthFormatted = String.format("%02d", i);
            setText(monthFormatted);
            assertTextIs(monthFormatted + "/");
        }
    }

    public void testGetMonth() {
        assertEquals("getMonth() should be \"\" if text is empty", "", view.getMonth());

        setText("01");
        assertEquals("01", view.getMonth());
        setText("01/");
        assertEquals("01", view.getMonth());
        setText("01/18");
        assertEquals("01", view.getMonth());
    }

    public void testGetYear() {
        assertEquals("getYear() should be \"\" if text is empty", "", view.getYear());

        setText("01/");
        assertEquals("", view.getYear());

        type('1');
        assertEquals("getYear() returns unformatted years", "1", view.getYear());

        type('8');
        assertEquals("18", view.getYear());

        setText("01/2018");
        assertEquals("getYear() will return 4-digit years", "2018", view.getYear());
    }

    public void testTypingForNumbersWithoutSlashWorks() {
        type('1', '2', '1', '8');
        assertTextIs("12/18");
    }

    public void testCanTypeSlashEvenIfThoughItsAddedForYou() {
        type('1', '2', '/', '1', '8');
        assertTextIs("12/18");
    }

    public void testSlashHasPadding() {
        setText("12/18");
        Spanned spanned = view.getText();

    }

    private MonthYearEditTextTest type(char... chars) {
        Editable editable = view.getText();
        for (char c : chars) {
            editable.append(c);
        }
        return this;
    }

    private MonthYearEditTextTest type(char c) {
        view.getText().append(c);
        return this;
    }

    private void assertTextIs(String expected) {
        assertEquals(expected, getString());
    }

    private String getString() {
        return view.getText().toString();
    }

    /** Clears the field and types the text as if it were new/untouched. */
    private void setText(CharSequence seq) {
        view.setText("");
        view.setText(seq);
    }
}
