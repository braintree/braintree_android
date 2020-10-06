package com.braintreepayments.api.exceptions;

import android.os.Parcel;

import com.google.android.gms.common.api.Status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class GooglePaymentExceptionTest {

    @Test
    public void testGooglePaymentException_isSerializable() {
        Status status = new Status(1, "Some status message");
        GooglePaymentException exception = new GooglePaymentException("Some message", status);

        Parcel parcel = Parcel.obtain();
        exception.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);

        GooglePaymentException actual = GooglePaymentException.CREATOR.createFromParcel(parcel);

        assertEquals("Some message", actual.getMessage());
        assertEquals("Some status message", actual.getStatus().getStatusMessage());
        assertEquals(1, actual.getStatus().getStatusCode());
    }
}
