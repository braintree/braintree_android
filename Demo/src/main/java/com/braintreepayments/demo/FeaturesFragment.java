package com.braintreepayments.demo;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class FeaturesFragment extends Fragment {

    public FeaturesFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_features, container, false);

        Context context = requireContext();
        LinearLayoutManager layoutManager = new LinearLayoutManager(context);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration =
            new DividerItemDecoration(context, layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);

        recyclerView.setAdapter(new FeaturesAdapter());
        return view;
    }
}