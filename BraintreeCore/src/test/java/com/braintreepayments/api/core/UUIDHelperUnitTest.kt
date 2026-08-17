package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.BraintreeSharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

import org.junit.Test
import java.util.*

class UUIDHelperUnitTest {
    private var braintreeSharedPreferences: BraintreeSharedPreferences = mockk(relaxed = true)

    @Test
    fun `when no GUID exists in shared preferences, getInstallationGUID generates and persists a new one`() {
        every {
            braintreeSharedPreferences.getString("InstallationGUID", null)
        } returns null

        val sut = UUIDHelper()
        val uuid = sut.getInstallationGUID(braintreeSharedPreferences)
        assertNotNull(uuid)
        verify { braintreeSharedPreferences.putString("InstallationGUID", uuid) }
    }

    @Test
    fun `when a GUID already exists in shared preferences, getInstallationGUID returns the existing one`() {
        val uuid = UUID.randomUUID().toString()
        every {
            braintreeSharedPreferences.getString("InstallationGUID", null)
        } returns uuid
        val sut = UUIDHelper()
        assertEquals(uuid, sut.getInstallationGUID(braintreeSharedPreferences))
    }
}
