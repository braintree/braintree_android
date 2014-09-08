package com.braintreepayments.demo;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class OptionsActivity extends Activity implements OnCheckedChangeListener {

    public static final int CUSTOM = 0;
    public static final int DROP_IN = 1;

    private static final String ENVIRONMENT = "environment";
    private static final String FORM_TYPE = "form_type";
    private static final String CUSTOMER = "customer";
    private static final String SANDBOX_BASE_SERVER_URL = "https://braintree-sample-merchant.herokuapp.com";
    private static final String PRODUCTION_BASE_SERVER_URL = "https://executive-sample-merchant.herokuapp.com";

    private RadioGroup mEnvironment;
    private RadioGroup mForm;
    private EditText mCustomerId;
    private SharedPreferences mPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.options);
        mEnvironment = (RadioGroup) findViewById(R.id.environment);
        mForm = (RadioGroup) findViewById(R.id.form);
        mCustomerId = (EditText) findViewById(R.id.customerId);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mEnvironment.check(getEnvironmentId());
        mEnvironment.setOnCheckedChangeListener(this);
        mForm.check(getFormId());
        mForm.setOnCheckedChangeListener(this);
        mCustomerId.setText(mPrefs.getString(CUSTOMER, ""));
    }

    protected void onDestroy() {
        mPrefs.edit().putString(CUSTOMER, mCustomerId.getText().toString()).commit();
        super.onDestroy();
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.getId() == R.id.environment) {
            if (checkedId == R.id.production) {
                mPrefs.edit().putString(ENVIRONMENT, PRODUCTION_BASE_SERVER_URL).commit();
            } else if (checkedId == R.id.sandbox) {
                mPrefs.edit().putString(ENVIRONMENT, SANDBOX_BASE_SERVER_URL).commit();
            }
        } else if (group.getId() == R.id.form) {
            if (checkedId == R.id.custom) {
                mPrefs.edit().putInt(FORM_TYPE, CUSTOM).commit();
            } else if (checkedId == R.id.dropin) {
                mPrefs.edit().putInt(FORM_TYPE, DROP_IN).commit();
            }
        }
    }

    private int getEnvironmentId() {
        if (getEnvironmentUrl(this).equals(PRODUCTION_BASE_SERVER_URL)) {
            return R.id.production;
        } else {
            return R.id.sandbox;
        }
    }

    public static String getEnvironmentUrl(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(ENVIRONMENT, SANDBOX_BASE_SERVER_URL);
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

    private int getFormId() {
        if (getFormType(this) == CUSTOM) {
            return R.id.custom;
        } else {
            return R.id.dropin;
        }
    }

    public static int getFormType(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(FORM_TYPE, DROP_IN);
    }

}
