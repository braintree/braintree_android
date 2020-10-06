package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;
import androidx.test.core.app.ApplicationProvider;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.testutils.TestTokenizationKey;

import java.util.concurrent.CountDownLatch;

import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
import static junit.framework.Assert.fail;

public class BraintreeFragmentTestUtils {

    public static BraintreeFragment getFragmentWithAuthorization(FragmentActivity activity, String authorization) {
        return getFragment(activity, authorization, null);
    }

    public static BraintreeFragment getFragmentWithConfiguration(FragmentActivity activity, String configuration) {
        return getFragment(activity, null, configuration);
    }

    public static BraintreeFragment getFragment(FragmentActivity activity, String authorization, String configuration) {
        try {
            if (authorization == null) {
                authorization = TestTokenizationKey.TOKENIZATION_KEY;
            }

            if (configuration != null) {
                setConfiguration(authorization, configuration);
            }

            BraintreeFragment fragment = BraintreeFragment.newInstance(activity, authorization);

            while (!fragment.isAdded()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }

            return fragment;
        } catch (InvalidArgumentException e) {
            fail(e.getMessage());
            return new BraintreeFragment();
        }
    }

    public static void setConfiguration(String authorization, String configuration) throws InvalidArgumentException {
        Authorization auth = Authorization.fromString(authorization);
        writeMockConfiguration(ApplicationProvider.getApplicationContext(), auth.getConfigUrl(), auth.getBearer(), configuration);
    }

    public static CardNonce tokenize(BraintreeFragment fragment, CardBuilder cardBuilder) {
        final CountDownLatch latch = new CountDownLatch(1);
        final CardNonce[] cardNonce = new CardNonce[1];
        PaymentMethodNonceCreatedListener listener = new PaymentMethodNonceCreatedListener() {
            @Override
            public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
                cardNonce[0] = (CardNonce) paymentMethodNonce;
                latch.countDown();
            }
        };
        fragment.addListener(listener);

        Card.tokenize(fragment, cardBuilder);

        try {
            latch.await();
        } catch (InterruptedException ignored) {}

        fragment.removeListener(listener);
        return cardNonce[0];
    }
}
