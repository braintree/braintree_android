package com.braintreepayments.api.uicomponents.cardfields

import android.app.Activity
import android.os.Parcel
import android.os.Parcelable
import android.text.InputFilter
import android.util.SparseArray
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.braintreepayments.api.uicomponents.R
import com.braintreepayments.api.uicomponents.util.CoroutineTestRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CardFieldsUnitTest {

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()

    private lateinit var activity: Activity
    private lateinit var viewModel: CardFieldsViewModel

    // Controllable backing flows so each test can drive the ViewModel's outputs directly.
    private val cardNumberValidation = MutableStateFlow<ValidationResult>(ValidationResult.Validating)
    private val expirationValidation = MutableStateFlow<ValidationResult>(ValidationResult.Validating)
    private val cvvValidation = MutableStateFlow<ValidationResult>(ValidationResult.Validating)
    private val detectedCardBrand = MutableStateFlow(CardBrand.UNKNOWN)
    private val isFormValid = MutableStateFlow(false)

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()

        viewModel = mockk(relaxed = true)
        every { viewModel.cardNumberValidation } returns cardNumberValidation
        every { viewModel.expirationValidation } returns expirationValidation
        every { viewModel.cvvValidation } returns cvvValidation
        every { viewModel.detectedCardBrand } returns detectedCardBrand
        every { viewModel.isFormValid } returns isFormValid
    }

    private fun createCardFields() = CardFields(activity, viewModel = viewModel)

    private fun CardFields.attach() = activity.setContentView(this)

    private fun CardFields.cardNumberView() =
        findViewById<CardNumberTextInputView>(R.id.card_fields_card_number_input)

    private fun CardFields.expirationView() =
        findViewById<ExpirationTextInputView>(R.id.card_fields_expiration_input)

    private fun CardFields.cvvView() =
        findViewById<CvvTextInputView>(R.id.card_fields_cvv_input)

    private fun BaseTextInputView.editText() =
        findViewById<EditText>(R.id.text_input_edit_text)

    private fun BaseTextInputView.errorLabel() =
        findViewById<TextView>(R.id.error_label)

    // region Inflation

    @Test
    fun `inflates all three child input views`() {
        val cardFields = createCardFields()

        assertTrue(cardFields.cardNumberView() != null)
        assertTrue(cardFields.expirationView() != null)
        assertTrue(cardFields.cvvView() != null)
    }

    // endregion

    // region Forwarding text changes

    @Test
    fun `typing a card number forwards the raw digits to the view model`() {
        val cardFields = createCardFields()

        cardFields.cardNumberView().setText("4111111111111111")

        verify { viewModel.onCardNumberChanged("4111111111111111") }
    }

    @Test
    fun `typing an expiration forwards the raw digits to the view model`() {
        val cardFields = createCardFields()

        cardFields.expirationView().setText("12/26")

        verify { viewModel.onExpiryChanged("1226") }
    }

    @Test
    fun `typing a cvv forwards the raw digits to the view model`() {
        val cardFields = createCardFields()

        cardFields.cvvView().setText("123")

        verify { viewModel.onCvvChanged("123") }
    }

    // endregion

    // region Forwarding focus changes

    @Test
    fun `blurring the card number field forwards the focus change to the view model`() {
        val cardFields = createCardFields()
        val editText = cardFields.cardNumberView().editText()

        editText.onFocusChangeListener.onFocusChange(editText, false)

        verify { viewModel.onFieldFocusChanged(CardField.CARD_NUMBER, false) }
    }

    @Test
    fun `focusing the cvv field forwards the focus change to the view model`() {
        val cardFields = createCardFields()
        val editText = cardFields.cvvView().editText()

        editText.onFocusChangeListener.onFocusChange(editText, true)

        verify { viewModel.onFieldFocusChanged(CardField.CVV, true) }
    }

    // endregion

    // region Error mapping

    @Test
    fun `Invalid validation displays the mapped error on the matching field`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()

            cardNumberValidation.value = ValidationResult.Invalid(R.string.card_number_error)
            advanceUntilIdle()

            val errorLabel = cardFields.cardNumberView().errorLabel()
            assertEquals(activity.getString(R.string.card_number_error), errorLabel.text.toString())
            assertEquals(View.VISIBLE, errorLabel.visibility)
        }

    @Test
    fun `Valid validation clears a previously shown error`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()
            val errorLabel = cardFields.cardNumberView().errorLabel()

            cardNumberValidation.value = ValidationResult.Invalid(R.string.card_number_error)
            advanceUntilIdle()
            cardNumberValidation.value = ValidationResult.Valid
            advanceUntilIdle()

            assertEquals(View.GONE, errorLabel.visibility)
        }

    @Test
    fun `each field maps only its own validation flow`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()

            cvvValidation.value = ValidationResult.Invalid(R.string.cvv_error)
            advanceUntilIdle()

            assertEquals(View.GONE, cardFields.cardNumberView().errorLabel().visibility)
            assertEquals(View.GONE, cardFields.expirationView().errorLabel().visibility)
            assertEquals(View.VISIBLE, cardFields.cvvView().errorLabel().visibility)
        }

    // endregion

    // region Brand -> CVV length

    @Test
    fun `detected Amex brand sets the cvv max length to four`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()

            detectedCardBrand.value = CardBrand.AMEX
            advanceUntilIdle()

            assertEquals(4, cardFields.cvvView().editText().lengthFilterMax())
        }

    @Test
    fun `detected Visa brand sets the cvv max length to three`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()

            detectedCardBrand.value = CardBrand.VISA
            advanceUntilIdle()

            assertEquals(3, cardFields.cvvView().editText().lengthFilterMax())
        }

    // endregion

    // region Validity listener

    @Test
    fun `setOnValidationChangedListener pushes the current validity immediately`() {
        isFormValid.value = true
        val cardFields = createCardFields()
        val received = mutableListOf<Boolean>()

        cardFields.setOnValidationChangedListener { received.add(it) }

        assertEquals(listOf(true), received)
    }

    @Test
    fun `listener set before attach receives the initial value exactly once`() =
        runTest(coroutineTestRule.testDispatcher) {
            // isFormValid stays at its default of false the whole time.
            val cardFields = createCardFields()
            val received = mutableListOf<Boolean>()

            // Common merchant ordering: register the listener, THEN the view attaches.
            cardFields.setOnValidationChangedListener { received.add(it) }
            cardFields.attach()
            advanceUntilIdle()

            // Setter delivers the initial value; drop(1) suppresses the StateFlow replay on attach.
            assertEquals(listOf(false), received)
        }

    @Test
    fun `listener set before attach still receives later validity changes`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields()
            val received = mutableListOf<Boolean>()

            cardFields.setOnValidationChangedListener { received.add(it) }
            cardFields.attach()
            advanceUntilIdle()

            isFormValid.value = true
            advanceUntilIdle()

            // Initial false (setter), then true (collector) — the replay is dropped, the change is not.
            assertEquals(listOf(false, true), received)
        }

    @Test
    fun `validity changes are pushed to the listener`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()
            val received = mutableListOf<Boolean>()
            cardFields.setOnValidationChangedListener { received.add(it) }

            isFormValid.value = true
            advanceUntilIdle()

            assertTrue(received.last())
        }

    // endregion

    // region Auto-advance

    @Test
    fun `card number becoming valid while focused advances to expiration`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()
            cardFields.cardNumberView().editText().requestFocus()

            cardNumberValidation.value = ValidationResult.Valid
            advanceUntilIdle()

            assertTrue(cardFields.expirationView().hasFocus())
        }

    @Test
    fun `card number becoming valid while not focused does not advance`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()

            cardNumberValidation.value = ValidationResult.Valid
            advanceUntilIdle()

            assertFalse(cardFields.expirationView().hasFocus())
        }

    @Test
    fun `invalid card number does not advance focus`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()
            cardFields.cardNumberView().editText().requestFocus()

            cardNumberValidation.value = ValidationResult.Invalid(R.string.card_number_error)
            advanceUntilIdle()

            assertFalse(cardFields.expirationView().hasFocus())
        }

    // endregion

    // region Lifecycle

    @Test
    fun `detaching stops collecting so later updates are ignored`() =
        runTest(coroutineTestRule.testDispatcher) {
            val cardFields = createCardFields().apply { attach() }
            advanceUntilIdle()
            val errorLabel = cardFields.cardNumberView().errorLabel()

            // Replace the content view, detaching CardFields and cancelling its scope.
            activity.setContentView(View(activity))
            cardNumberValidation.value = ValidationResult.Invalid(R.string.card_number_error)
            advanceUntilIdle()

            assertEquals(View.GONE, errorLabel.visibility)
        }

    // endregion

    // region State saving across configuration change

    @Test
    fun `saving then restoring the hierarchy restores all three fields`() {
        val original = createCardFields().withSaveId()
        original.cardNumberView().setText("4111111111111111")
        original.expirationView().setText("12/26")
        original.cvvView().setText("123")

        val container = SparseArray<Parcelable>()
        original.saveHierarchyState(container)

        val restored = createCardFields().withSaveId()
        restored.restoreHierarchyState(container)

        assertEquals(
            original.cardNumberView().editText().text.toString(),
            restored.cardNumberView().editText().text.toString()
        )
        assertEquals(
            original.expirationView().editText().text.toString(),
            restored.expirationView().editText().text.toString()
        )
        assertEquals(
            original.cvvView().editText().text.toString(),
            restored.cvvView().editText().text.toString()
        )
    }

    @Test
    fun `restoring forwards the saved values back to the view model`() {
        val original = createCardFields().withSaveId()
        original.cardNumberView().setText("4111111111111111")
        original.expirationView().setText("12/26")
        original.cvvView().setText("123")
        val container = SparseArray<Parcelable>()
        original.saveHierarchyState(container)

        val restored = createCardFields().withSaveId()
        restored.restoreHierarchyState(container)

        verify { viewModel.onCardNumberChanged("4111111111111111") }
        verify { viewModel.onExpiryChanged("1226") }
        verify { viewModel.onCvvChanged("123") }
    }

    @Test
    fun `SavedState survives parcelling for process death`() {
        val original = createCardFields().withSaveId()
        original.cardNumberView().setText("4111111111111111")
        original.expirationView().setText("12/26")
        original.cvvView().setText("123")
        val container = SparseArray<Parcelable>()
        original.saveHierarchyState(container)
        val state = container.get(SAVE_VIEW_ID) as CardFields.SavedState

        val parcel = Parcel.obtain()
        try {
            state.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val fromParcel = CardFields.SavedState.CREATOR.createFromParcel(parcel)

            assertEquals(state.cardNumber, fromParcel.cardNumber)
            assertEquals(state.expiration, fromParcel.expiration)
            assertEquals(state.cvv, fromParcel.cvv)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun `restoring a non-SavedState parcelable does not crash`() {
        val cardFields = createCardFields().withSaveId()
        val container = SparseArray<Parcelable>()
        container.put(SAVE_VIEW_ID, View.BaseSavedState.EMPTY_STATE)

        cardFields.restoreHierarchyState(container)

        assertEquals("", cardFields.cardNumberView().editText().text.toString())
    }

    // endregion

    private fun CardFields.withSaveId() = apply { id = SAVE_VIEW_ID }

    private fun EditText.lengthFilterMax(): Int =
        filters.filterIsInstance<InputFilter.LengthFilter>().first().max

    private companion object {
        // Arbitrary stable id; view state is only saved/restored for views that have one.
        const val SAVE_VIEW_ID = 0x00BEEF01
    }
}
