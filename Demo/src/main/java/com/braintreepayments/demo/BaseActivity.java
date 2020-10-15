package com.braintreepayments.demo;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.ArrayAdapter;

import androidx.annotation.CallSuper;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.core.content.ContextCompat;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.interfaces.BraintreeCancelListener;
import com.braintreepayments.api.interfaces.BraintreeErrorListener;
import com.braintreepayments.api.interfaces.BraintreePaymentResultListener;
import com.braintreepayments.api.interfaces.PaymentMethodNonceCreatedListener;
import com.braintreepayments.api.internal.SignatureVerificationOverrides;
import com.braintreepayments.api.models.BinData;
import com.braintreepayments.api.models.BraintreePaymentResult;
import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.demo.internal.ApiClient;
import com.braintreepayments.demo.internal.ApiClientRequestInterceptor;
import com.braintreepayments.demo.models.ClientToken;
import com.braintreepayments.demo.models.PayPalUAT;

import java.util.Arrays;
import java.util.List;

import retrofit.Callback;
import retrofit.RestAdapter;
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
                (Settings.useTokenizationKey(this) && !mAuthorization.equals(Settings.getTokenizationKey(this))) ||
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
            getSupportFragmentManager().beginTransaction().remove(mBraintreeFragment).commit();
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
        }

        String authType = Settings.getAuthorizationType(this);
        if (authType.equals(getString(R.string.tokenization_key))) {
            mAuthorization = Settings.getTokenizationKey(this);
            setProgressBarIndeterminateVisibility(false);
            onAuthorizationFetched();

        } else if (authType.equals(getString(R.string.paypal_uat))) {
            // NOTE: - The PP UAT is fetched from the PPCP sample server
            //       - The only feature that currently works with a PP UAT is Card Tokenization.
            ApiClient apiClient = new RestAdapter.Builder()
                    .setEndpoint("https://ppcp-sample-merchant-sand.herokuapp.com")
                    .setRequestInterceptor(new ApiClientRequestInterceptor())
                    .build()
                    .create(ApiClient.class);

            apiClient.getPayPalUAT("US", new Callback<PayPalUAT>() {
                @Override
                public void success(PayPalUAT uat, Response response) {
                    setProgressBarIndeterminateVisibility(false);

                    if (TextUtils.isEmpty(uat.getUAT())) {
                        showDialog("PayPal UAT was empty");
                    } else {
                        mAuthorization = uat.getUAT();
                        onAuthorizationFetched();
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    setProgressBarIndeterminateVisibility(false);

                    if (error.getResponse() == null) {
                        showDialog(error.getCause().getMessage());
                    } else {
                        showDialog("Unable to get a PayPal UAT. Response Code: " +
                                error.getResponse().getStatus() + " Response body: " +
                                error.getResponse().getBody());
                    }
                }
            });

        } else if (authType.equals(getString(R.string.client_token))) {
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

        List<String> envs = Arrays.asList(getResources().getStringArray(R.array.environments));
        actionBar.setSelectedNavigationItem(envs.indexOf(Settings.getEnvironment(this)));
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        String env = getResources().getStringArray(R.array.environments)[itemPosition];
        if (!Settings.getEnvironment(this).equals(env)) {
            Settings.setEnvironment(this, env);
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
