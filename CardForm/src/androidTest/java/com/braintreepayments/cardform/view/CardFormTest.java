package com.braintreepayments.cardform.view;

import android.test.UiThreadTest;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.braintreepayments.cardform.OnCardFormValidListener;
import com.braintreepayments.cardform.R;
import com.braintreepayments.cardform.test.TestActivityTestCase;

import java.util.concurrent.atomic.AtomicInteger;

import static com.braintreepayments.testutils.CardNumber.VISA;

public class CardFormTest extends TestActivityTestCase {

    private static final String TEST = "TEST";

    private CardForm mCardForm;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mCardForm = (CardForm) getActivity().findViewById(android.R.id.custom);
        mCardForm.setRequiredFields(true, true, true, true, TEST);
    }

    @UiThreadTest
    public void testSetsIMEActionAsGoForExpirationIfCvvAndPostalAreNotPresent() {
        mCardForm.setRequiredFields(true, true, false, false, TEST);

        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
    }

    @UiThreadTest
    public void testSetsIMEActionAsGoForCvvIfCvvIsPresentAndPostalIsNot() {
        mCardForm.setRequiredFields(true, true, true, false, TEST);

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_cvv)).getImeOptions());
    }

    @UiThreadTest
    public void testSetsIMEActionAsGoForPostalAndNextForExpirationIfCvvIsNotPresent() {
        mCardForm.setRequiredFields(true, true, false, true, TEST);

        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_postal_code)).getImeOptions());
    }

    @UiThreadTest
    public void testSetsIMEActionAsGoForPostalCodeIfCvvAndPostalArePresent() {
        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_expiration)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_NEXT,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_cvv)).getImeOptions());
        assertEquals(EditorInfo.IME_ACTION_GO,
                ((TextView) mCardForm.findViewById(R.id.bt_card_form_postal_code)).getImeOptions());
    }

    @UiThreadTest
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

    @UiThreadTest
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

    @UiThreadTest
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

    @UiThreadTest
    public void testGetCardNumberReturnsCardNumber() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).setText(VISA);
        assertEquals(VISA, mCardForm.getCardNumber());
    }

    @UiThreadTest
    public void testGetExpirationMonthReturnsExpirationMonth() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).setText("1230");
        assertEquals("12", mCardForm.getExpirationMonth());
    }

    @UiThreadTest
    public void testGetExpirationYearReturnsExpirationYear() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).setText("1230");
        assertEquals("30", mCardForm.getExpirationYear());
    }

    @UiThreadTest
    public void testGetCvvReturnsCvv() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).setText("123");
        assertEquals("123", mCardForm.getCvv());
    }

    @UiThreadTest
    public void testGetPostalCodeReturnsPostalCode() {
        ((EditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).setText("12345");
        assertEquals("12345", mCardForm.getPostalCode());
    }

    @UiThreadTest
    public void testSetCardNumberError() {
        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).isError());
        mCardForm.setCardNumberError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_card_number)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_card_number)).isFocused());
    }

    @UiThreadTest
    public void testSetExpirationError() {
        mCardForm.setRequiredFields(false, true, true, true, TEST);

        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).isError());
        mCardForm.setExpirationError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_expiration)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_expiration)).isFocused());
    }

    @UiThreadTest
    public void testSetExpirationErrorDoesNotRequestFocusIfCardNumberIsAlreadyFocused() {
        mCardForm.findViewById(R.id.bt_card_form_card_number).requestFocus();
        mCardForm.setExpirationError();
        assertTrue(mCardForm.findViewById(R.id.bt_card_form_card_number).isFocused());
        assertFalse(mCardForm.findViewById(R.id.bt_card_form_expiration).isFocused());
    }

    @UiThreadTest
    public void testSetCvvError() {
        mCardForm.setRequiredFields(false, false, true, true, TEST);

        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).isError());
        mCardForm.setCvvError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_cvv)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_cvv)).isFocused());
    }

    @UiThreadTest
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

    @UiThreadTest
    public void testSetPostalCodeError() {
        mCardForm.setRequiredFields(false, false, false, true, TEST);

        assertFalse(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).isError());
        mCardForm.setPostalCodeError();
        assertTrue(((ErrorEditText) mCardForm.findViewById(R.id.bt_card_form_postal_code)).isError());
        assertTrue((mCardForm.findViewById(R.id.bt_card_form_postal_code)).isFocused());
    }

    @UiThreadTest
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

    @UiThreadTest
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
