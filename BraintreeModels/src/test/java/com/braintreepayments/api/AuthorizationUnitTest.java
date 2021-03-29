package com.braintreepayments.api;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AuthorizationUnitTest {

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
