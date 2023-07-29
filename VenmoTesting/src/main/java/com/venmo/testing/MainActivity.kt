package com.venmo.testing

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.braintreepayments.api.*
import com.venmo.testing.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), VenmoListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var braintreeClient: BraintreeClient
    private lateinit var venmoClient: VenmoClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initComponents()

        braintreeClient = BraintreeClient(this, "sandbox_zjxmgb4x_38kx88m8y5bf27c6")
        venmoClient = VenmoClient(this, braintreeClient).also {
            it.setListener(this)
        }
    }

    @SuppressLint("InflateParams")
    private fun initComponents() {
        binding.debugRadioButton.setOnCheckedChangeListener { _, b ->
            if (!isAppInstalled(!b) && b) {
                binding.venmoButton.isEnabled = false
                showToast("Venmo Debug App not installed")
            } else if (b) {
                binding.venmoButton.isEnabled = true
            }
        }

        binding.releaseRadioButton.setOnCheckedChangeListener { _, b ->
            if (!isAppInstalled(b) && b) {
                binding.venmoButton.isEnabled = false
                showToast("Venmo Release App not installed")
            } else if (b) {
                binding.venmoButton.isEnabled = true
            }
        }

        binding.productionRadioButton.setOnCheckedChangeListener { _, b ->
            if (b) {
                binding.merchantIdEditText.setText(getString(R.string.default_merchant_id))
            } else {
                binding.merchantIdEditText.setText("")
            }
        }

        binding.venmoButton.setOnClickListener {
            it.hideKeyboard()
            it.isEnabled = false

            binding.resultTextView.setText(R.string.waiting_for_result)

            braintreeClient.getConfiguration { _: Configuration?, _: Exception? ->
                val venmoRequest = VenmoRequest(getPaymentMethodUsage())
                venmoRequest.profileId =
                    binding.merchantIdEditText.text.toString().takeIf { id -> id.isNotEmpty() }
                venmoRequest.shouldVault = true //??
                venmoClient.tokenizeVenmoAccount(this, venmoRequest)
            }
        }
    }

    private fun getPaymentMethodUsage() =
        if (binding.multiUseRadioButton.isChecked) VenmoPaymentMethodUsage.MULTI_USE else VenmoPaymentMethodUsage.SINGLE_USE

    private fun isAppInstalled(isRelease: Boolean): Boolean {
        return try {
            packageManager.getPackageInfoCompat(
                if (isRelease) VENMO_RELEASE_PACKAGE_NAME else VENMO_DEBUG_PACKAGE_NAME, 0
            )
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun View.hideKeyboard() {
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager).hideSoftInputFromWindow(this.windowToken, 0)
    }

    companion object {
        const val VENMO_DEBUG_PACKAGE_NAME = "com.venmo.fifa"
        const val VENMO_RELEASE_PACKAGE_NAME = "com.venmo"
    }

    override fun onVenmoSuccess(venmoAccountNonce: VenmoAccountNonce) {
        binding.resultTextView.text = venmoAccountNonce.string
        binding.venmoButton.isEnabled = true
    }

    override fun onVenmoFailure(error: Exception) {
        binding.resultTextView.text = error.toString()
        binding.venmoButton.isEnabled = true
    }
}

internal fun PackageManager.getPackageInfoCompat(packageName: String, flags: Int = 0): PackageInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getPackageInfo(packageName, flags)
    }
