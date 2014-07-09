package com.braintreepayments.api.android.dropin.view;

import android.test.AndroidTestCase;
import android.text.Editable;

import com.braintreepayments.api.dropin.utils.CardType;
import com.braintreepayments.api.dropin.view.CvvEditText;

public class CvvEditTextTest extends AndroidTestCase {
    private CvvEditText view;

    @Override
    protected void setUp() throws Exception {
        view = new CvvEditText(getContext());
    }

    public void testDefaultLimitIs3() {
        type("123").assertTextIs("123");
        type("4").assertTextIs("123");
    }

    public void testCustomLimits() {
        for (CardType type : CardType.values()) {
            view.setCardType(type);
            if (type == CardType.AMEX) {
                type("1234").assertTextIs("1234");
                type("5").assertTextIs("1234");
            } else {
                type("123").assertTextIs("123");
                type("4").assertTextIs("123");
            }
            clearText();
        }
    }

    private CvvEditTextTest type(String text) {
        Editable editable = view.getText();
        for (char c : text.toCharArray()) {
            editable.append(c);
        }
        return this;
    }

    private void clearText() {
        view.getText().clear();
    }

    private CvvEditTextTest assertTextIs(String expected) {
        assertEquals(expected, view.getText().toString());
        return this;
    }

}
