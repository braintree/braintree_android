package com.braintreepayments.api;

import android.content.Context;
import android.content.Intent;
import android.test.ActivityInstrumentationTestCase2;

import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.exceptions.BraintreeException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.internal.HttpRequest;
import com.braintreepayments.api.models.ClientToken;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;

import static com.braintreepayments.api.TestDependencyInjector.injectBraintree;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test utility class with access to protected methods of classes in `com.braintreepayments.api`
 */
public class BraintreeTestUtils {

    /**
     * Injects an instance of {@link com.braintreepayments.api.Braintree} that will use the given
     * client token and sets the client token as an extra to be received by
     * {@link com.braintreepayments.api.dropin.BraintreePaymentActivity} upon calling
     * {@link android.test.ActivityInstrumentationTestCase2#getActivity()}.
     *
     * @param testCase
     * @param clientToken
     */
    public static void setUpActivityTest(
            ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase, String clientToken)
            throws JSONException {
        injectBraintree(testCase.getInstrumentation().getContext(), clientToken,
                clientToken);
        setClientTokenExtraForTest(testCase, clientToken);
    }

    /**
     * Creates a {@link com.braintreepayments.api.models.ClientToken} with FakePayPal
     * and sets it the client token as an extra to be received by
     * {@link com.braintreepayments.api.dropin.BraintreePaymentActivity} upon calling
     * {@link android.test.ActivityInstrumentationTestCase2#getActivity()}.
     *
     * @param testCase
     * @return
     */
    public static String setClientTokenExtraForTest(ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase) {
        String clientToken = new TestClientTokenBuilder().withPayPal().build();
        setClientTokenExtraForTest(testCase, clientToken);

        return clientToken;
    }

    /**
     * Sets the client token as an extra to be received by
     * {@link com.braintreepayments.api.dropin.BraintreePaymentActivity} upon calling
     * {@link android.test.ActivityInstrumentationTestCase2#getActivity()}.
     *
     * @param testCase
     * @param clientToken
     */
    public static void setClientTokenExtraForTest(ActivityInstrumentationTestCase2<BraintreePaymentActivity> testCase,
            String clientToken) {
        Intent intent = new Intent(testCase.getInstrumentation().getContext(),
                BraintreePaymentActivity.class);
        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, clientToken);
        testCase.setActivityIntent(intent);
    }

    /**
     * Creates a mock {@link BraintreeApi} that will throw an {@link UnexpectedException} for any
     * network request made.
     *
     * @param context
     * @return
     * @throws BraintreeException
     * @throws ErrorWithResponse
     */
    public static BraintreeApi unexpectedExceptionThrowingApi(final Context context)
            throws BraintreeException, ErrorWithResponse {
        HttpRequest mockRequest = mock(HttpRequest.class);
        when(mockRequest.get(anyString())).thenThrow(new UnexpectedException("Mocked HTTP request"));
        when(mockRequest.post(anyString(), anyString())).thenThrow(
                new UnexpectedException("Mocked HTTP request"));

        ClientToken mockClientToken = mock(ClientToken.class);
        when(mockClientToken.getConfigUrl()).thenReturn("http://example.com/");
        return new BraintreeApi(context, mockClientToken, null, mockRequest);
    }

    /**
     * Creates a BraintreeApi that has not yet fetched configuration.
     *
     * @param context
     * @param clientToken
     * @return
     */
    public static BraintreeApi getNotSetupBraintreeApi(Context context, String clientToken)
            throws JSONException {
        return new BraintreeApi(context, ClientToken.fromString(clientToken));
    }

    /**
     * Post an unrecoverable error to the {@link com.braintreepayments.api.Braintree.ErrorListener}s
     * currently registered with the {@link Braintree} instance.
     *
     * @param braintree
     * @param exception
     */
    public static void postUnrecoverableErrorFromBraintree(Braintree braintree, BraintreeException exception) {
        braintree.postUnrecoverableErrorToListeners(exception);
    }

    /**
     * Verifies {@link BraintreeApi#setup()} has been called once on a mock or spy'd
     * {@link BraintreeApi}
     *
     * @param braintreeApi
     * @throws ErrorWithResponse
     * @throws BraintreeException
     */
    public static void verifySetupCalledOnBraintreeApi(BraintreeApi braintreeApi)
            throws ErrorWithResponse, BraintreeException, JSONException {
        verify(braintreeApi).setup();
    }
}
