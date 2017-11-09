package com.braintreepayments.api.models;

import android.os.Parcel;

import com.braintreepayments.api.exceptions.InvalidArgumentException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationUnitTest {

    @Test
    public void fromString_returnsValidClientTokenWhenBase64() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));

        assertTrue(authorization instanceof ClientToken);
    }

    @Test
    public void fromString_returnsValidClientTokenWhenJSON() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("client_token.json"));

        assertTrue(authorization instanceof ClientToken);
    }

    @Test
    public void fromString_returnsValidTokenizationKey() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(TOKENIZATION_KEY);

        assertTrue(authorization instanceof TokenizationKey);
    }

    @Test(expected = InvalidArgumentException.class)
    public void fromString_throwsWhenPassedNull() throws InvalidArgumentException {
        Authorization.fromString(null);
    }

    @Test(expected = InvalidArgumentException.class)
    public void fromString_throwsWhenPassedAnEmptyString() throws InvalidArgumentException {
        Authorization.fromString("");
    }

    @Test(expected = InvalidArgumentException.class)
    public void fromString_throwsWhenPassedJunk() throws InvalidArgumentException {
        Authorization.fromString("not authorization");
    }

    @Test
    public void getType_returnsTokenizationKey() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(TOKENIZATION_KEY);

        assertTrue(authorization instanceof TokenizationKey);
    }

    @Test
    public void getType_returnsClientToken() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));

        assertTrue(authorization instanceof ClientToken);
    }

    @Test
    public void parcelable_parcelsClientTokenCorrectly() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(stringFromFixture("base_64_client_token.txt"));
        Parcel parcel = Parcel.obtain();
        authorization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Authorization parceled = ClientToken.CREATOR.createFromParcel(parcel);

        assertEquals(authorization.toString(), parceled.toString());
        assertEquals(authorization.getAuthorization(), parceled.getAuthorization());
        assertEquals(authorization.getConfigUrl(), parceled.getConfigUrl());
        assertEquals(((ClientToken) authorization).getAuthorizationFingerprint(),
                ((ClientToken) parceled).getAuthorizationFingerprint());
    }

    @Test
    public void parcelable_parcelsTokenizationKeyCorrectly() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(TOKENIZATION_KEY);
        Parcel parcel = Parcel.obtain();
        authorization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        Authorization parceled = TokenizationKey.CREATOR.createFromParcel(parcel);

        assertEquals(authorization.toString(), parceled.toString());
        assertEquals(authorization.getAuthorization(), parceled.getAuthorization());
        assertEquals(authorization.getConfigUrl(), parceled.getConfigUrl());
        assertEquals(((TokenizationKey) authorization).getEnvironment(), ((TokenizationKey) parceled).getEnvironment());
        assertEquals(((TokenizationKey) authorization).getMerchantId(), ((TokenizationKey) parceled).getMerchantId());
        assertEquals(((TokenizationKey) authorization).getUrl(), ((TokenizationKey) parceled).getUrl());
    }
}
