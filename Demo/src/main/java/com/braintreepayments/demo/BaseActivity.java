package com.braintreepayments.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreePaymentResultListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.internal.SignatureVerificationOverrides;
import com.braintreepayments.api.models.BinData;
import com.braintreepayments.api.models.BraintreePaymentResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.demo.models.ClientToken;
import com.paypal.android.sdk.onetouch.core.PayPalOneTouchCore;

import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

@SuppressWarnings("deprecation")
public abstract class BaseActivity extends AppCompatActivity implements OnRequestPermissionsResultCallback,
        PaymentMethodNonceCreatedListener, BraintreeCancelListener, BraintreeErrorListener,
        BraintreePaymentResultListener, ActionBar.OnNavigationListener {

    private static final String EXTRA_AUTHORIZATION = "com.braintreepayments.demo.EXTRA_AUTHORIZATION";
    private static final String EXTRA_CUSTOMER_ID = "com.braintreepayments.demo.EXTRA_CUSTOMER_ID";

    protected String mAuthorization;
    protected String mCustomerId;
    protected BraintreeFragment mBraintreeFragment;

    private boolean mActionBarSetup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setProgressBarIndeterminateVisibility(true);

        if (savedInstanceState != null) {
            mAuthorization = savedInstanceState.getString(EXTRA_AUTHORIZATION);
            mCustomerId = savedInstanceState.getString(EXTRA_CUSTOMER_ID);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mActionBarSetup) {
            setupActionBar();
            mActionBarSetup = true;
        }

        SignatureVerificationOverrides.disableAppSwitchSignatureVerification(
                Settings.isPayPalSignatureVerificationDisabled(this));
        PayPalOneTouchCore.useHardcodedConfig(this, Settings.useHardcodedPayPalConfiguration(this));

        if (BuildConfig.DEBUG && ContextCompat.checkSelfPermission(this, WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{ WRITE_EXTERNAL_STORAGE }, 1);
        } else {
            handleAuthorizationState();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        handleAuthorizationState();
    }

    private void handleAuthorizationState() {
        if (mAuthorization == null ||
                (Settings.useTokenizationKey(this) && !mAuthorization.equals(Settings.getEnvironmentTokenizationKey(this))) ||
                !TextUtils.equals(mCustomerId, Settings.getCustomerId(this))) {
            performReset();
        } else {
            onAuthorizationFetched();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAuthorization != null) {
            outState.putString(EXTRA_AUTHORIZATION, mAuthorization);
            outState.putString(EXTRA_CUSTOMER_ID, mCustomerId);
        }
    }

    @CallSuper
    @Override
    public void onPaymentMethodNonceCreated(PaymentMethodNonce paymentMethodNonce) {
        setProgressBarIndeterminateVisibility(true);

        Log.d(getClass().getSimpleName(), "Payment Method Nonce received: " + paymentMethodNonce.getTypeLabel());
    }

    @CallSuper
    @Override
    public void onBraintreePaymentResult(BraintreePaymentResult result) {
        setProgressBarIndeterminateVisibility(true);

        Log.d(getClass().getSimpleName(), "Braintree Payment Result received: " + result.getClass().getSimpleName());
    }

    @CallSuper
    @Override
    public void onCancel(int requestCode) {
        setProgressBarIndeterminateVisibility(false);

        Log.d(getClass().getSimpleName(), "Cancel received: " + requestCode);
    }

    @CallSuper
    @Override
    public void onError(Exception error) {
        setProgressBarIndeterminateVisibility(false);

        Log.d(getClass().getSimpleName(), "Error received (" + error.getClass() + "): "  + error.getMessage());
        Log.d(getClass().getSimpleName(), error.toString());

        showDialog("An error occurred (" + error.getClass() + "): " + error.getMessage());
    }

    private void performReset() {
        setProgressBarIndeterminateVisibility(true);

        mAuthorization = null;
        mCustomerId = Settings.getCustomerId(this);

        if (mBraintreeFragment != null) {
            getFragmentManager().beginTransaction().remove(mBraintreeFragment).commit();
            mBraintreeFragment = null;
        }

        reset();
        fetchAuthorization();
    }

    protected abstract void reset();

    protected abstract void onAuthorizationFetched();

    protected void fetchAuthorization() {
        if (mAuthorization != null) {
            setProgressBarIndeterminateVisibility(false);
            onAuthorizationFetched();
        } else if (Settings.useTokenizationKey(this)) {
            mAuthorization = Settings.getEnvironmentTokenizationKey(this);
            setProgressBarIndeterminateVisibility(false);
            onAuthorizationFetched();
        } else {
            DemoApplication.getApiClient(this).getClientToken(Settings.getCustomerId(this),
                    Settings.getMerchantAccountId(this), new Callback<ClientToken>() {
                        @Override
                        public void success(ClientToken clientToken, Response response) {
                            setProgressBarIndeterminateVisibility(false);

                            if (TextUtils.isEmpty(clientToken.getClientToken())) {
                                showDialog("Client token was empty");
                            } else {
                                mAuthorization = clientToken.getClientToken();
                                onAuthorizationFetched();
                            }
                        }

                        @Override
                        public void failure(RetrofitError error) {
                            setProgressBarIndeterminateVisibility(false);

                            if (error.getResponse() == null) {
                                showDialog(error.getCause().getMessage());
                            } else {
                                showDialog("Unable to get a client token. Response Code: " +
                                        error.getResponse().getStatus() + " Response body: " +
                                        error.getResponse().getBody());
                            }
                        }
                    });
        }
    }

    protected void showDialog(String message) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    protected void setUpAsBack() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.environments,
                android.R.layout.simple_spinner_dropdown_item);
        actionBar.setListNavigationCallbacks(adapter, this);
        actionBar.setSelectedNavigationItem(Settings.getEnvironment(this));
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        if (Settings.getEnvironment(this) != itemPosition) {
            Settings.setEnvironment(this, itemPosition);
            performReset();
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.reset:
                performReset();
                return true;
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return false;
        }
    }

    public static String getDisplayString(BinData binData) {
        return "Bin Data: \n"  +
                "         - Prepaid: " + binData.getHealthcare() + "\n" +
                "         - Healthcare: " + binData.getHealthcare() + "\n" +
                "         - Debit: " + binData.getDebit() + "\n" +
                "         - Durbin Regulated: " + binData.getDurbinRegulated() + "\n" +
                "         - Commercial: " + binData.getCommercial() + "\n" +
                "         - Payroll: " + binData.getPayroll() + "\n" +
                "         - Issuing Bank: " + binData.getIssuingBank() + "\n" +
                "         - Country of Issuance: " + binData.getCountryOfIssuance() + "\n" +
                "         - Product Id: " + binData.getProductId();
    }
}
