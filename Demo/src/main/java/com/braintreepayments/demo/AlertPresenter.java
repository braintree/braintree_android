package com.braintreepayments.demo;

import android.app.AlertDialog;
import android.content.Context;

import androidx.fragment.app.Fragment;

public class AlertPresenter {

    public void showErrorDialog(Fragment fragment, Exception error) {
        showErrorDialog(fragment.requireContext(), error);
    }

    public void showErrorDialog(Context context, Exception error) {
        String message = "An error occurred (" + error.getClass() + "): " + error.getMessage();
        showDialog(context, message);
    }

    public void showDialog(Fragment fragment, String message) {
        showDialog(fragment.requireContext(), message);
    }

    public void showDialog(Context context, String message) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
