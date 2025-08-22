package com.braintreepayments.api.googlepay

import android.os.Parcel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import com.google.android.gms.common.api.Status
import kotlinx.parcelize.parcelableCreator
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class GooglePayExceptionUnitTest {

        @Test
        fun `parcels GooglePayException successfully`() {
            val status = Status(1, "Some status message")
            val exception = GooglePayException("Some message", status)

            val parcel = Parcel.obtain().apply {
                exception.writeToParcel(this, 0)
                setDataPosition(0)
            }

            val actual = parcelableCreator<GooglePayException>().createFromParcel(parcel)

            assertEquals("Some message", actual.message)
            assertEquals("Some status message", actual.status?.statusMessage)
            assertEquals(1, actual.status?.statusCode)
        }
}
