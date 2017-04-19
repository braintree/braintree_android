package com.braintreepayments.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.braintreepayments.api.PayPal;
import com.braintreepayments.api.dropin.DropInActivity;
import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.braintreepayments.api.dropin.utils.PaymentMethodType;
import com.braintreepayments.api.models.AndroidPayCardNonce;
import com.braintreepayments.api.models.CardNonce;
import com.braintreepayments.api.models.PayPalAccountNonce;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.PostalAddress;
import com.braintreepayments.api.models.VenmoAccountNonce;
import com.braintreepayments.api.models.VisaCheckoutAddress;
import com.braintreepayments.api.models.VisaCheckoutNonce;
import com.google.android.gms.identity.intents.model.UserAddress;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.LineItem;

import java.util.Collections;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class MainActivity extends BaseActivity {

    static final String EXTRA_PAYMENT_METHOD_NONCE = "payment_method_nonce";
    static final String EXTRA_DEVICE_DATA = "device_data";
    static final String EXTRA_COLLECT_DEVICE_DATA = "collect_device_data";
    static final String EXTRA_ANDROID_PAY_CART = "android_pay_cart";

    private static final int DROP_IN_REQUEST = 1;
    private static final int ANDROID_PAY_REQUEST = 2;
    private static final int CARDS_REQUEST = 3;
    private static final int PAYPAL_REQUEST = 4;
    private static final int VISA_CHECKOUT_REQUEST = 5;

    private static final String KEY_NONCE = "nonce";

    private PaymentMethodNonce mNonce;

    private ImageView mNonceIcon;
    private TextView mNonceString;
    private TextView mNonceDetails;
    private TextView mDeviceData;

    private Button mDropInButton;
    private Button mAndroidPayButton;
    private Button mCardsButton;
    private Button mPayPalButton;
    private Button mVisaCheckoutButton;
    private Button mCreateTransactionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mNonceIcon = (ImageView) findViewById(R.id.nonce_icon);
        mNonceString = (TextView) findViewById(R.id.nonce);
        mNonceDetails = (TextView) findViewById(R.id.nonce_details);
        mDeviceData = (TextView) findViewById(R.id.device_data);

        mDropInButton = (Button) findViewById(R.id.drop_in);
        mAndroidPayButton = (Button) findViewById(R.id.android_pay);
        mCardsButton = (Button) findViewById(R.id.card);
        mPayPalButton = (Button) findViewById(R.id.paypal);
        mVisaCheckoutButton = (Button) findViewById(R.id.visa_checkout);
        mCreateTransactionButton = (Button) findViewById(R.id.create_transaction);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(KEY_NONCE)) {
                mNonce = savedInstanceState.getParcelable(KEY_NONCE);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNonce != null) {
            outState.putParcelable(KEY_NONCE, mNonce);
        }
    }

    public void launchDropIn(View v) {
        startActivityForResult(getDropInRequest().getIntent(this), DROP_IN_REQUEST);
    }

    public void launchAndroidPay(View v) {
        Intent intent = new Intent(this, AndroidPayActivity.class)
                .putExtra(EXTRA_ANDROID_PAY_CART, getAndroidPayCart());
        startActivityForResult(intent, ANDROID_PAY_REQUEST);
    }

    public void launchCards(View v) {
        Intent intent = new Intent(this, CardActivity.class)
                .putExtra(EXTRA_COLLECT_DEVICE_DATA, Settings.shouldCollectDeviceData(this));
        startActivityForResult(intent, CARDS_REQUEST);
    }

    public void launchPayPal(View v) {
        Intent intent = new Intent(this, PayPalActivity.class)
                .putExtra(EXTRA_COLLECT_DEVICE_DATA, Settings.shouldCollectDeviceData(this));
        startActivityForResult(intent, PAYPAL_REQUEST);
    }

    public void launchVisaCheckout(View v) {
        Intent intent = new Intent(this, VisaCheckoutActivity.class);
        startActivityForResult(intent, VISA_CHECKOUT_REQUEST);
    }

    private DropInRequest getDropInRequest() {
        DropInRequest dropInRequest = new DropInRequest()
                .amount("1.00")
                .clientToken(mAuthorization)
                .collectDeviceData(Settings.shouldCollectDeviceData(this))
                .requestThreeDSecureVerification(Settings.isThreeDSecureEnabled(this))
                .androidPayCart(getAndroidPayCart())
                .androidPayShippingAddressRequired(Settings.isAndroidPayShippingAddressRequired(this))
                .androidPayPhoneNumberRequired(Settings.isAndroidPayPhoneNumberRequired(this))
                .androidPayAllowedCountriesForShipping(Settings.getAndroidPayAllowedCountriesForShipping(this));

        if (Settings.isPayPalAddressScopeRequested(this)) {
            dropInRequest.paypalAdditionalScopes(Collections.singletonList(PayPal.SCOPE_ADDRESS));
        }

        return dropInRequest;
    }

    public void createTransaction(View v) {
        Intent intent = new Intent(this, CreateTransactionActivity.class)
                .putExtra(CreateTransactionActivity.EXTRA_PAYMENT_METHOD_NONCE, mNonce);
        startActivity(intent);

        mCreateTransactionButton.setEnabled(false);
        clearNonce();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == DROP_IN_REQUEST) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                displayResult(result.getPaymentMethodNonce(), result.getDeviceData());
            } else {
                displayResult((PaymentMethodNonce) data.getParcelableExtra(EXTRA_PAYMENT_METHOD_NONCE),
                        data.getStringExtra(EXTRA_DEVICE_DATA));
                mCreateTransactionButton.setEnabled(true);
            }
        } else if (resultCode != RESULT_CANCELED) {
            showDialog(((Exception) data.getSerializableExtra(DropInActivity.EXTRA_ERROR)).getMessage());
        }
    }

    @Override
    protected void reset() {
        enableButtons(false);
        mCreateTransactionButton.setEnabled(false);

        clearNonce();
    }

    @Override
    protected void onAuthorizationFetched() {
        enableButtons(true);
    }

    private void displayResult(PaymentMethodNonce paymentMethodNonce, String deviceData) {
        mNonce = paymentMethodNonce;

        mNonceIcon.setImageResource(PaymentMethodType.forType(mNonce).getDrawable());
        mNonceIcon.setVisibility(VISIBLE);

        mNonceString.setText(getString(R.string.nonce) + ": " + mNonce.getNonce());
        mNonceString.setVisibility(VISIBLE);

        String details = "";
        if (mNonce instanceof CardNonce) {
            CardNonce cardNonce = (CardNonce) mNonce;

            details = "Card Last Two: " + cardNonce.getLastTwo() + "\n";
            details += "3DS isLiabilityShifted: " + cardNonce.getThreeDSecureInfo().isLiabilityShifted() + "\n";
            details += "3DS isLiabilityShiftPossible: " + cardNonce.getThreeDSecureInfo().isLiabilityShiftPossible();
        } else if (mNonce instanceof PayPalAccountNonce) {
            PayPalAccountNonce paypalAccountNonce = (PayPalAccountNonce) mNonce;

            details = "First name: " + paypalAccountNonce.getFirstName() + "\n";
            details += "Last name: " + paypalAccountNonce.getLastName() + "\n";
            details += "Email: " + paypalAccountNonce.getEmail() + "\n";
            details += "Phone: " + paypalAccountNonce.getPhone() + "\n";
            details += "Payer id: " + paypalAccountNonce.getPayerId() + "\n";
            details += "Client metadata id: " + paypalAccountNonce.getClientMetadataId() + "\n";
            details += "Billing address: " + formatAddress(paypalAccountNonce.getBillingAddress()) + "\n";
            details += "Shipping address: " + formatAddress(paypalAccountNonce.getShippingAddress());
        } else if (mNonce instanceof AndroidPayCardNonce) {
            AndroidPayCardNonce androidPayCardNonce = (AndroidPayCardNonce) mNonce;

            details = "Underlying Card Last Two: " + androidPayCardNonce.getLastTwo() + "\n";
            details += "Email: " + androidPayCardNonce.getEmail() + "\n";
            details += "Billing address: " + formatAddress(androidPayCardNonce.getBillingAddress()) + "\n";
            details += "Shipping address: " + formatAddress(androidPayCardNonce.getShippingAddress());
        } else if (mNonce instanceof VisaCheckoutNonce) {
            VisaCheckoutNonce visaCheckoutNonce = (VisaCheckoutNonce) mNonce;
            details = "User data\n";
            details += "First name: " + visaCheckoutNonce.getUserData().getUserFirstName() + "\n";
            details += "Last name: " + visaCheckoutNonce.getUserData().getUserLastName() + "\n";
            details += "Full name: " + visaCheckoutNonce.getUserData().getUserFullName() + "\n";
            details += "User name: " + visaCheckoutNonce.getUserData().getUsername() + "\n";
            details += "Email: " + visaCheckoutNonce.getUserData().getUserEmail() + "\n";
            details += "Billing Address: " + formatAddress(visaCheckoutNonce.getBillingAddress()) + "\n";
            details += "Shipping Address: " + formatAddress(visaCheckoutNonce.getShippingAddress()) + "\n";
        } else if (mNonce instanceof VenmoAccountNonce) {
            VenmoAccountNonce venmoAccountNonce = (VenmoAccountNonce) mNonce;
            details = "Username: " + venmoAccountNonce.getUsername();
        }

        mNonceDetails.setText(details);
        mNonceDetails.setVisibility(VISIBLE);

        mDeviceData.setText("Device Data: " + deviceData);
        mDeviceData.setVisibility(VISIBLE);

        mCreateTransactionButton.setEnabled(true);
    }

    private void clearNonce() {
        mNonceIcon.setVisibility(GONE);
        mNonceString.setVisibility(GONE);
        mNonceDetails.setVisibility(GONE);
        mDeviceData.setVisibility(GONE);
        mCreateTransactionButton.setEnabled(false);
    }

    private String formatAddress(VisaCheckoutAddress address) {
        return address.getFirstName() + " " + address.getLastName() + " " + address.getStreetAddress() + " " +
                address.getExtendedAddress() + " " + address.getLocality() + " " + address.getPostalCode() + " " +
                address.getRegion() + " " + address.getCountryCode() + " " + address.getPhoneNumber();
    }

    private String formatAddress(PostalAddress address) {
        return address.getRecipientName() + " " + address.getStreetAddress() + " " +
            address.getExtendedAddress() + " " + address.getLocality() + " " + address.getRegion() +
                " " + address.getPostalCode() + " " + address.getCountryCodeAlpha2();
    }

    private String formatAddress(UserAddress address) {
        if(address == null) {
            return "null";
        }
        return address.getName() + " " + address.getAddress1() + " " + address.getAddress2() + " " +
                address.getAddress3() + " " + address.getAddress4() + " " + address.getAddress5() + " " +
                address.getLocality() + " " + address.getAdministrativeArea() + " " + address.getPostalCode() + " " +
                address.getSortingCode() + " " + address.getCountryCode();
    }

    private Cart getAndroidPayCart() {
        return Cart.newBuilder()
                .setCurrencyCode(Settings.getAndroidPayCurrency(this))
                .setTotalPrice("1.00")
                .addLineItem(LineItem.newBuilder()
                        .setCurrencyCode("USD")
                        .setDescription("Description")
                        .setQuantity("1")
                        .setUnitPrice("1.00")
                        .setTotalPrice("1.00")
                        .build())
                .build();
    }

    private void enableButtons(boolean enable) {
        mDropInButton.setEnabled(enable);
        mAndroidPayButton.setEnabled(enable);
        mCardsButton.setEnabled(enable);
        mPayPalButton.setEnabled(enable);
        mVisaCheckoutButton.setEnabled(enable);
    }
}
