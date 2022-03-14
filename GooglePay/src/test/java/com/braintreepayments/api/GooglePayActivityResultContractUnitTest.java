package com.braintreepayments.api;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.braintreepayments.api.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;
import static com.google.android.gms.wallet.AutoResolveHelper.RESULT_ERROR;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.wallet.PaymentData;
import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.TransactionInfo;
import com.google.android.gms.wallet.WalletConstants;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GooglePayActivityResultContractUnitTest {

    @Test
    public void createIntent_returnsIntentWithExtras() {
        GooglePayRequest googlePayRequest = new GooglePayRequest();
        googlePayRequest.setTransactionInfo(TransactionInfo.newBuilder()
                .setTotalPrice("1.00")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .setCurrencyCode("USD")
                .build());

        PaymentDataRequest paymentDataRequest = PaymentDataRequest.fromJson(googlePayRequest.toJson());

        GooglePayIntentData intentData = new GooglePayIntentData(1, paymentDataRequest);

        Context context = ApplicationProvider.getApplicationContext();

        GooglePayActivityResultContract sut = new GooglePayActivityResultContract();
        Intent intent = sut.createIntent(context, intentData);

        assertEquals(1, intent.getIntExtra(EXTRA_ENVIRONMENT, 0));
        assertSame(paymentDataRequest, intent.getParcelableExtra(EXTRA_PAYMENT_DATA_REQUEST));
    }

    @Test
    public void parseResult_whenResultIsOK_andPaymentDataExists_returnsGooglePayResultWithPaymentData() {
        PaymentData paymentData = PaymentData.fromJson("{}");
        Intent data = new Intent();
        paymentData.putIntoIntent(data);

        GooglePayActivityResultContract sut = new GooglePayActivityResultContract();

        GooglePayResult result = sut.parseResult(RESULT_OK, data);
        assertNotNull(result.getPaymentData());
        assertNull(result.getError());
    }

    @Test
    public void parseResult_whenResultIsCANCELED_returnsGooglePayResultWithError() {
        Intent data = new Intent();

        GooglePayActivityResultContract sut = new GooglePayActivityResultContract();

        GooglePayResult result = sut.parseResult(RESULT_CANCELED, data);

        Exception error = result.getError();
        assertTrue(error instanceof UserCanceledException);
        assertEquals("User canceled Google Pay.", error.getMessage());
        assertNull(result.getPaymentData());
    }

    @Test
    public void parseResult_whenResultIsRESULT_ERROR_returnsGooglePayResultWithError() {
        Intent data = new Intent();

        GooglePayActivityResultContract sut = new GooglePayActivityResultContract();

        GooglePayResult result = sut.parseResult(RESULT_ERROR, data);

        Exception error = result.getError();
        assertTrue(error instanceof GooglePayException);
        assertEquals("An error was encountered during the Google Pay " +
                "flow. See the status object in this exception for more details.", error.getMessage());
        assertNull(result.getPaymentData());
    }

    @Test
    public void parseResult_whenResultIsUnexpected_returnsGooglePayResultWithError() {
        Intent data = new Intent();

        GooglePayActivityResultContract sut = new GooglePayActivityResultContract();

        GooglePayResult result = sut.parseResult(2, data);

        Exception error = result.getError();
        assertTrue(error instanceof BraintreeException);
        assertEquals("An unexpected error occurred.", error.getMessage());
        assertNull(result.getPaymentData());
    }
}
