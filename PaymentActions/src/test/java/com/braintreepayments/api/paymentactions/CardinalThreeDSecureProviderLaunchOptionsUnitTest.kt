package com.braintreepayments.api.paymentactions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.InvalidArgumentException
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.cardinalcommerce.cardinalmobilesdk.Cardinal
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalRenderType
import com.cardinalcommerce.cardinalmobilesdk.enums.CardinalUiType
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalConfigurationParameters
import com.cardinalcommerce.cardinalmobilesdk.services.CardinalInitService
import com.cardinalcommerce.shared.models.enums.ButtonType
import com.cardinalcommerce.shared.userinterfaces.ButtonCustomization
import com.cardinalcommerce.shared.userinterfaces.LabelCustomization
import com.cardinalcommerce.shared.userinterfaces.TextBoxCustomization
import com.cardinalcommerce.shared.userinterfaces.ToolbarCustomization
import com.cardinalcommerce.shared.userinterfaces.UiCustomization
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How [CardinalThreeDSecureProvider] applies [PaymentActionThreeDSecureOptions]. Runs on Robolectric because
 * the requestor app URL check resolves a real intent against the package manager.
 */
@RunWith(RobolectricTestRunner::class)
class CardinalThreeDSecureProviderLaunchOptionsUnitTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val cardinal = mockk<Cardinal>(relaxed = true)
    private val configured = mutableListOf<CardinalConfigurationParameters>()
    private val configuration = mockk<Configuration> {
        every { environment } returns "sandbox"
        every { cardinalAuthenticationJwt } returns "cardinal-jwt"
    }

    @Before
    fun setUp() {
        mockkStatic(Cardinal::class)
        every { Cardinal.getInstance() } returns cardinal
        every { cardinal.configure(any(), capture(configured)) } just Runs
        every { cardinal.init(any(), any<CardinalInitService>()) } answers {
            secondArg<CardinalInitService>().onSetupCompleted("df-ref")
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Cardinal::class)
    }

    private fun buildSut(options: PaymentActionThreeDSecureOptions) = CardinalThreeDSecureProvider(
        context,
        MockkBraintreeClientBuilder().configurationSuccess(configuration).build(),
        options,
    )

    /** Runs device data collection with [options] and returns the parameters Cardinal was configured with. */
    private suspend fun configureWith(options: PaymentActionThreeDSecureOptions): CardinalConfigurationParameters {
        assertIs<DeviceDataResult.Collected>(buildSut(options).collectDeviceData())
        return configured.last()
    }

    /** Runs device data collection with [uiCustomization] and returns the customization Cardinal received. */
    private suspend fun customizedWith(uiCustomization: PaymentActionThreeDSecureUiCustomization): UiCustomization =
        configureWith(PaymentActionThreeDSecureOptions(uiCustomization = uiCustomization)).uiCustomization

    /** Declares an activity in [packageName] whose intent filter matches [REQUESTOR_APP_URL]. */
    private fun declareReturnActivity(
        packageName: String = context.packageName,
        withDefaultCategory: Boolean = true,
        exported: Boolean = true,
        permission: String? = null,
    ) {
        val activity = ComponentName(packageName, "$packageName.ReturnActivity")
        val activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            name = activity.className
            this.exported = exported
            this.permission = permission
        }
        val filter = IntentFilter(Intent.ACTION_VIEW).apply {
            if (withDefaultCategory) addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            addDataScheme(REQUESTOR_APP_SCHEME)
        }
        shadowOf(context.packageManager).apply {
            addOrUpdateActivity(activityInfo)
            addIntentFilterForActivity(activity, filter)
        }
    }

    @Test
    fun `default options leave Cardinal's own defaults in place`() = runTest {
        val parameters = configureWith(PaymentActionThreeDSecureOptions())

        val cardinalDefaults = CardinalConfigurationParameters()
        assertEquals(cardinalDefaults.uiType, parameters.uiType)
        assertEquals(cardinalDefaults.renderType.toString(), parameters.renderType.toString())
        assertEquals(cardinalDefaults.threeDSRequestorAppURL, parameters.threeDSRequestorAppURL)
        assertNothingCustomized(parameters.uiCustomization)
    }

    @Test
    fun `each UI type maps to Cardinal's UI type`() = runTest {
        val expected = mapOf(
            PaymentActionThreeDSecureUiType.NATIVE to CardinalUiType.NATIVE,
            PaymentActionThreeDSecureUiType.HTML to CardinalUiType.HTML,
            PaymentActionThreeDSecureUiType.BOTH to CardinalUiType.BOTH,
        )

        assertEquals(PaymentActionThreeDSecureUiType.entries.toSet(), expected.keys)
        expected.forEach { (uiType, cardinalUiType) ->
            assertEquals(cardinalUiType, configureWith(PaymentActionThreeDSecureOptions(uiType = uiType)).uiType)
        }
    }

    @Test
    fun `render types map to Cardinal's render types in order`() = runTest {
        val options = PaymentActionThreeDSecureOptions(renderTypes = PaymentActionThreeDSecureRenderType.entries)

        val renderTypes = configureWith(options).renderType

        assertEquals(
            listOf(
                CardinalRenderType.OTP,
                CardinalRenderType.SINGLE_SELECT,
                CardinalRenderType.MULTI_SELECT,
                CardinalRenderType.OOB,
                CardinalRenderType.HTML,
            ),
            (0 until renderTypes.length()).map { renderTypes.get(it) },
        )
    }

    @Test
    fun `a button customization maps every field onto Cardinal's button customization`() = runTest {
        val customization = customizedWith(
            PaymentActionThreeDSecureUiCustomization(
                buttonCustomizations = mapOf(
                    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_NEXT to
                        PaymentActionThreeDSecureButtonCustomization(
                            textFontName = "Button Font",
                            textColor = "#000001",
                            textFontSize = 11,
                            backgroundColor = "#000002",
                            cornerRadius = 3,
                        ),
                ),
            )
        )

        customization.getButtonCustomization(ButtonType.NEXT).run {
            assertEquals("Button Font", textFontName)
            assertEquals("#000001", textColor)
            assertEquals(11, textFontSize)
            assertEquals("#000002", backgroundColor)
            assertEquals(3, cornerRadius)
        }
    }

    @Test
    fun `a label customization maps every field onto Cardinal's label customization`() = runTest {
        val customization = customizedWith(
            PaymentActionThreeDSecureUiCustomization(
                labelCustomization = PaymentActionThreeDSecureLabelCustomization(
                    textFontName = "Label Font",
                    textColor = "#000003",
                    textFontSize = 12,
                    headingTextColor = "#000004",
                    headingTextFontName = "Heading Font",
                    headingTextFontSize = 13,
                ),
            )
        )

        customization.labelCustomization.run {
            assertEquals("Label Font", textFontName)
            assertEquals("#000003", textColor)
            assertEquals(12, textFontSize)
            assertEquals("#000004", headingTextColor)
            assertEquals("Heading Font", headingTextFontName)
            assertEquals(13, headingTextFontSize)
        }
    }

    @Test
    fun `a text box customization maps every field onto Cardinal's text box customization`() = runTest {
        val customization = customizedWith(
            PaymentActionThreeDSecureUiCustomization(
                textBoxCustomization = PaymentActionThreeDSecureTextBoxCustomization(
                    textFontName = "Text Box Font",
                    textColor = "#000005",
                    textFontSize = 14,
                    borderWidth = 2,
                    borderColor = "#000006",
                    cornerRadius = 4,
                ),
            )
        )

        customization.textBoxCustomization.run {
            assertEquals("Text Box Font", textFontName)
            assertEquals("#000005", textColor)
            assertEquals(14, textFontSize)
            assertEquals(2, borderWidth)
            assertEquals("#000006", borderColor)
            assertEquals(4, cornerRadius)
        }
    }

    @Test
    fun `a toolbar customization maps every field onto Cardinal's toolbar customization`() = runTest {
        val customization = customizedWith(
            PaymentActionThreeDSecureUiCustomization(
                toolbarCustomization = PaymentActionThreeDSecureToolbarCustomization(
                    textFontName = "Toolbar Font",
                    textColor = "#000007",
                    textFontSize = 15,
                    backgroundColor = "#000008",
                    headerText = "Header",
                    buttonText = "Cancel",
                ),
            )
        )

        customization.toolbarCustomization.run {
            assertEquals("Toolbar Font", textFontName)
            assertEquals("#000007", textColor)
            assertEquals(15, textFontSize)
            assertEquals("#000008", backgroundColor)
            assertEquals("Header", headerText)
            assertEquals("Cancel", buttonText)
        }
    }

    @Test
    fun `every button type can be styled separately`() = runTest {
        val cardinalButtonTypes = mapOf(
            PaymentActionThreeDSecureButtonType.BUTTON_TYPE_VERIFY to ButtonType.VERIFY,
            PaymentActionThreeDSecureButtonType.BUTTON_TYPE_CONTINUE to ButtonType.CONTINUE,
            PaymentActionThreeDSecureButtonType.BUTTON_TYPE_NEXT to ButtonType.NEXT,
            PaymentActionThreeDSecureButtonType.BUTTON_TYPE_CANCEL to ButtonType.CANCEL,
            PaymentActionThreeDSecureButtonType.BUTTON_TYPE_RESEND to ButtonType.RESEND,
        )
        assertEquals(PaymentActionThreeDSecureButtonType.entries.toSet(), cardinalButtonTypes.keys)
        // A different color per button, so a button mapped to the wrong Cardinal type shows up.
        val colors = cardinalButtonTypes.keys.withIndex().associate { (index, type) -> type to "#00000$index" }

        val customization = customizedWith(
            PaymentActionThreeDSecureUiCustomization(
                buttonCustomizations = colors.mapValues { (_, color) ->
                    PaymentActionThreeDSecureButtonCustomization(backgroundColor = color)
                },
            )
        )

        cardinalButtonTypes.forEach { (buttonType, cardinalButtonType) ->
            assertEquals(colors[buttonType], customization.getButtonCustomization(cardinalButtonType).backgroundColor)
        }
    }

    @Test
    fun `button types without a customization keep the default look`() = runTest {
        val customization = customizedWith(
            PaymentActionThreeDSecureUiCustomization(
                buttonCustomizations = mapOf(
                    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_VERIFY to
                        PaymentActionThreeDSecureButtonCustomization(backgroundColor = "#000002"),
                ),
            )
        )

        ButtonType.values().filter { it != ButtonType.VERIFY }.forEach {
            assertNull(customization.getButtonCustomization(it))
        }
    }

    @Test
    fun `null texts and zero numbers keep Cardinal's defaults`() = runTest {
        val options = PaymentActionThreeDSecureOptions(
            uiCustomization = PaymentActionThreeDSecureUiCustomization(
                buttonCustomizations = mapOf(
                    PaymentActionThreeDSecureButtonType.BUTTON_TYPE_VERIFY to
                        PaymentActionThreeDSecureButtonCustomization(),
                ),
                labelCustomization = PaymentActionThreeDSecureLabelCustomization(),
                textBoxCustomization = PaymentActionThreeDSecureTextBoxCustomization(),
                toolbarCustomization = PaymentActionThreeDSecureToolbarCustomization(),
            ),
        )

        val customization = configureWith(options).uiCustomization

        val buttonDefaults = ButtonCustomization()
        customization.getButtonCustomization(ButtonType.VERIFY).run {
            assertEquals(buttonDefaults.textFontName, textFontName)
            assertEquals(buttonDefaults.textColor, textColor)
            assertEquals(buttonDefaults.textFontSize, textFontSize)
            assertEquals(buttonDefaults.backgroundColor, backgroundColor)
            assertEquals(buttonDefaults.cornerRadius, cornerRadius)
        }
        val labelDefaults = LabelCustomization()
        customization.labelCustomization.run {
            assertEquals(labelDefaults.textFontSize, textFontSize)
            assertEquals(labelDefaults.headingTextColor, headingTextColor)
            assertEquals(labelDefaults.headingTextFontName, headingTextFontName)
            assertEquals(labelDefaults.headingTextFontSize, headingTextFontSize)
        }
        val textBoxDefaults = TextBoxCustomization()
        customization.textBoxCustomization.run {
            assertEquals(textBoxDefaults.borderWidth, borderWidth)
            assertEquals(textBoxDefaults.borderColor, borderColor)
            assertEquals(textBoxDefaults.cornerRadius, cornerRadius)
        }
        val toolbarDefaults = ToolbarCustomization()
        customization.toolbarCustomization.run {
            assertEquals(toolbarDefaults.backgroundColor, backgroundColor)
            assertEquals(toolbarDefaults.headerText, headerText)
            assertEquals(toolbarDefaults.buttonText, buttonText)
        }
    }

    @Test
    fun `a UI customization Cardinal rejects fails naming it before Cardinal is configured`() = runTest {
        // Cardinal rejects an empty color
        val options = PaymentActionThreeDSecureOptions(
            uiCustomization = PaymentActionThreeDSecureUiCustomization(
                labelCustomization = PaymentActionThreeDSecureLabelCustomization(textColor = ""),
            ),
        )

        val result = buildSut(options).collectDeviceData()

        val error = assertIs<InvalidArgumentException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.rejectedCustomizationMessage("labelCustomization"), error.message)
        assertTrue(configured.isEmpty())
    }

    @Test
    fun `a requestor app URL that an activity in this app handles is sent to Cardinal`() = runTest {
        declareReturnActivity()

        val parameters = configureWith(PaymentActionThreeDSecureOptions(requestorAppUrl = REQUESTOR_APP_URL))

        assertEquals(REQUESTOR_APP_URL, parameters.threeDSRequestorAppURL)
    }

    @Test
    fun `a requestor app URL that no activity handles fails before Cardinal is configured`() = runTest {
        val sut = buildSut(PaymentActionThreeDSecureOptions(requestorAppUrl = REQUESTOR_APP_URL))

        val result = sut.collectDeviceData()

        val error = assertIs<InvalidArgumentException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertEquals(CardinalThreeDSecureProvider.requestorAppUrlNotHandledMessage(REQUESTOR_APP_URL), error.message)
        assertTrue(configured.isEmpty())
    }

    @Test
    fun `a requestor app URL that only another app handles fails`() = runTest {
        declareReturnActivity(packageName = "com.example.otherapp")

        val result = buildSut(PaymentActionThreeDSecureOptions(requestorAppUrl = REQUESTOR_APP_URL)).collectDeviceData()

        assertIs<InvalidArgumentException>(assertIs<DeviceDataResult.Failed>(result).error)
    }

    @Test
    fun `an intent filter without the DEFAULT category doesn't handle the requestor app URL`() = runTest {
        declareReturnActivity(withDefaultCategory = false)

        val result = buildSut(PaymentActionThreeDSecureOptions(requestorAppUrl = REQUESTOR_APP_URL)).collectDeviceData()

        assertIs<InvalidArgumentException>(assertIs<DeviceDataResult.Failed>(result).error)
    }

    @Test
    fun `a requestor app URL handled by an activity that isn't exported fails`() = runTest {
        declareReturnActivity(exported = false)

        val result = buildSut(PaymentActionThreeDSecureOptions(requestorAppUrl = REQUESTOR_APP_URL)).collectDeviceData()

        assertIs<InvalidArgumentException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertTrue(configured.isEmpty())
    }

    @Test
    fun `a requestor app URL handled by an activity that needs a permission fails`() = runTest {
        declareReturnActivity(permission = "com.example.merchant.permission.RETURN")

        val result = buildSut(PaymentActionThreeDSecureOptions(requestorAppUrl = REQUESTOR_APP_URL)).collectDeviceData()

        assertIs<InvalidArgumentException>(assertIs<DeviceDataResult.Failed>(result).error)
        assertTrue(configured.isEmpty())
    }

    @Test
    fun `presentChallenge fails the same way when no activity handles the requestor app URL`() = runTest {
        val sut = buildSut(PaymentActionThreeDSecureOptions(requestorAppUrl = REQUESTOR_APP_URL))

        val result = sut.presentChallenge("mpi-tx-id", "challenge-payload")

        assertIs<InvalidArgumentException>(assertIs<ChallengeResult.Failed>(result).error)
        assertTrue(configured.isEmpty())
    }

    private fun assertNothingCustomized(customization: UiCustomization) {
        ButtonType.values().forEach { assertNull(customization.getButtonCustomization(it)) }
        assertNull(customization.labelCustomization)
        assertNull(customization.textBoxCustomization)
        assertNull(customization.toolbarCustomization)
    }

    companion object {
        private const val REQUESTOR_APP_SCHEME = "com.example.merchant.payments"
        private const val REQUESTOR_APP_URL = "$REQUESTOR_APP_SCHEME://return"
    }
}
