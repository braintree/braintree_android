package com.braintreepayments.api.uicomponents.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.braintreepayments.api.uicomponents.util.CoroutineTestRule
import com.braintreepayments.api.uicomponents.util.DataStoreMock
import io.mockk.mockk
import kotlin.test.AfterTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PayPalPendingRequestRepositoryTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var subject: PayPalPendingRequestRepository

    @Before
    fun setUp() {
        dataStore = DataStoreMock()
        subject = PayPalPendingRequestRepository(
            context = mockk(),
            dataStore = dataStore,
            dispatcher = coroutineTestRule.testDispatcher
        )
    }

    @Test
    fun `storePendingRequest stores the pending request in the DataStore`() = runTest {
        val pendingRequest = "test-pending-request"
        subject.storePendingRequest(pendingRequest)

        val storedValue = dataStore.data.first()[stringPreferencesKey("pending_request_key")]
        assertEquals(pendingRequest, storedValue)
    }

    @Test
    fun `getPendingRequest returns the stored value`() = runTest {
        val pendingRequest = "test-pending-request"
        dataStore.edit { it[stringPreferencesKey("pending_request_key")] = pendingRequest }

        val result = subject.getPendingRequest()
        assertEquals(pendingRequest, result)
    }

    @Test
    fun `getPendingRequest returns null when no value is stored`() = runTest {
        val result = subject.getPendingRequest()
        assertNull(result)
    }

    @Test
    fun `clearPendingRequest removes the stored value`() = runTest {
        dataStore.edit { it[stringPreferencesKey("pending_request_key")] = "test-pending-request" }

        subject.clearPendingRequest()

        val storedValue = dataStore.data.first()[stringPreferencesKey("pending_request_key")]
        assertNull(storedValue)
    }

    @AfterTest
    fun forceFailIfException() {
        runTest { }
    }
}
