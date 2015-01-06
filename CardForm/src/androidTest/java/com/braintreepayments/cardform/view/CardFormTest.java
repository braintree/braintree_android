package com.braintreepayments.cardform.view;

import android.test.AndroidTestCase;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.braintreepayments.cardform.OnCardFormValidListener;
import com.braintreepayments.cardform.R;

import java.util.concurrent.atomic.AtomicInteger;

import static com.braintreepayments.testutils.CardNumber.VISA;

public class CardFormTest extends AndroidTestCase {

    private static final String TEST = "TEST";

    private CardForm mCardForm;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mCardForm = new CardForm(getContext());
        mCardForm.setRequiredFields(true, true, true, true, TEST);
    }

    public void testCardNumberIsShownIfRequired() {
        mCardForm.setRequiredFields(true, false, false, false, TEST);

        assertEquals(View.VISIBLE,
                mCardForm.findViewById(R.id.bt_card_form_card_number).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_expiration).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_cvv).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_postal_code).getVisibility());
    }

    public void testExpirationIsShownIfRequired() {
        mCardForm.setRequiredFields(false, true, false, false, TEST);

        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_card_number).getVisibility());
        assertEquals(View.VISIBLE, mCardForm.findViewById(R.id.bt_card_form_expiration).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_cvv).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_postal_code).getVisibility());
    }

    public void testCvvIsShownIfRequired() {
        mCardForm.setRequiredFields(false, false, true, false, TEST);

        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_card_number).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_expiration).getVisibility());
        assertEquals(View.VISIBLE, mCardForm.findViewById(R.id.bt_card_form_cvv).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_postal_code).getVisibility());
    }

    public void testPostalCodeIsShownIfRequired() {
        mCardForm.setRequiredFields(false, false, false, true, TEST);

        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_card_number).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_expiration).getVisibility());
        assertEquals(View.GONE, mCardForm.findViewById(R.id.bt_card_form_cvv).getVisibility());
        assertEquals(View.VISIBLE,
                mCardForm.findViewById(R.id.bt_card_form_postal_code).getVisibility());
    }

    public void testSetsIMEActionAsGoForExpirationIfCvvAndPostalAreNotPresent() {
        mCardForm.setRequiredFields(true, true, false, false, TEST);

        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
    }

    public void testSetsIMEActionAsGoForCvvIfCvvIsPresentAndPostalIsNot() {
        mCardForm.setRequiredFields(true, true, true, false, TEST);

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_cvv)).getImeOptions());
    }

    public void testSetsIMEActionAsGoForPostalAndNextForExpirationIfCvvIsNotPresent() {
        mCardForm.setRequiredFields(true, true, false, true, TEST);

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_postal_code)).getImeOptions());
    }

    public void testSetsIMEActionAsGoForPostalCodeIfCvvAndPostalArePresent() {
        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_cvv)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_postal_code)).getImeOptions());
    }

    public void testSetEnabledSetsStateCorrectly() {
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_card_number).isEnabled());
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_expiration).isEnabled());
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_cvv).isEnabled());
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_postal_code).isEnabled());

        mCardForm.setEnabled(false);

        assertFalse(mCardForm.findViewById(R.id.bt_card_form_card_number).isEnabled());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_expiration).isEnabled());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_cvv).isEnabled());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_postal_code).isEnabled());
    }

    public void testIsValidOnlyValidatesRequiredFields() {
        mCardForm.setRequiredFields(true, false, false, false, TEST);
        assertFalse(mCardForm.isValid());
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).setText(VISA);
        assertTrue(mCardForm.isValid());
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).isError());

        mCardForm.setRequiredFields(false, true, false, false, TEST);
        assertFalse(mCardForm.isValid());
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).setText("1230");
        assertTrue(mCardForm.isValid());
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).isError());

        mCardForm.setRequiredFields(false, false, true, false, TEST);
        assertFalse(mCardForm.isValid());
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).setText("123");
        assertTrue(mCardForm.isValid());
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).isError());

        mCardForm.setRequiredFields(false, false, false, true, TEST);
        assertFalse(mCardForm.isValid());
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).setText("12345");
        assertTrue(mCardForm.isValid());
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).isError());
    }

    public void testValidateSetsErrorOnFields() {
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).isError());
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).isError());
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).isError());
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).isError());

        mCardForm.validate();

        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).isError());
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).isError());
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).isError());
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).isError());
    }

    public void testGetCardNumberReturnsCardNumber() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).setText(VISA);
        assertEquals(VISA, mCardForm.getCardNumber());
    }

    public void testGetExpirationMonthReturnsExpirationMonth() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).setText("1230");
        assertEquals("12", mCardForm.getExpirationMonth());
    }

    public void testGetExpirationYearReturnsExpirationYear() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).setText("1230");
        assertEquals("30", mCardForm.getExpirationYear());
    }

    public void testGetCvvReturnsCvv() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).setText("123");
        assertEquals("123", mCardForm.getCvv());
    }

    public void testGetPostalCodeReturnsPostalCode() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).setText("12345");
        assertEquals("12345", mCardForm.getPostalCode());
    }

    public void testSetCardNumberError() {
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).isError());
        mCardForm.setCardNumberError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_card_number)).isFocused());
    }

    public void testSetExpirationError() {
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).isError());
        mCardForm.setExpirationError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_expiration)).isFocused());
    }

    public void testSetExpirationErrorDoesNotRequestFocusIfCardNumberIsAlreadyFocused() {
        mCardForm.findViewById(R.id.bt_card_form_card_number).requestFocus();
        mCardForm.setExpirationError();
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_card_number).isFocused());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_expiration).isFocused());
    }

    public void testSetCvvError() {
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).isError());
        mCardForm.setCvvError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_cvv)).isFocused());
    }

    public void testSetCvvErrorDoesNotRequestFocusIfCardNumberOrExpirationIsAlreadyFocused() {
        mCardForm.findViewById(R.id.bt_card_form_card_number).requestFocus();
        mCardForm.setCvvError();
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_card_number).isFocused());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_cvv).isFocused());

        mCardForm.findViewById(R.id.bt_card_form_expiration).requestFocus();
        mCardForm.setCvvError();
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_expiration).isFocused());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_cvv).isFocused());
    }

    public void testSetPostalCodeError() {
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).isError());
        mCardForm.setPostalCodeError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_postal_code)).isFocused());
    }

    public void testSetPostalCodeErrorDoesNotRequestFocusIfCardNumberCvvOrExpirationIsAlreadyFocused() {
        mCardForm.findViewById(R.id.bt_card_form_card_number).requestFocus();
        mCardForm.setPostalCodeError();
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_card_number).isFocused());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_postal_code).isFocused());

        mCardForm.findViewById(R.id.bt_card_form_expiration).requestFocus();
        mCardForm.setPostalCodeError();
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_expiration).isFocused());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_postal_code).isFocused());

        mCardForm.findViewById(R.id.bt_card_form_cvv).requestFocus();
        mCardForm.setPostalCodeError();
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_cvv).isFocused());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_postal_code).isFocused());
    }

    public void testOnCardFormValidListenerOnlyCalledOnValidityChange() {
        final AtomicInteger counter = new AtomicInteger(0);
        mCardForm.setOnCardFormValidListener(new OnCardFormValidListener() {
            @Override
            public void onCardFormValid(boolean valid) {
                counter.getAndIncrement();
            }
        });

        ((EditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).setText(VISA);
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).setText("0925");
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).setText("123");
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).setText("12345");

        assertEquals(1, counter.get());

        ((EditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).setText("12");

        assertEquals(2, counter.get());
    }

}
