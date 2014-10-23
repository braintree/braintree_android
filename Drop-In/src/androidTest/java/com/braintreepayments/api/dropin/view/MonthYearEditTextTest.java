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
            assertTextIs("0" + i);
        }
    }

    public void testTyping_0_or_1_doesntAddPrefix_0() {
        setText("0");
        assertTextIs("0");

        setText("1");
        assertTextIs("1");
    }

    public void testCanOnlyTypeNumeric() {
        type('-');
        assertTextIs("");

        type('5', ':', '/', '-', 'h');
        assertTextIs("05");
    }

    public void testTypingNumbersWithoutSlashWorks() {
        type('1', '2', '1', '8');
        assertTextIs("1218");
    }

    public void testAddsSlashForYou() {
        setText("1218");

        Spanned spanned = view.getText();

        AppendSlashSpan[] appendSlashSpan = spanned.getSpans(0, view.getText().toString().length(), AppendSlashSpan.class);
        assertEquals(1, appendSlashSpan.length);
    }

    public void testMaxLengthIsSix() {
        type('1', '2', '2', '0', '0', '1', '5');
        assertTextIs("122001");
    }

    public void testGetMonth() {
        assertEquals("getMonth() should be \"\" if text is empty", "", view.getMonth());

        setText("1");
        assertEquals("", view.getMonth());
        setText("01");
        assertEquals("01", view.getMonth());
        setText("0218");
        assertEquals("02", view.getMonth());
        setText("032018");
        assertEquals("03", view.getMonth());
    }

    public void testGetYear() {
        assertEquals("getYear() should be \"\" if text is empty", "", view.getYear());

        setText("01");
        assertEquals("", view.getYear());

        type('1');
        assertEquals("getYear() doesn't return unformatted years", "", view.getYear());

        type('8');
        assertEquals("18", view.getYear());

        setText("012018");
        assertEquals("getYear() will return 4-digit years", "2018", view.getYear());
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
