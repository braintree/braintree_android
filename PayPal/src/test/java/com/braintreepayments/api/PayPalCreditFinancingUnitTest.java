package com.braintreepayments.api;

import android.os.Parcel;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PayPalCreditFinancingUnitTest {

    @Test
    public void fromJson_returnsNullWhenEmpty() throws JSONException {
        PayPalCreditFinancing payPalCreditFinancing = PayPalCreditFinancing.fromJson(null);
        assertNotNull(payPalCreditFinancing);
        assertFalse(payPalCreditFinancing.isCardAmountImmutable());
        assertEquals(0, payPalCreditFinancing.getTerm());
        assertFalse(payPalCreditFinancing.hasPayerAcceptance());
        assertNull(payPalCreditFinancing.getMonthlyPayment());
        assertNull(payPalCreditFinancing.getTotalCost());
        assertNull(payPalCreditFinancing.getTotalInterest());
    }

    @Test
    public void canCreateCreditFinancing_fromStandardJson() throws JSONException {
        String paypalAccountResponse = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE;
        JSONObject creditFinancingJsonObject = new JSONObject(paypalAccountResponse).getJSONArray("paypalAccounts")
                .getJSONObject(0).getJSONObject("details").getJSONObject("creditFinancingOffered");

        PayPalCreditFinancing payPalCreditFinancing = PayPalCreditFinancing.fromJson(creditFinancingJsonObject);

        assertFalse(payPalCreditFinancing.isCardAmountImmutable());
        assertEquals(18, payPalCreditFinancing.getTerm());
        assertTrue(payPalCreditFinancing.hasPayerAcceptance());
        assertEquals("USD", payPalCreditFinancing.getMonthlyPayment().getCurrency());
        assertEquals("USD", payPalCreditFinancing.getTotalCost().getCurrency());
        assertEquals("USD", payPalCreditFinancing.getTotalInterest().getCurrency());
        assertEquals("13.88", payPalCreditFinancing.getMonthlyPayment().getValue());
        assertEquals("250.00", payPalCreditFinancing.getTotalCost().getValue());
        assertEquals("0.00", payPalCreditFinancing.getTotalInterest().getValue());
    }

    @Test
    public void canCreateCreditFinancing_fromJsonWithoutCreditFinancingData() throws JSONException {
        String paypalAccountResponse = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE_WITHOUT_CREDIT_FINANCING_DATA;
        JSONObject creditFinancingJsonObject = new JSONObject(paypalAccountResponse).getJSONArray("paypalAccounts")
                .getJSONObject(0).getJSONObject("details").getJSONObject("creditFinancingOffered");

        PayPalCreditFinancing payPalCreditFinancing = PayPalCreditFinancing.fromJson(creditFinancingJsonObject);

        assertFalse(payPalCreditFinancing.isCardAmountImmutable());
        assertEquals(18, payPalCreditFinancing.getTerm());
        assertFalse(payPalCreditFinancing.hasPayerAcceptance());
        assertNull(payPalCreditFinancing.getMonthlyPayment().getCurrency());
        assertNull(payPalCreditFinancing.getTotalCost().getCurrency());
        assertNull(payPalCreditFinancing.getTotalInterest().getCurrency());
        assertNull(payPalCreditFinancing.getMonthlyPayment().getValue());
        assertNull(payPalCreditFinancing.getTotalCost().getValue());
        assertNull(payPalCreditFinancing.getTotalInterest().getValue());
    }

    @Test
    public void writeToParcel_serializesCorrectly() throws JSONException {
        String paypalAccountResponse = Fixtures.PAYMENT_METHODS_PAYPAL_ACCOUNT_RESPONSE;
        JSONObject creditFinancingJsonObject = new JSONObject(paypalAccountResponse).getJSONArray("paypalAccounts")
                .getJSONObject(0).getJSONObject("details").getJSONObject("creditFinancingOffered");

        PayPalCreditFinancing preSerialized = PayPalCreditFinancing.fromJson(creditFinancingJsonObject);
        Parcel parcel = Parcel.obtain();
        preSerialized.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalCreditFinancing payPalCreditFinancing = PayPalCreditFinancing.CREATOR.createFromParcel(parcel);

        assertNotNull(payPalCreditFinancing);
        assertFalse(payPalCreditFinancing.isCardAmountImmutable());
        assertEquals(18, payPalCreditFinancing.getTerm());
        assertTrue(payPalCreditFinancing.hasPayerAcceptance());
        assertEquals("USD", payPalCreditFinancing.getMonthlyPayment().getCurrency());
        assertEquals("USD", payPalCreditFinancing.getTotalCost().getCurrency());
        assertEquals("USD", payPalCreditFinancing.getTotalInterest().getCurrency());
        assertEquals("13.88", payPalCreditFinancing.getMonthlyPayment().getValue());
        assertEquals("250.00", payPalCreditFinancing.getTotalCost().getValue());
        assertEquals("0.00", payPalCreditFinancing.getTotalInterest().getValue());
    }
}
