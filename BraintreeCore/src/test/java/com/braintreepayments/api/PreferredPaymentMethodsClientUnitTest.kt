package com.braintreepayments.api

import android.content.Context
import com.braintreepayments.api.Configuration.Companion.fromJson
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.robolectric.RobolectricTestRunner
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.Exception

@RunWith(RobolectricTestRunner::class)
class PreferredPaymentMethodsClientUnitTest {

    private var context: Context = mockk(relaxed = true)
    private var applicationContext: Context = mockk(relaxed = true)
    private var graphQLEnabledConfiguration = fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
    private var graphQLDisabledConfiguration = fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
    private var deviceInspector: DeviceInspector = mockk(relaxed = true)

    @Before
    @Throws(JSONException::class)
    fun setUp() {
        every { context.applicationContext } returns applicationContext
    }

    @Test
    @Throws(Exception::class)
    fun fetchPreferredPaymentMethods_whenPayPalAppIsInstalled_callsListenerWithTrue() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        every { deviceInspector.isPayPalInstalled(applicationContext) } returns true

        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isPayPalPreferred())
                verify { braintreeClient.sendAnalyticsEvent("preferred-payment-methods.paypal.app-installed.true") }
            }
        })
    }

    @Test
    @Throws(Exception::class)
    fun fetchPreferredPaymentMethods_whenVenmoAppIsInstalled_callsListenerWithTrue() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        every { braintreeClient.getConfiguration(any()) } answers {
            firstArg<ConfigurationCallback>().onResult(graphQLDisabledConfiguration, null)
        }

        every { deviceInspector.isVenmoInstalled(applicationContext) } returns true

        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isVenmoPreferred())
                verify { braintreeClient.sendAnalyticsEvent("preferred-payment-methods.venmo.app-installed.true") }
            }
        })
    }

    @Test
    @Throws(InterruptedException::class)
    fun fetchPreferredPaymentMethods_whenVenmoAppIsNotInstalled_callsListenerWithFalseForVenmo() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        every { braintreeClient.getConfiguration(any()) } answers {
            firstArg<ConfigurationCallback>().onResult(graphQLDisabledConfiguration, null)
        }

        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isVenmoPreferred())
                verify { braintreeClient.sendAnalyticsEvent("preferred-payment-methods.venmo.app-installed.false") }
            }
        })
    }

    @Test
    @Throws(InterruptedException::class)
    fun fetchPreferredPaymentMethods_sendsQueryToGraphQL() {
        val response = "{\"data\": {\"preferredPaymentMethods\": {\"paypal\": true}}}"
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { braintreeClient.getConfiguration(any()) } answers {
            firstArg<ConfigurationCallback>().onResult(graphQLEnabledConfiguration, null)
        }
        every { braintreeClient.sendGraphQLPOST(any(), any()) } answers {
            secondArg<HttpResponseCallback>().onResult(response, null)
        }

        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                val querySlot = slot<String>()

                verify { braintreeClient.sendGraphQLPOST(capture(querySlot), ofType(HttpResponseCallback::class)) }

                val expectedQuery =
                    "{ \"query\": \"query PreferredPaymentMethods { preferredPaymentMethods { paypalPreferred } }\" }"
                assertEquals(expectedQuery, querySlot.captured)
            }
        })
    }

    @Test
    @Throws(InterruptedException::class)
    fun fetchPreferredPaymentMethods_whenGraphQLIsNotEnabled_andPayPalAppNotInstalled_callsListenerWithFalseForPayPal() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        every { braintreeClient.getConfiguration(any()) } answers {
            firstArg<ConfigurationCallback>().onResult(graphQLDisabledConfiguration, null)
        }
        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred())
                assertFalse(preferredPaymentMethodsResult.isVenmoPreferred())
                verify {
                    braintreeClient.sendAnalyticsEvent("preferred-payment-methods.api-disabled")
                }
            }
        })
    }

    @Test
    @Throws(InterruptedException::class)
    fun fetchPreferredPaymentMethods_whenApiDetectsPayPalPreferred_callsListenerWithTrueForPayPal() {
        val response = "{\"data\": {\"preferredPaymentMethods\": {\"paypalPreferred\": true}}}"
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { braintreeClient.getConfiguration(any()) } answers {
            firstArg<ConfigurationCallback>().onResult(graphQLEnabledConfiguration, null)
        }
        every { braintreeClient.sendGraphQLPOST(any(), any()) } answers {
            secondArg<HttpResponseCallback>().onResult(response, null)
        }

        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                assertTrue(preferredPaymentMethodsResult.isPayPalPreferred())
                verify {
                    braintreeClient.sendAnalyticsEvent("preferred-payment-methods.paypal.api-detected.true")
                }
            }
        })
    }

    @Test
    @Throws(InterruptedException::class)
    fun fetchPreferredPaymentMethods_whenApiDetectsPayPalNotPreferred_callsListenerWithFalseForPayPal() {
        val response = "{\"data\": {\"preferredPaymentMethods\": {\"paypalPreferred\": false}}}"
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { braintreeClient.getConfiguration(any()) } answers {
            firstArg<ConfigurationCallback>().onResult(graphQLEnabledConfiguration, null)
        }
        every { braintreeClient.sendGraphQLPOST(any(), any()) } answers {
            secondArg<HttpResponseCallback>().onResult(response, null)
        }

        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred())
                verify {
                    braintreeClient.sendAnalyticsEvent("preferred-payment-methods.paypal.api-detected.false")
                }
            }
        })
    }

    @Test
    @Throws(InterruptedException::class)
    fun fetchPreferredPaymentMethods_whenGraphQLReturnsError_callsListenerWithFalseForPayPal() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { braintreeClient.getConfiguration(any()) } answers {
            firstArg<ConfigurationCallback>().onResult(graphQLEnabledConfiguration, null)
        }
        every { braintreeClient.sendGraphQLPOST(any(), any()) } answers {
            secondArg<HttpResponseCallback>().onResult(null, Exception())
        }

        val sut = PreferredPaymentMethodsClient(braintreeClient, deviceInspector)
        sut.fetchPreferredPaymentMethods(context, object : PreferredPaymentMethodsCallback {
            override fun onResult(preferredPaymentMethodsResult: PreferredPaymentMethodsResult) {
                assertFalse(preferredPaymentMethodsResult.isPayPalPreferred())
                verify {
                    braintreeClient.sendAnalyticsEvent("preferred-payment-methods.api-error")
                }
            }
        })
    }
}