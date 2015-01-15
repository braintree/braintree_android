package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class OptionsActivity extends Activity implements OnCheckedChangeListener {

    public static final int CUSTOM = 0;
    public static final int DROP_IN = 1;

    public static final String ENVIRONMENT = "environment";
    private static final String FORM_TYPE = "form_type";
    private static final String CUSTOMER = "customer";

    private static final String SANDBOX_BASE_SERVER_URL = "https://braintree-sample-merchant.herokuapp.com";
    private static final String PRODUCTION_BASE_SERVER_URL = "https://executive-sample-merchant.herokuapp.com";

    private EditText mCustomerId;
    private SharedPreferences mPrefs;

    @SuppressWarnings("ConstantConditions")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        RadioGroup form = (RadioGroup) findViewById(R.id.form);
        form.check(getFormId());
        form.setOnCheckedChangeListener(this);

        mCustomerId = (EditText) findViewById(R.id.customerId);
        mCustomerId.setText(mPrefs.getString(CUSTOMER, ""));
    }

    protected void onDestroy() {
        mPrefs.edit().putString(CUSTOMER, mCustomerId.getText().toString()).apply();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.getId() == R.id.form) {
            if (checkedId == R.id.custom) {
                mPrefs.edit().putInt(FORM_TYPE, CUSTOM).apply();
            } else if (checkedId == R.id.dropin) {
                mPrefs.edit().putInt(FORM_TYPE, DROP_IN).apply();
            }
        }
    }

    private int getFormId() {
        if (getFormType(this) == CUSTOM) {
            return R.id.custom;
        } else {
            return R.id.dropin;
        }
    }

    public static String getEnvironmentUrl(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getInt(ENVIRONMENT, 0) == 0) {
            return SANDBOX_BASE_SERVER_URL;
        } else {
            return PRODUCTION_BASE_SERVER_URL;
        }
    }

    public static String getClientTokenUrl(Context context) {
        String path = "/client_token";
        String customer = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(CUSTOMER, "");
        if (!TextUtils.isEmpty(customer)) {
            path += "?customer_id=" + customer;
        }

        return getEnvironmentUrl(context) + path;
    }

    public static int getFormType(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(FORM_TYPE, DROP_IN);
    }
}
