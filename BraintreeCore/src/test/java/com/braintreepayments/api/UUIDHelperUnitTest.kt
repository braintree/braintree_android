package com.braintreepayments.api

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
    fun getInstallationGUID_returnsNewGUIDWhenOneDoesNotExistAndPersistsIt() {
        every {
            braintreeSharedPreferences.getString("InstallationGUID", null)
        } returns null

        val sut = UUIDHelper()
        val uuid = sut.getInstallationGUID(braintreeSharedPreferences)
        assertNotNull(uuid)
        verify { braintreeSharedPreferences.putString("InstallationGUID", uuid) }
    }

    @Test
    fun getInstallationGUID_returnsExistingGUIDWhenOneExist() {
        val uuid = UUID.randomUUID().toString()
        every {
            braintreeSharedPreferences.getString("InstallationGUID", null)
        } returns uuid
        val sut = UUIDHelper()
        assertEquals(uuid, sut.getInstallationGUID(braintreeSharedPreferences))
    }
}
