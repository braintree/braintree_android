package com.braintreepayments.api.models;

import android.os.Parcel;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.testutils.Fixtures;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationUnitTest {

    @Test
    public void fromString_returnsValidClientTokenWhenBase64() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);

        assertTrue(authorization instanceof ClientToken);
    }

    @Test
    public void fromString_returnsValidTokenizationKey() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);

        assertTrue(authorization instanceof TokenizationKey);
    }

    @Test
    public void fromString_returnsValidPayPalUAT() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_PAYPAL_UAT);

        assertTrue(authorization instanceof PayPalUAT);
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
    public void parcelable_parcelsClientTokenCorrectly() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN);
        Parcel parcel = Parcel.obtain();
        authorization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        ClientToken parceled = ClientToken.CREATOR.createFromParcel(parcel);

        assertEquals(authorization.toString(), parceled.toString());
        assertEquals(authorization.getBearer(), parceled.getBearer());
        assertEquals(authorization.getConfigUrl(), parceled.getConfigUrl());
        assertEquals(((ClientToken) authorization).getAuthorizationFingerprint(),
                parceled.getAuthorizationFingerprint());
    }

    @Test
    public void parcelable_parcelsTokenizationKeyCorrectly() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY);
        Parcel parcel = Parcel.obtain();
        authorization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        TokenizationKey parceled = TokenizationKey.CREATOR.createFromParcel(parcel);

        assertEquals(authorization.toString(), parceled.toString());
        assertEquals(authorization.getBearer(), parceled.getBearer());
        assertEquals(authorization.getConfigUrl(), parceled.getConfigUrl());
        assertEquals(((TokenizationKey) authorization).getEnvironment(), parceled.getEnvironment());
        assertEquals(((TokenizationKey) authorization).getMerchantId(), parceled.getMerchantId());
        assertEquals(((TokenizationKey) authorization).getUrl(), parceled.getUrl());
    }

    @Test
    public void parcelable_parcelsPayPalUATCorrectly() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(Fixtures.BASE64_PAYPAL_UAT);
        Parcel parcel = Parcel.obtain();
        authorization.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        PayPalUAT parceled = PayPalUAT.CREATOR.createFromParcel(parcel);

        assertEquals(authorization.toString(), parceled.toString());
        assertEquals(authorization.getBearer(), parceled.getBearer());
        assertEquals(authorization.getConfigUrl(), parceled.getConfigUrl());
        assertEquals(((PayPalUAT) authorization).getPayPalURL(), parceled.getPayPalURL());
    }
}
