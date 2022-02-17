package com.braintreepayments.api;

import static com.braintreepayments.api.GooglePayClient.EXTRA_ENVIRONMENT;
import static com.braintreepayments.api.GooglePayClient.EXTRA_PAYMENT_DATA_REQUEST;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertSame;
import static org.mockito.Mockito.mock;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.google.android.gms.wallet.PaymentDataRequest;
import com.google.android.gms.wallet.ShippingAddressRequirements;
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

    }

    @Test
    public void parseResult_whenResultIsCANCELED_returnsGooglePayResultWithError() {

    }
}
