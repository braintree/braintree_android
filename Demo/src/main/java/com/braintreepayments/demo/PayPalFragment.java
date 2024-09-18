package com.braintreepayments.demo;

import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalCheckoutRequest;
import static com.braintreepayments.demo.PayPalRequestFactory.createPayPalVaultRequest;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.fragment.NavHostFragment;

import com.braintreepayments.api.core.ExperimentalBetaApi;
import com.braintreepayments.api.core.PaymentMethodNonce;
import com.braintreepayments.api.datacollector.DataCollector;
import com.braintreepayments.api.datacollector.DataCollectorRequest;
import com.braintreepayments.api.datacollector.DataCollectorResult;
import com.braintreepayments.api.paypal.PayPalClient;
import com.braintreepayments.api.paypal.PayPalLauncher;
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest;
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult;
import com.braintreepayments.api.paypal.PayPalPendingRequest;
import com.braintreepayments.api.paypal.PayPalPendingRequestEditFi;
import com.braintreepayments.api.paypal.PayPalRequest;
import com.braintreepayments.api.paypal.PayPalResult;
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditAuthResult;
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditAuthRequest;
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditResponse;
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultErrorHandlingEditRequest;
import com.google.android.material.textfield.TextInputEditText;

public class PayPalFragment extends BaseFragment {

    private String deviceData;
    private String amount;

    private PayPalClient payPalClient;
    private PayPalLauncher payPalLauncher;

    private DataCollector dataCollector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_paypal, container, false);
        TextInputEditText buyerEmailEditText = view.findViewById(R.id.buyer_email_edit_text);
        Button billingAgreementButton = view.findViewById(R.id.paypal_billing_agreement_button);
        Button singlePaymentButton = view.findViewById(R.id.paypal_single_payment_button);
        Switch ppSwitch = view.findViewById(R.id.paypal_edit_error_request_toggle);
        TextInputEditText editText = view.findViewById(R.id.paypal_edit_vault_id_field);
        TextInputEditText riskCorrelationIdText = view.findViewById(R.id.paypal_edit_fi_risk_correlation_id_field);

        Button editVaultButton = view.findViewById(R.id.paypal_edit_vault_button);

        singlePaymentButton.setOnClickListener(v -> {
            launchPayPal(false, buyerEmailEditText.getText().toString());
        });
        billingAgreementButton.setOnClickListener(v -> {
            launchPayPal(true, buyerEmailEditText.getText().toString());
        });

        editVaultButton.setOnClickListener(v -> {
            boolean isEditFIErrorRequestOn = ppSwitch.isChecked();

            if(isEditFIErrorRequestOn) {
                launchEditFiErrorHandlingRequest(editText.getText().toString(),riskCorrelationIdText.getText().toString() );
            } else {
                launchPayPalEditFIVault(editText.getText().toString());
            }

        });

        ppSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                    riskCorrelationIdText.setVisibility(View.VISIBLE);
                } else {
                    riskCorrelationIdText.setVisibility(View.GONE);
                }
            }
        });

        riskCorrelationIdText.setVisibility(View.GONE);

        payPalClient = new PayPalClient(
                requireContext(),
                super.getAuthStringArg(),
                Uri.parse("https://mobile-sdk-demo-site-838cead5d3ab.herokuapp.com/")
        );
        payPalLauncher = new PayPalLauncher();

        amount = RandomDollarAmount.getNext();
        return view;
    }

    @OptIn(markerClass = ExperimentalBetaApi.class)
    @Override
    public void onResume() {
        super.onResume();
        PayPalPendingRequest.Started pendingRequest = getPendingRequest();
        if (pendingRequest != null) {
            PayPalPaymentAuthResult paymentAuthResult = payPalLauncher.handleReturnToAppFromBrowser(pendingRequest, requireActivity().getIntent());

            if (paymentAuthResult != null) {
                if (paymentAuthResult instanceof PayPalPaymentAuthResult.Success) {
                    completePayPalFlow((PayPalPaymentAuthResult.Success) paymentAuthResult);
                } else {
                    handleError(new Exception("User did not complete payment flow"));
                }
            }

            clearPendingRequest();
        }

        PayPalPendingRequestEditFi.Started pendingRequestEditFi = getPendingRequestEditFi();

        if (pendingRequestEditFi != null) {
            PayPalVaultEditAuthResult editAuthResult = payPalLauncher.handleReturnToAppFromBrowser(pendingRequestEditFi, requireActivity().getIntent());

            if (editAuthResult != null) {

                if (editAuthResult instanceof PayPalVaultEditAuthResult.Success) {
                    // For example, call server lookup_fi_details
                } else if (editAuthResult instanceof PayPalVaultEditAuthResult.Failure) {
                    // Handle failure.error
                } else if (editAuthResult instanceof PayPalVaultEditAuthResult.NoResult) {
                    // Handle user canceled
                }
            } else {
                handleError(new Exception("User did not complete payment flow"));
            }

            clearPendingRequestEditFi();
        }

    }

    private void storePendingRequest(PayPalPendingRequest.Started request) {
        PendingRequestStore.getInstance().putPayPalPendingRequest(requireContext(), request);
    }

    private PayPalPendingRequest.Started getPendingRequest() {
        return PendingRequestStore.getInstance().getPayPalPendingRequest(requireContext());
    }

    private void clearPendingRequest() {
        PendingRequestStore.getInstance().clearPayPalPendingRequest(requireContext());
    }

    private void storePendingRequestForEditFi(PayPalPendingRequestEditFi.Started request) {
        PendingRequestStore.getInstance().putPayPalPendingRequestEditFi(requireContext(), request);
    }

    private PayPalPendingRequestEditFi.Started getPendingRequestEditFi() {
        return PendingRequestStore.getInstance().getPayPalPendingRequestEditFi(requireContext());
    }

    private void clearPendingRequestEditFi() {
        PendingRequestStore.getInstance().clearPayPalPendingRequestEditFi(requireContext());
    }

    private void launchPayPal(boolean isBillingAgreement, String buyerEmailAddress) {
        FragmentActivity activity = getActivity();
        activity.setProgressBarIndeterminateVisibility(true);

        dataCollector = new DataCollector(requireContext(), super.getAuthStringArg());

        if (Settings.shouldCollectDeviceData(requireActivity())) {
            dataCollector.collectDeviceData(requireActivity(), new DataCollectorRequest(true), (dataCollectorResult) -> {
                if (dataCollectorResult instanceof DataCollectorResult.Success) {
                    deviceData = ((DataCollectorResult.Success) dataCollectorResult).getDeviceData();
                }
                launchPayPal(activity, isBillingAgreement, amount, buyerEmailAddress);
            });
        } else {
            launchPayPal(activity, isBillingAgreement, amount, buyerEmailAddress);
        }
    }

    private void launchPayPal(
            FragmentActivity activity,
            boolean isBillingAgreement,
            String amount,
            String buyerEmailAddress
    ) {
        PayPalRequest payPalRequest;
        if (isBillingAgreement) {
            payPalRequest = createPayPalVaultRequest(activity, buyerEmailAddress);
        } else {
            payPalRequest = createPayPalCheckoutRequest(activity, amount, buyerEmailAddress);
        }
        payPalClient.createPaymentAuthRequest(requireContext(), payPalRequest, (paymentAuthRequest) -> {
            if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.Failure) {
                handleError(((PayPalPaymentAuthRequest.Failure) paymentAuthRequest).getError());
            } else if (paymentAuthRequest instanceof PayPalPaymentAuthRequest.ReadyToLaunch) {
                PayPalPendingRequest request = payPalLauncher.launch(requireActivity(),
                        ((PayPalPaymentAuthRequest.ReadyToLaunch) paymentAuthRequest));
                if (request instanceof PayPalPendingRequest.Started) {
                    storePendingRequest((PayPalPendingRequest.Started) request);
                } else if (request instanceof PayPalPendingRequest.Failure) {
                    handleError(((PayPalPendingRequest.Failure) request).getError());
                }
            }
        });
    }

    private void completePayPalFlow(PayPalPaymentAuthResult.Success paymentAuthResult) {
        payPalClient.tokenize(paymentAuthResult, payPalResult -> {
            if (payPalResult instanceof PayPalResult.Failure) {
                handleError(((PayPalResult.Failure) payPalResult).getError());
            } else if (payPalResult instanceof PayPalResult.Success) {
                handlePayPalResult(((PayPalResult.Success) payPalResult).getNonce());
            }
        });
    }

    private void handlePayPalResult(PaymentMethodNonce paymentMethodNonce) {
        if (paymentMethodNonce != null) {
            super.onPaymentMethodNonceCreated(paymentMethodNonce);

            PayPalFragmentDirections.ActionPayPalFragmentToDisplayNonceFragment action =
                    PayPalFragmentDirections.actionPayPalFragmentToDisplayNonceFragment(paymentMethodNonce);
            action.setTransactionAmount(amount);
            action.setDeviceData(deviceData);

            NavHostFragment.findNavController(this).navigate(action);
        }
    }

    @OptIn(markerClass = ExperimentalBetaApi.class)
    private void launchPayPalEditFIVault(String editVaultId) {
        PayPalVaultEditAuthRequest request = new PayPalVaultEditAuthRequest(
                editVaultId,
                null
        );

        payPalClient.createEditAuthRequest(requireContext(), request, (result) -> {
            if (result instanceof PayPalVaultEditResponse.Failure) {
                PayPalVaultEditResponse.Failure.Failure failure = (PayPalVaultEditResponse.Failure) result;
                String correlationId = failure.getRiskCorrelationId();
                //TODO: PayPalVaultErrorHandlingEditRequest and Analytics
            }

            if (result instanceof PayPalVaultEditResponse.ReadyToLaunch) {
                PayPalVaultEditResponse.ReadyToLaunch success = (PayPalVaultEditResponse.ReadyToLaunch) result;

                //TODO: Analytics
                PayPalPendingRequestEditFi pendingRequest = payPalLauncher.launch(requireActivity(), success);
                if (pendingRequest instanceof PayPalPendingRequestEditFi.Started) {
                    storePendingRequestForEditFi((PayPalPendingRequestEditFi.Started) pendingRequest);
                } else if (pendingRequest instanceof PayPalPendingRequestEditFi.Failure) {
                    handleError(((PayPalPendingRequestEditFi.Failure) pendingRequest).getError());
                }
            }
        });
    }

    @OptIn(markerClass = ExperimentalBetaApi.class)
    private void launchEditFiErrorHandlingRequest(String editVaultId, String riskCorrelationId) {
        PayPalVaultErrorHandlingEditRequest request = new PayPalVaultErrorHandlingEditRequest(
                editVaultId,
                riskCorrelationId
        );

        payPalClient.createEditErrorRequest(request, (result) -> {
            if (result instanceof PayPalVaultEditResponse.Failure) {
                PayPalVaultEditResponse.Failure.Failure failure = (PayPalVaultEditResponse.Failure) result;
                String correlationId = failure.getRiskCorrelationId();
                //TODO: PayPalVaultErrorHandlingEditRequest and Analytics
            }

            if (result instanceof PayPalVaultEditResponse.ReadyToLaunch) {
                PayPalVaultEditResponse.ReadyToLaunch success = (PayPalVaultEditResponse.ReadyToLaunch) result;

                //TODO: Analytics
                PayPalPendingRequestEditFi pendingRequest = payPalLauncher.launch(requireActivity(), success);
                if (pendingRequest instanceof PayPalPendingRequestEditFi.Started) {
                    storePendingRequestForEditFi((PayPalPendingRequestEditFi.Started) pendingRequest);
                } else if (pendingRequest instanceof PayPalPendingRequestEditFi.Failure) {
                    handleError(((PayPalPendingRequestEditFi.Failure) pendingRequest).getError());
                }
            }
        });
    }
}
