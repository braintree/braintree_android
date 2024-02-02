package com.venmo.testing

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.preference.*
import com.braintreepayments.api.*

class PreferencesFragment : PreferenceFragmentCompat(), VenmoListener {

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var venmoClient: VenmoClient

    private var variant: SwitchPreferenceCompat? = null
    private var environment: SwitchPreferenceCompat? = null
    private var merchantId: EditTextPreference? = null
    private var merchants: ListPreference? = null
    private var paymentIntent: SwitchPreferenceCompat? = null
    private var amount: ListPreference? = null
    private var shipping: SwitchPreferenceCompat? = null
    private var billing: SwitchPreferenceCompat? = null
    private var launch: Preference? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences)

        initComponents()
        setClient()
        setListeners()
    }

    private fun initComponents() {
        variant = findPreference(VARIANT)
        environment = findPreference(ENVIRONMENT)
        merchantId = findPreference(MERCHANT_ID)
        merchants = findPreference(MERCHANTS)
        paymentIntent = findPreference(PAYMENT_INTENT)
        amount = findPreference(AMOUNT)
        shipping = findPreference(SHIPPING)
        billing = findPreference(BILLING)
        launch = findPreference(LAUNCH)

        if (!isAppInstalled(variant?.isChecked == true)) {
            launch?.isEnabled = false
            if (variant?.isChecked == true) {
                showToast(getString(R.string.venmo_release_not_installed))
            } else {
                showToast(getString(R.string.venmo_debug_not_installed))
            }
        }

        environment?.let {
            merchants?.apply {
                if (it.isChecked.not()) {
                    entries = resources.getStringArray(R.array.sandbox_merchants_entries)
                    entryValues = resources.getStringArray(R.array.sandbox_merchants_values)
                } else {
                    entries = resources.getStringArray(R.array.production_merchants_entries)
                    entryValues = resources.getStringArray(R.array.production_merchants_values)
                }
            }
            it.setOnPreferenceChangeListener { _, _ ->
                merchantId?.text = ""
                startActivity(Intent.makeRestartActivityTask(activity?.intent?.component))
                return@setOnPreferenceChangeListener true
            }
        }

        merchantId?.apply {
            summary = if (this.text.isNullOrEmpty().not()) {
                "Profile selected: ${this.text}"
            } else {
                "Profile selected: default (no selection)"
            }

            setOnPreferenceChangeListener { _, newValue ->
                summary = if (newValue.toString().isNotEmpty()) {
                    "Profile selected: ${this.text}"
                } else {
                    "Profile selected: default (no selection)"
                }
                summary = "Profile selected: ${newValue ?: "default (no selection)"}"

                merchants?.apply {
                    summary = try {
                        val value = entries[findIndexOfValue(newValue as String?)]
                        "Using: $value"
                    } catch (e: Exception) {
                        if (text.isNullOrEmpty().not()) {
                            "Custom Merchant Profile Id selected"
                        } else {
                            ""
                        }
                    }
                }
                return@setOnPreferenceChangeListener true
            }

            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }

        merchants?.apply {
            summary = if (value == merchantId?.text) {
                "Using: $entry"
            } else if (merchantId?.text.isNullOrEmpty().not()) {
                "Custom Merchant Profile Id selected"
            } else {
                ""
            }

            setOnPreferenceChangeListener { _, newValue ->
                merchantId?.apply {
                    text = newValue.toString()
                    summary = "Profile selected: $newValue"
                }

                val value = entries[findIndexOfValue(newValue as String?)]
                summary = "Using: $value"
                return@setOnPreferenceChangeListener true
            }
        }

        amount?.apply {
            summary = "Selected option: $entry"
            setOnPreferenceChangeListener { _, newValue ->
                val value = entries[findIndexOfValue(newValue as String?)]
                summary = "Selected option $value"
                return@setOnPreferenceChangeListener true
            }
        }
    }

    private fun setClient() {
        context?.let {
            braintreeClient = BraintreeClient(
                it,
                if (environment?.isChecked == true) PRODUCTION_ENVIRONMENT else SANDBOX_ENVIRONMENT
            )
        }
        venmoClient =
            VenmoClient(this, braintreeClient).also {
                it.setListener(this)
            }
    }

    private fun setListeners() {
        variant?.setOnPreferenceChangeListener { _, newValue ->
            if (!isAppInstalled(newValue as Boolean)) {
                launch?.isEnabled = false
                if (newValue) {
                    showToast(getString(R.string.venmo_release_not_installed))
                } else {
                    showToast(getString(R.string.venmo_debug_not_installed))
                }
            } else {
                launch?.isEnabled = true
            }
            return@setOnPreferenceChangeListener true
        }

        environment?.setOnPreferenceChangeListener { _, _ ->
            merchantId?.text = ""
            startActivity(Intent.makeRestartActivityTask(activity?.intent?.component))
            return@setOnPreferenceChangeListener true
        }

        merchantId?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                summary = if (newValue.toString().isNotEmpty()) {
                    "Profile selected: ${this.text}"
                } else {
                    "Profile selected: default (no selection)"
                }
                summary = "Profile selected: ${newValue ?: "default (no selection)"}"

                merchants?.apply {
                    summary = try {
                        val value = entries[findIndexOfValue(newValue as String?)]
                        "Using: $value"
                    } catch (e: Exception) {
                        if (text.isNullOrEmpty().not()) {
                            "Custom Merchant Profile Id selected"
                        } else {
                            ""
                        }
                    }
                }
                return@setOnPreferenceChangeListener true
            }

            setOnBindEditTextListener {
                it.inputType = InputType.TYPE_CLASS_NUMBER
            }
        }

        merchants?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                merchantId?.apply {
                    text = newValue.toString()
                    summary = "Profile selected: $newValue"
                }

                val value = entries[findIndexOfValue(newValue as String?)]
                summary = "Using: $value"
                return@setOnPreferenceChangeListener true
            }
        }

        amount?.apply {
            setOnPreferenceChangeListener { _, newValue ->
                val value = entries[findIndexOfValue(newValue as String?)]
                summary = "Selected option $value"
                return@setOnPreferenceChangeListener true
            }
        }

        launch?.setOnPreferenceClickListener {
            launch?.isEnabled = false

            braintreeClient.getConfiguration { _: Configuration?, _: Exception? ->
                val venmoRequest = VenmoRequest(getPaymentMethodUsage())
                venmoRequest.profileId = merchantId?.text
                venmoRequest.shouldVault = paymentIntent?.isChecked == true

                venmoRequest.collectCustomerShippingAddress = shipping?.isChecked == true
                venmoRequest.collectCustomerBillingAddress = billing?.isChecked == true

                val lineItems = ArrayList<VenmoLineItem>()
                if (amount?.value == "only_amount") {
                    venmoRequest.totalAmount = "10"
                } else if (amount?.value == "amount_line_items") {
                    lineItems.add(VenmoLineItem(VenmoLineItem.KIND_DEBIT, "Item 1", 1, "2"))
                    lineItems.add(VenmoLineItem(VenmoLineItem.KIND_DEBIT, "Item 2", 2, "5"))
                    lineItems.add(VenmoLineItem(VenmoLineItem.KIND_CREDIT, "Discount", 1, "2"))
                    venmoRequest.subTotalAmount = "10"
                    venmoRequest.taxAmount = "0.5"
                    venmoRequest.shippingAmount = "0.5"
                    venmoRequest.discountAmount = "1"
                    venmoRequest.totalAmount = "10"
                }
                venmoRequest.setLineItems(lineItems)

                venmoClient.setApplicationVariant(getVariant())
                activity?.let { venmoClient.tokenizeVenmoAccount(it, venmoRequest) }
            }
            return@setOnPreferenceClickListener true
        }
    }

    private fun isAppInstalled(isRelease: Boolean) = try {
        val variant = if (isRelease) VENMO_RELEASE_PACKAGE else VENMO_DEBUG_PACKAGE
        activity?.packageManager?.getPackageInfoCompat(variant, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    private fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    private fun getPaymentMethodUsage() =
        if (paymentIntent?.isChecked == true) VenmoPaymentMethodUsage.MULTI_USE else VenmoPaymentMethodUsage.SINGLE_USE

    private fun getVariant() =
        if (variant?.isChecked == true) VENMO_RELEASE_PACKAGE else VENMO_DEBUG_PACKAGE

    override fun onVenmoSuccess(venmoAccountNonce: VenmoAccountNonce) {
        val sb = StringBuilder("User approved transaction")
        sb.append("\nusername: ${venmoAccountNonce.username}")
        venmoAccountNonce.firstName?.let { sb.append("\nfirstname: $it") }
        venmoAccountNonce.lastName?.let { sb.append("\nlastname: $it") }
        venmoAccountNonce.email?.let { sb.append("\nemail: $it") }
        venmoAccountNonce.phoneNumber?.let { sb.append("\nphone-number: $it") }
        venmoAccountNonce.shippingAddress?.streetAddress?.let { sb.append("\nshipping-address: $it") }
        venmoAccountNonce.billingAddress?.streetAddress?.let { sb.append("\nbilling-address: $it") }
        sb.append("\npayment-nonce: ${venmoAccountNonce.string}")
        venmoAccountNonce.externalId?.let { sb.append("\nexternal-id: $it") }

        showDialog(sb.toString())
        launch?.isEnabled = true
    }

    override fun onVenmoFailure(error: Exception) {
        showDialog(error.toString())
        launch?.isEnabled = true
    }

    private fun showDialog(message: String) {
        activity?.let {
            AlertDialog.Builder(it)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                }
                .create()
                .show()
        }
    }

    private companion object {
        const val VENMO_RELEASE_PACKAGE = "com.venmo"
        const val VENMO_DEBUG_PACKAGE = "com.venmo.fifa"
        const val SANDBOX_ENVIRONMENT = "sandbox_zjxmgb4x_38kx88m8y5bf27c6"
        const val PRODUCTION_ENVIRONMENT = "production_4xbgn8rb_vwfg3wgq8b3n3xss"

        const val VARIANT = "variant"
        const val ENVIRONMENT = "environment"
        const val MERCHANT_ID = "merchant_id"
        const val MERCHANTS = "merchants"
        const val PAYMENT_INTENT = "payment_intent"
        const val AMOUNT = "amount"
        const val SHIPPING = "shipping"
        const val BILLING = "billing"
        const val LAUNCH = "launch"
    }
}

internal fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }