package com.paypal.android.sdk.onetouch.core;

import android.net.Uri;
import android.os.Parcel;

import com.paypal.android.sdk.onetouch.core.base.ContextInspector;
import com.paypal.android.sdk.onetouch.core.encryption.EncryptionUtils;
import com.paypal.android.sdk.onetouch.core.enums.ResultType;
import com.paypal.android.sdk.onetouch.core.exception.BrowserSwitchException;
import com.paypal.android.sdk.onetouch.core.exception.ResponseParsingException;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk = 19, constants = BuildConfig.class)
public class AuthorizationRequestTest {

    private AuthorizationRequest mRequest;
    private ContextInspector mContextInspector;

    @Before
    public void setup() {
        mRequest = new AuthorizationRequest(RuntimeEnvironment.application);
        mRequest.successUrl("com.braintreepayments.demo.braintree", "success");
        mRequest.cancelUrl("com.braintreepayments.demo.braintree", "cancel");
        mContextInspector = mock(ContextInspector.class);
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorForMissingPayloadInSuccessResponse() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response incomplete", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorForInvalidBase64PayloadInSuccessResponse() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payload=afjfi");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response incomplete", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorForInvalidJsonPayload() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payload=amFhZmpmamY=");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response incomplete", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorForMissingMsgGUIDInPayload() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payload=eyJ0ZXN0IjoidGVzdCJ9");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response incomplete", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorForMissingPayloadEnc() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payload=eyJtc2dfR1VJRCI6Im1zZ19HVUlEIn0=");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response invalid", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorWhenNoMsgGUIDInPreferences() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payload=eyJtc2dfR1VJRCI6Im1zZ19HVUlEIn0=&payloadEnc=encrypteddata");
        when(mContextInspector.getStringPreference("com.paypal.otc.msg_guid")).thenReturn("");
        when(mContextInspector.getStringPreference("com.paypal.otc.key")).thenReturn("key");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response invalid", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorForInvalidMsgGUID() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payload=eyJtc2dfR1VJRCI6Im1zZ19HVUlEIn0=&payloadEnc=encrypteddata");
        when(mContextInspector.getStringPreference("com.paypal.otc.msg_guid")).thenReturn("original-msg-guid");
        when(mContextInspector.getStringPreference("com.paypal.otc.key")).thenReturn("key");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertEquals("Response invalid", result.getError().getMessage());
        assertTrue(result.getError() instanceof ResponseParsingException);
    }

    @Test
    public void parseBrowserSwitchResponse_returnsErrorWhenNoEncryptionKeyInPreferences() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payload=eyJtc2dfR1VJRCI6Im1zZ19HVUlEIn0=&payloadEnc=encrypteddata");
        when(mContextInspector.getStringPreference("com.paypal.otc.msg_guid")).thenReturn("msg_GUID");
        when(mContextInspector.getStringPreference("com.paypal.otc.key")).thenReturn("");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response invalid", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_parsesSuccessResponses() throws JSONException {
        setMsgGUID("19342c28-c6d3-4948-a989-8ee1d40dae5c");
        setEncryptionKey("96BF26A3851D0127AF474CC556D9C296A9A02171CD319EE5249C4B3033FC7BB6");
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payloadEnc=9kuRyRmR8PpMoc3umclWyuGeibf4%2FIWdDyF9ehFL0TWI33GJ83iAHC7NdZszNRXrsa5uvO20N6BpGCKm3M%2F%2FjXzI5XrkxHhpKnH4j96mLNlTjDI2012cUZ7hotqm8rahnFkobykEwfI6OTGu3Zk%2Bc6FQ4Vi44nEeIw7Ocs%2Fiw%2BwzXGaZz3LgJajediKcwL%2BrDJIRgoZTnjzT2aM2No7wvORpAk%2FBRmAp1QxZkzTor0zcYgPEia%2B%2FivuEDRFKDUIC%2BC4GRulZB70Bzo7FBSvSOyy1rldRjaLg9W8bhBd8WOXjyg%2F0gLryjIqppmA%2FWFwUH7nBrbqpOyCyp5hskE5NZSNFZEokUEh4oOHI%2BMQshZETuivdWEjNrvbWq8bp2JHv5fnM9NjqmHY%2Bu23sEHQoJQ%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6IjE5MzQyYzI4LWM2ZDMtNDk0OC1hOTg5LThlZTFkNDBkYWU1YyIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9&x-source=com.braintree.browserswitch");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Success, result.getResultType());
        JSONAssert.assertEquals("{\"response\":{\"code\":\"fake_code\"},\"client\":{\"environment\":\"mock\",\"product_name\":\"PayPalOneTouch-Android\",\"platform\":\"Android\",\"paypal_sdk_version\":\"" + BuildConfig.PRODUCT_VERSION + "\"},\"response_type\":\"authorization_code\",\"user\":{\"display_string\":\"test@test.com\"}}", result.getResponse().toString(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void parseBrowserSwitchResponse_parsesSuccessResponseAndReturnsErrorWhenErrorIsPresent() {
        setMsgGUID("19342c28-c6d3-4948-a989-8ee1d40dae5c");
        setEncryptionKey("96BF26A3851D0127AF474CC556D9C296A9A02171CD319EE5249C4B3033FC7BB6");
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payloadEnc=9kuRyRmR8PpMoc3umclWyuGeibf4%2FIWdDyF9ehFL0TWI33GJ83iAHC7NdZszNRXrsa5uvO20N6BpGCKm3M%2F%2FjXzI5XrkxHhpKnH4j96mLNlTjDI2012cUZ7hotqm8rahnFkobykEwfI6OTGu3Zk%2Bc6FQ4Vi44nEeIw7Ocs%2Fiw%2BwzXGaZz3LgJajediKcwL%2BrDJIRgoZTnjzT2aM2No7wvORpAk%2FBRmAp1QxZkzTor0zcYgPEia%2B%2FivuEDRFKDUIC%2BC4GRulZB70Bzo7FBSvSOyy1rldRjaLg9W8bhBd8WOXjyg%2F0gLryjIqppmA%2FWFwUH7nBrbqpOyCyp5hskE5NZSNFZEokUEh4oOHI%2BMQshZETuivdWEjNrvbWq8bp2JHv5fnM9NjqmHY%2Bu23sEHQoJQ%3D%3D&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6IjE5MzQyYzI4LWM2ZDMtNDk0OC1hOTg5LThlZTFkNDBkYWU1YyIsImVycm9yIjoiVGhlcmUgd2FzIGFuIGVycm9yIn0=");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof BrowserSwitchException);
        assertEquals("There was an error", result.getError().getMessage());
    }

    @Test
    public void parseBrowserSwitchResponse_parsesSuccessResponsesAndReturnsErrorWhenInvalidPayloadEncIsReturned() {
        setMsgGUID("19342c28-c6d3-4948-a989-8ee1d40dae5c");
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/success?payloadEnc=9kuRy&payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6IjE5MzQyYzI4LWM2ZDMtNDk0OC1hOTg5LThlZTFkNDBkYWU1YyIsInJlc3BvbnNlX3R5cGUiOiJjb2RlIiwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOm51bGx9");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("java.lang.IllegalArgumentException: bad base-64", result.getError().getMessage());
    }

    @Test
    public void parseBrowserResponse_parsesErrorsFromCancelResponses() throws JSONException {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/cancel?payload=eyJ2ZXJzaW9uIjozLCJtc2dfR1VJRCI6bnVsbCwicmVzcG9uc2VfdHlwZSI6bnVsbCwiZW52aXJvbm1lbnQiOiJtb2NrIiwiZXJyb3IiOnsiZGVidWdfaWQiOm51bGwsIm1lc3NhZ2UiOiJFbmNyeXB0ZWQgcGF5bG9hZCBoYXMgZXhwaXJlZCJ9LCJsYW5ndWFnZSI6bnVsbH0=");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof BrowserSwitchException);
        JSONAssert.assertEquals("{\"debug_id\":null,\"message\":\"Encrypted payload has expired\"}",
                result.getError().getMessage(), JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void parseBrowserSwitchResponse_parsesCancelResponses() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/cancel");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Cancel, result.getResultType());
    }

    @Test
    public void parseBrowserResponse_returnsErrorForUnrecongnizedUri() {
        Uri uri = Uri.parse("com.braintreepayments.demo.braintree://onetouch/v1/abort?payload=eyJ0ZXN0IjoidGVzdCJ9");

        Result result = mRequest.parseBrowserResponse(mContextInspector, uri);

        assertEquals(ResultType.Error, result.getResultType());
        assertTrue(result.getError() instanceof ResponseParsingException);
        assertEquals("Response uri invalid", result.getError().getMessage());
    }

    @Test
    public void parcels() {
        AuthorizationRequest request = new AuthorizationRequest(RuntimeEnvironment.application);
        request.environment("test");
        request.clientId("client-id");
        request.clientMetadataId("client-metadata-id");
        request.cancelUrl("com.braintreepayments.demo.braintree.cancel", "cancel");
        request.successUrl("com.braintreepayments.demo.braintree.success", "success");
        request.privacyUrl("privacy-url");
        request.userAgreementUrl("user-agreement-url");
        request.withAdditionalPayloadAttribute("payload-key", "payload-value");
        request.withScopeValue("email");
        request.withScopeValue("address");

        Parcel parcel = Parcel.obtain();
        request.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        AuthorizationRequest parceledRequest = AuthorizationRequest.CREATOR.createFromParcel(parcel);

        assertEquals("test", parceledRequest.getEnvironment());
        assertEquals("client-id", parceledRequest.getClientId());
        assertEquals("client-metadata-id", parceledRequest.getClientMetadataId());
        assertEquals("com.braintreepayments.demo.braintree.cancel://onetouch/v1/cancel", parceledRequest.getCancelUrl());
        assertEquals("com.braintreepayments.demo.braintree.success://onetouch/v1/success", parceledRequest.getSuccessUrl());
        assertEquals("privacy-url", parceledRequest.getPrivacyUrl());
        assertEquals("user-agreement-url", parceledRequest.getUserAgreementUrl());
        assertEquals("{payload-key=payload-value}", parceledRequest.getAdditionalPayloadAttributes().toString());
        assertEquals("address email", parceledRequest.getScopeString());
    }

    private void setMsgGUID(String msgGUID) {
        try {
            Field field = AuthorizationRequest.class.getDeclaredField("mMsgGuid");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(mRequest, msgGUID);
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        }
    }

    private void setEncryptionKey(String encryptionKey) {
        try {
            Field field = AuthorizationRequest.class.getDeclaredField("mEncryptionKey");
            field.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            field.set(mRequest, EncryptionUtils.hexStringToByteArray(encryptionKey));
        } catch (NoSuchFieldException e) {
            fail(e.getMessage());
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
        }
    }
}
