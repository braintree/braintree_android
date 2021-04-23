package com.braintreepayments.api;

import android.os.Parcel;

import com.google.android.gms.common.api.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class GooglePayExceptionUnitTest {

    @Test
    public void testGooglePayException_isSerializable() {
        Status status = new Status(1, "Some status message");
        GooglePayException exception = new GooglePayException("Some message", status);

        Parcel parcel = Parcel.obtain();
        exception.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePayException actual = GooglePayException.CREATOR.createFromParcel(parcel);

        assertEquals("Some message", actual.getMessage());
        assertEquals("Some status message", actual.getStatus().getStatusMessage());
        assertEquals(1, actual.getStatus().getStatusCode());
    }
}