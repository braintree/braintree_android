package com.braintreepayments.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FeaturesAdapter extends RecyclerView.Adapter<FeatureViewHolder> {

    // Ref: https://youtu.be/KhLVD6iiZQs
    interface ItemClickListener {
        void onFeatureSelected(DemoFeature feature);
    }

    private final List<DemoFeature> features;
    private final ItemClickListener itemClickListener;

    public FeaturesAdapter(List<DemoFeature> features, ItemClickListener itemClickListener) {
        this.features = features;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public FeatureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.item_feature, parent, false);
        return new FeatureViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureViewHolder holder, int position) {
        holder.bind(features.get(position));
        holder.itemView.setOnClickListener(view -> {
            int pos = holder.getAdapterPosition();
            itemClickListener.onFeatureSelected(features.get(pos));
        });
    }

    @Override
    public int getItemCount() {
        return features.size();
    }
}
