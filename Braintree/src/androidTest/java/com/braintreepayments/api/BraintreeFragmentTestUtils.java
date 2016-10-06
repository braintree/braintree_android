package com.braintreepayments.api;

import android.app.Activity;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.CardBuilder;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.testutils.TestTokenizationKey;

import java.util.concurrent.CountDownLatch;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.SharedPreferencesHelper.writeMockConfiguration;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.spy;

public class BraintreeFragmentTestUtils {

    public static BraintreeFragment getFragmentWithAuthorization(Activity activity, String authorization) {
        return getFragment(activity, authorization, null);
    }

    public static BraintreeFragment getFragmentWithConfiguration(Activity activity, String configuration) {
        return getFragment(activity, null, configuration);
    }

    public static BraintreeFragment getMockFragmentWithAuthorization(Activity activity, String authorization) {
        return getMockFragment(activity, authorization, null);
    }

    public static BraintreeFragment getMockFragmentWithConfiguration(Activity activity, String configuration) {
        return getMockFragment(activity, null, configuration);
    }

    public static BraintreeFragment getMockFragment(Activity activity, String authoirzation, String configuration) {
        return spy(getFragment(activity, authoirzation, configuration));
    }

    public static BraintreeFragment getFragment(Activity activity, String authorization, String configuration) {
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
        String appendedAuthorization = "";
        if (auth instanceof ClientToken) {
            appendedAuthorization = ((ClientToken) auth).getAuthorizationFingerprint();
        } else if (auth instanceof TokenizationKey) {
            appendedAuthorization = auth.toString();
        }

        writeMockConfiguration(getTargetContext(), auth.getConfigUrl(), appendedAuthorization, configuration);
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
