package com.braintreepayments.api.uicomponents.cardfields

import android.content.Intent
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class CardFieldsTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    private fun launchActivity(): ActivityScenario<CardFieldsTestActivity> {
        val intent = Intent(
            InstrumentationRegistry.getInstrumentation().targetContext,
            CardFieldsTestActivity::class.java
        )
        return ActivityScenario.launch(intent)
    }

    private fun waitForMain() = instrumentation.waitForIdleSync()

    private fun CardFields.cardNumberEditText(): EditText =
        findViewById<CardNumberTextInputView>(R.id.card_fields_card_number_input)
            .findViewById(R.id.text_input_edit_text)

    private fun CardFields.expirationEditText(): EditText =
        findViewById<ExpirationTextInputView>(R.id.card_fields_expiration_input)
            .findViewById(R.id.text_input_edit_text)

    private fun CardFields.cvvEditText(): EditText =
        findViewById<CvvTextInputView>(R.id.card_fields_cvv_input)
            .findViewById(R.id.text_input_edit_text)

    private fun CardFields.cardNumberErrorLabel(): TextView =
        findViewById<CardNumberTextInputView>(R.id.card_fields_card_number_input)
            .findViewById(R.id.error_label)

    private fun CardFields.expirationErrorLabel(): TextView =
        findViewById<ExpirationTextInputView>(R.id.card_fields_expiration_input)
            .findViewById(R.id.error_label)

    private fun CardFields.cvvErrorLabel(): TextView =
        findViewById<CvvTextInputView>(R.id.card_fields_cvv_input)
            .findViewById(R.id.error_label)

    private fun CardFields.cardBrandContentDescription(): String? {
        val cardNumberInput = findViewById<CardNumberTextInputView>(R.id.card_fields_card_number_input)
        val inputContainer = cardNumberInput.findViewById<FrameLayout>(R.id.input_container)
        for (i in 0 until inputContainer.childCount) {
            val child = inputContainer.getChildAt(i)
            if (child is ImageView) return child.contentDescription?.toString()
        }
        return null
    }

    // region Pay button state

    @Test
    fun cardFields_payButton_isInitiallyDisabled() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                assertFalse(activity.payButton.isEnabled)
            }
        }
    }

    @Test
    fun cardFields_payButton_isEnabledAfterAllFieldsAreValid() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("4111111111111111")
                activity.cardFields.expirationEditText().setText("1245")
                activity.cardFields.cvvEditText().also { cvv ->
                    cvv.setText("123")
                    cvv.clearFocus()
                }
            }
            waitForMain()
            scenario.onActivity { activity ->
                assertTrue(activity.payButton.isEnabled)
            }
        }
    }

    // endregion

    // region Validation errors

    @Test
    fun cardFields_showsCardNumberError_afterIncompleteInputAndBlur() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().also { et ->
                    et.requestFocus()
                    et.setText("4111111111")
                }
                activity.cardFields.expirationEditText().requestFocus()
            }
            waitForMain()
            scenario.onActivity { activity ->
                val errorLabel = activity.cardFields.cardNumberErrorLabel()
                assertTrue(errorLabel.visibility == View.VISIBLE)
                assertTrue(errorLabel.text.isNotEmpty())
            }
        }
    }

    @Test
    fun cardFields_showsExpirationError_afterBlurWithoutInput() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.expirationEditText().requestFocus()
                activity.cardFields.cvvEditText().requestFocus()
            }
            waitForMain()
            scenario.onActivity { activity ->
                val errorLabel = activity.cardFields.expirationErrorLabel()
                assertTrue(errorLabel.visibility == View.VISIBLE)
                assertTrue(errorLabel.text.isNotEmpty())
            }
        }
    }

    @Test
    fun cardFields_showsCvvError_afterIncompleteInputAndBlur() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cvvEditText().also { et ->
                    et.requestFocus()
                    et.setText("12")
                }
                activity.cardFields.expirationEditText().requestFocus()
            }
            waitForMain()
            scenario.onActivity { activity ->
                val errorLabel = activity.cardFields.cvvErrorLabel()
                assertTrue(errorLabel.visibility == View.VISIBLE)
                assertTrue(errorLabel.text.isNotEmpty())
            }
        }
    }

    @Test
    fun cardFields_clearsCardNumberError_afterCorrectingInput() {
        launchActivity().use { scenario ->
            // First trigger the error
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().also { et ->
                    et.requestFocus()
                    et.setText("4111111111")
                }
                activity.cardFields.expirationEditText().requestFocus()
            }
            waitForMain()
            // Then correct the input
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().also { et ->
                    et.requestFocus()
                    et.setText("4111111111111111")
                }
                activity.cardFields.expirationEditText().requestFocus()
            }
            waitForMain()
            scenario.onActivity { activity ->
                val errorLabel = activity.cardFields.cardNumberErrorLabel()
                assertTrue(errorLabel.visibility == View.GONE)
            }
        }
    }

    // endregion

    // region Card Brand Detection

    @Test
    fun cardFields_displaysVisaBrand_whenCardStartsWith4() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("4")
            }
            waitForMain()
            scenario.onActivity { activity ->
                assertEquals("Visa", activity.cardFields.cardBrandContentDescription())
            }
        }
    }

    @Test
    fun cardFields_displaysMastercardBrand_whenCardStartsWith51() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("51")
            }
            waitForMain()
            scenario.onActivity { activity ->
                assertEquals("Mastercard", activity.cardFields.cardBrandContentDescription())
            }
        }
    }

    @Test
    fun cardFields_displaysAmexBrand_whenCardStartsWith34() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("34")
            }
            waitForMain()
            scenario.onActivity { activity ->
                assertEquals("American Express", activity.cardFields.cardBrandContentDescription())
            }
        }
    }

    @Test
    fun cardFields_displaysDiscoverBrand_whenCardStartsWith6011() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("6011")
            }
            waitForMain()
            scenario.onActivity { activity ->
                assertEquals("Discover", activity.cardFields.cardBrandContentDescription())
            }
        }
    }

    @Test
    fun cardFields_displaysUnionPayBrand_whenCardStartsWith620() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("620")
            }
            waitForMain()
            scenario.onActivity { activity ->
                assertEquals("UnionPay", activity.cardFields.cardBrandContentDescription())
            }
        }
    }

    // endregion

    // region CVV Length Revalidation

    @Test
    fun cardFields_showsCvvError_whenSwitchingFromVisaToAmex() {
        launchActivity().use { scenario ->
            // Enter a Visa card with a valid 3-digit CVV
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("4111111111111111")
                activity.cardFields.cvvEditText().also { cvv ->
                    cvv.requestFocus()
                    cvv.setText("123")
                }
            }
            waitForMain()
            // Switch to Amex — brand changes, 3-digit CVV is now too short (Amex requires 4)
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("378282246310005")
            }
            waitForMain()
            // Blur CVV to trigger validation against the new brand
            scenario.onActivity { activity ->
                activity.cardFields.expirationEditText().requestFocus()
            }
            waitForMain()
            scenario.onActivity { activity ->
                val errorLabel = activity.cardFields.cvvErrorLabel()
                assertTrue(errorLabel.visibility == View.VISIBLE)
                assertTrue(errorLabel.text.isNotEmpty())
            }
        }
    }

    @Test
    fun cardFields_showsCvvError_whenSwitchingFromAmexToVisa() {
        launchActivity().use { scenario ->
            // Enter an Amex card with a valid 4-digit CVV
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("378282246310005")
                activity.cardFields.cvvEditText().also { cvv ->
                    cvv.requestFocus()
                    cvv.setText("1234")
                }
            }
            waitForMain()
            // Switch to Visa — brand changes, 4-digit CVV is now too long (Visa requires 3)
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("4111111111111111")
            }
            waitForMain()
            // Blur CVV to trigger validation against the new brand
            scenario.onActivity { activity ->
                activity.cardFields.expirationEditText().requestFocus()
            }
            waitForMain()
            scenario.onActivity { activity ->
                val errorLabel = activity.cardFields.cvvErrorLabel()
                assertTrue(errorLabel.visibility == View.VISIBLE)
                assertTrue(errorLabel.text.isNotEmpty())
            }
        }
    }

    // endregion

    // region Configuration change (rotation)

    @Test
    fun cardFields_payButton_remainsEnabled_afterConfigurationChange() {
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.cardNumberEditText().setText("4111111111111111")
                activity.cardFields.expirationEditText().setText("1245")
                activity.cardFields.cvvEditText().also { cvv ->
                    cvv.setText("123")
                    cvv.clearFocus()
                }
            }
            waitForMain()
            scenario.onActivity { activity ->
                assertTrue("Pay button should be enabled before rotation", activity.payButton.isEnabled)
            }

            scenario.recreate()
            waitForMain()

            scenario.onActivity { activity ->
                assertTrue("Pay button should remain enabled after rotation", activity.payButton.isEnabled)
            }
        }
    }

    // endregion

    // region Submit before initialize

    @Test
    fun cardFields_submit_beforeInitialize_deliversFailureResult() {
        var result: CardFieldsResult? = null
        launchActivity().use { scenario ->
            scenario.onActivity { activity ->
                activity.cardFields.setCardFieldsResultCallback { result = it }
                activity.cardFields.submit()
            }
        }
        assertTrue(result is CardFieldsResult.Failure)
        assertTrue((result as CardFieldsResult.Failure).error is BraintreeException)
    }

    // endregion
}
