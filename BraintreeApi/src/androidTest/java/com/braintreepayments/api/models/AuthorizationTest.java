package com.braintreepayments.api.models;

import android.os.Parcel;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import com.braintreepayments.api.exceptions.InvalidArgumentException;

import org.junit.Test;
import org.junit.runner.RunWith;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class AuthorizationTest {

    @Test(timeout = 1000)
    public void fromString_returnsValidClientTokenWhenBase64() throws InvalidArgumentException {
        String clientTokenString = stringFromFixture("base_64_client_token.txt");
        Authorization authorization = Authorization.fromString(clientTokenString);
        assertTrue(authorization instanceof ClientToken);
    }

    @Test(timeout = 1000)
    public void fromString_returnsValidClientTokenWhenJSON() throws InvalidArgumentException {
        String clientTokenString = stringFromFixture("client_token.json");
        Authorization authorization = Authorization.fromString(clientTokenString);
        assertTrue(authorization instanceof ClientToken);
    }

    @Test(timeout = 1000)
    public void fromString_returnsValidClientKey() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(CLIENT_KEY);
        assertTrue(authorization instanceof ClientKey);
    }

    @Test(timeout = 1000, expected = InvalidArgumentException.class)
    public void fromString_throwsWhenPassedJunk() throws InvalidArgumentException {
        Authorization.fromString("I am not an authorization thing");
    }

    @Test(timeout = 1000)
    public void getType_returnsClientKey() throws InvalidArgumentException {
        Authorization authorization = Authorization.fromString(CLIENT_KEY);
        assertTrue(authorization instanceof ClientKey);
    }

    @Test(timeout = 1000)
    public void getType_returnsClientToken() throws InvalidArgumentException {
        String clientTokenString = stringFromFixture("base_64_client_token.txt");
        Authorization authorization = Authorization.fromString(clientTokenString);

    }

    @Test(timeout = 1000)
    public void parcelable_parcelsClientTokenCorrectly() throws InvalidArgumentException {
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);

        String clientTokenString = stringFromFixture("base_64_client_token.txt");
        Authorization authorization = Authorization.fromString(clientTokenString);
        authorization.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        Authorization authorization1 = new ClientToken(parcel);
        assertEquals(authorization.toString(), authorization1.toString());
        assertEquals(authorization.getConfigUrl(), authorization1.getConfigUrl());
        assertEquals(((ClientToken) authorization).getAuthorizationFingerprint(),
                ((ClientToken) authorization1).getAuthorizationFingerprint());
    }

    @Test(timeout = 1000)
    public void parcelable_parcelsClientKeyCorrectly() throws InvalidArgumentException {
        Parcel parcel = Parcel.obtain();
        parcel.setDataPosition(0);

        Authorization authorization = Authorization.fromString(CLIENT_KEY);
        authorization.writeToParcel(parcel, 0);

        parcel.setDataPosition(0);

        Authorization authorization1 = new ClientKey(parcel);
        assertEquals(authorization.toString(), authorization1.toString());
        assertEquals(authorization.getConfigUrl(), authorization1.getConfigUrl());
        assertEquals(((ClientKey) authorization).getEnvironment(), ((ClientKey) authorization1).getEnvironment());
        assertEquals(((ClientKey) authorization).getMerchantId(), ((ClientKey) authorization1).getMerchantId());
        assertEquals(((ClientKey) authorization).getUrl(), ((ClientKey) authorization1).getUrl());
    }
}
