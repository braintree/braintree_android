package com.braintreepayments.api.paypalsavedpaymentmethod.compose

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingRequestRepositoryUnitTest {

    private val dataStore = InMemoryDataStore()

    private fun TestScope.repository(
        key: String = "instance",
        dataStore: DataStore<Preferences> = this@PendingRequestRepositoryUnitTest.dataStore
    ) = PendingRequestRepository(mockk(), key, dataStore, StandardTestDispatcher(testScheduler))

    @Test
    fun `get returns null when nothing is stored`() = runTest {
        assertNull(repository().get())
    }

    @Test
    fun `get returns the stored pending request`() = runTest {
        val subject = repository()

        subject.store("pending-request")

        assertEquals("pending-request", subject.get())
    }

    @Test
    fun `clear removes the stored pending request`() = runTest {
        val subject = repository()
        subject.store("pending-request")

        subject.clear()

        assertNull(subject.get())
    }

    @Test
    fun `instances do not see each other's pending request`() = runTest {
        val first = repository("first")
        val second = repository("second")

        first.store("first-request")

        assertEquals("first-request", first.get())
        assertNull(second.get())
    }

    @Test
    fun `get returns null when the data store can't be read`() = runTest {
        val subject = repository(dataStore = InMemoryDataStore(data = flow { throw IOException("read failed") }))

        assertNull(subject.get())
    }

    @Test
    fun `clear ignores a failed write`() = runTest {
        val subject = repository(dataStore = InMemoryDataStore(failWrites = true))

        subject.clear()
    }

    @Test
    fun `store returns true when the request is written`() = runTest {
        assertTrue(repository().store("pending-request"))
    }

    @Test
    fun `store returns false instead of throwing when the write fails`() = runTest {
        assertFalse(repository(dataStore = InMemoryDataStore(failWrites = true)).store("pending-request"))
    }

    private class InMemoryDataStore(
        private val state: MutableStateFlow<Preferences> = MutableStateFlow(emptyPreferences()),
        override val data: Flow<Preferences> = state,
        private val failWrites: Boolean = false
    ) : DataStore<Preferences> {

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            if (failWrites) throw IOException("write failed")
            return transform(state.value).also { state.value = it }
        }
    }
}
