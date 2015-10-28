package com.braintreepayments.api.internal;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.braintreepayments.api.BraintreeBroadcastManager;
import com.braintreepayments.api.BraintreeBrowserSwitchActivity;
import com.braintreepayments.api.BraintreeFragment;

/**
 * A {@link BroadcastReceiver} which recieves activity result callbacks on
 * behalf of a {@link BraintreeFragment}
 */
public class BraintreeBroadcastReceiver extends BroadcastReceiver {

    private BraintreeFragment mFragment;
    private Intent mIntent;

    @Override
    public void onReceive(Context context, Intent intent) {
        int requestCode = mIntent.getIntExtra(BraintreeBrowserSwitchActivity.EXTRA_REQUEST_CODE,
                BraintreeBrowserSwitchActivity.BROWSER_SWITCH_REQUEST_CODE);
        int resultCode =
                mIntent.getIntExtra(BraintreeBrowserSwitchActivity.EXTRA_RESULT_CODE,
                        Activity.RESULT_OK);
        Intent browserIntent =
                mIntent.getParcelableExtra(BraintreeBrowserSwitchActivity.EXTRA_INTENT);

        if (mFragment != null) {
            mFragment.onActivityResult(requestCode, resultCode, browserIntent);
        }
    }

    public void register(BraintreeFragment fragment) {
        mFragment = fragment;
        if (!BraintreeBroadcastManager.getInstance(fragment.getActivity()).isRegistered(this,
                BraintreeBrowserSwitchActivity.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED)) {
            BraintreeBroadcastManager.getInstance(fragment.getActivity()).registerReceiver(this,
                    new IntentFilter(
                            BraintreeBrowserSwitchActivity.LOCAL_BROADCAST_BROWSER_SWITCH_COMPLETED));
        }
    }

    public void unregister(Context context) {
        mFragment = null;
        BraintreeBroadcastManager.getInstance(context).unregisterReceiver(this);
    }

    public void setIntent(Intent intent){
        mIntent = intent;
    }
}
