package com.braintreepayments.api.android.dropin.view;

import android.test.AndroidTestCase;
import android.text.Editable;

import com.braintreepayments.api.dropin.view.PaddingSpan;
import com.braintreepayments.api.dropin.view.CardEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.braintreepayments.api.BraintreeTestUtils.assertBitmapsEqual;
import static com.braintreepayments.api.dropin.R.drawable.bt_amex;
import static com.braintreepayments.api.dropin.R.drawable.bt_card_highlighted;
import static com.braintreepayments.api.dropin.R.drawable.bt_diners;
import static com.braintreepayments.api.dropin.R.drawable.bt_discover;
import static com.braintreepayments.api.dropin.R.drawable.bt_jcb;
import static com.braintreepayments.api.dropin.R.drawable.bt_maestro;
import static com.braintreepayments.api.dropin.R.drawable.bt_mastercard;
import static com.braintreepayments.api.dropin.R.drawable.bt_visa;

public class CardEditTextTest extends AndroidTestCase {

    private CardEditText view;

    @Override
    protected void setUp() throws Exception {
        view = new CardEditText(getContext());
    }

    public void testVisa() {
        helper("4", "111 1111 1111 1111", bt_visa, 4, 8, 12);
    }

    public void testMasterCard() {
        helper("55", "55 5555 5555 4444", bt_mastercard, 4, 8, 12);
    }

    public void testDiscover() {
        helper("6011", "1111 1111 1117", bt_discover, 4, 8, 12);
    }

    public void testAmex() {
        helper("37", "82 822463 10005", bt_amex, 4, 10);
    }

    public void testJcb() {
        helper("35", "30 1113 3330 0000", bt_jcb, 4, 8, 12);
    }

    public void testDiners() {
        helper("3000", "0000 0000 04", bt_diners, 4, 8, 12);
    }

    public void testMaestro() {
        helper("5018", "0000 0000 0009", bt_maestro, 4, 8, 12);
    }

    public void testUnionPay() {
        helper("62", "40 8888 8888 8885", bt_card_highlighted, 4, 8, 12);
    }

    private void helper(String start, String end, int drawable, int... spans) {
        assertHintIs(bt_card_highlighted);
        type(start).assertHintIs(drawable);
        type(end).assertSpansAt(spans);
    }

    private void assertSpansAt(int... indices) {
        Editable text = view.getText();
        List<PaddingSpan> allSpans = Arrays.asList(text.getSpans(0, text.length(),
                PaddingSpan.class));
        List<PaddingSpan> foundSpans = new ArrayList<PaddingSpan>();
        for (int i : indices) {
            PaddingSpan[] span = text.getSpans(i - 1, i, PaddingSpan.class);
            assertEquals(1, span.length);
            foundSpans.add(span[0]);
        }
        assertEquals(allSpans, foundSpans);
    }

    private void assertHintIs(int resId) {
        assertBitmapsEqual(getContext().getResources().getDrawable(resId),
                view.getCompoundDrawables()[2]);
    }

    private CardEditTextTest type(String text) {
        Editable editable = view.getText();
        for (char c : text.toCharArray()) {
            if (c != ' ') {
                editable.append(c);
            }
        }
        return this;
    }
}
