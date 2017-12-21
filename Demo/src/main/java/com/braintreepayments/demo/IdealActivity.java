package com.braintreepayments.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.braintreepayments.api.BraintreeFragment;
import com.braintreepayments.api.Ideal;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.BraintreePaymentResultListener;
import com.braintreepayments.api.interfaces.BraintreeResponseListener;
import com.braintreepayments.api.models.BraintreePaymentResult;
import com.braintreepayments.api.models.IdealBank;
import com.braintreepayments.api.models.IdealRequest;
import com.braintreepayments.api.models.IdealResult;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.UUID;

public class IdealActivity extends BaseActivity implements BraintreePaymentResultListener, OnItemClickListener {

    private static final int ISSUING_BANKS_REQUEST_CODE = 1001;

    private Button mIdealButton;
    private boolean mIsPolling;

    private ListView mIssuingBanksListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ideal_activity);

        mIdealButton = (Button) findViewById(R.id.ideal_button);
        mIsPolling = false;
    }

    @Override
    protected void reset() {
        mIdealButton.setEnabled(false);
    }

    @Override
    protected void onAuthorizationFetched() {
        try {
            mBraintreeFragment = BraintreeFragment.newInstance(this, mAuthorization);
            mIdealButton.setEnabled(true);
        } catch (InvalidArgumentException e) {
            onError(e);
        }
    }

    @Override
    public void onBraintreePaymentResult(BraintreePaymentResult result) {
        super.onBraintreePaymentResult(result);

        IdealResult idealResult = (IdealResult) result;
        if (!mIsPolling && !("COMPLETE".equals(idealResult.getStatus()))) {
            try {
                Ideal.pollForCompletion(mBraintreeFragment, idealResult.getId(), 1, 1000);
                mIsPolling = true;
            } catch (InvalidArgumentException e) {
                onError(e);
            }
        } else {
            Intent intent = new Intent()
                    .putExtra(MainActivity.EXTRA_PAYMENT_RESULT, result);

            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    }

    public void launchIdeal(View v) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setTitle("Fetching issuing banks");

        Ideal.fetchIssuingBanks(mBraintreeFragment, new BraintreeResponseListener<List<IdealBank>>() {
            @Override
            public void onResponse(final List<IdealBank> idealBanks) {
                dialog.dismiss();

                new AlertDialog.Builder(IdealActivity.this)
                    .setTitle("Issuing Banks")
                    .setAdapter(new BankListAdapter(IdealActivity.this, idealBanks), new OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            IdealBank bank = idealBanks.get(i);

                            IdealRequest builder = new IdealRequest()
                                    .issuerId(bank.getId())
                                    .amount("10")
                                    .orderId(UUID.randomUUID().toString().substring(0, 15))
                                    .currency("EUR");

                            Ideal.startPayment(mBraintreeFragment, builder, new BraintreeResponseListener<IdealResult>() {
                                @Override
                                public void onResponse(IdealResult idealResult) {
                                    String idealId = idealResult.getId();
                                }
                            });
                        }
                    })
                    .create()
                    .show();
            }
        });
        dialog.show();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        IdealBank bank = (IdealBank) mIssuingBanksListView.getAdapter().getItem(position);

        IdealRequest builder = new IdealRequest()
                .issuerId(bank.getId())
                .amount("10")
                .orderId(UUID.randomUUID().toString())
                .currency("EUR");

        Ideal.startPayment(mBraintreeFragment, builder, new BraintreeResponseListener<IdealResult>() {
            @Override
            public void onResponse(IdealResult idealResult) {
                String idealId = idealResult.getId();
            }
        });
    }

    private static class BankListAdapter extends BaseAdapter {

        private Activity mActivity;
        private List<IdealBank> mBanks;

        public BankListAdapter(Activity activity, List<IdealBank> banks) {
            mActivity = activity;
            mBanks = banks;
        }

        @Override
        public int getCount() {
            return mBanks.size();
        }

        @Override
        public Object getItem(int position) {
            return mBanks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mBanks.get(position).getId().hashCode();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = mActivity.getLayoutInflater();
                convertView = inflater.inflate(R.layout.ideal_bank_list_item, parent, false);
            }

            IdealBank bank = mBanks.get(position);
            ImageView bankIcon = (ImageView) convertView.findViewById(R.id.ideal_bank_list_item_icon);
            Picasso.with(mActivity).load(bank.getImageUri()).into(bankIcon);
            ((TextView) convertView.findViewById(R.id.ideal_bank_list_item_title)).setText(bank.getName());

            return convertView;
        }
    }
}
