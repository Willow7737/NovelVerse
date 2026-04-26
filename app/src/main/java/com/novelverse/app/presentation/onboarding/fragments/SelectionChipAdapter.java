package com.novelverse.app.presentation.onboarding.fragments;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;
import com.novelverse.app.utils.HapticUtils;

import java.util.List;

/**
 * Generic selection chip adapter for onboarding (genres, attribution, etc.).
 */
public class SelectionChipAdapter extends RecyclerView.Adapter<SelectionChipAdapter.ChipViewHolder> {

    public interface OnChipClickListener {
        void onChipClick(int position, ChipItem item);
    }

    private final List<ChipItem> items;
    private final boolean singleSelect;
    private final OnChipClickListener listener;
    private int selectedPosition = -1;

    public SelectionChipAdapter(List<ChipItem> items, boolean singleSelect, OnChipClickListener listener) {
        this.items = items;
        this.singleSelect = singleSelect;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChipViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_selection_chip, parent, false);
        return new ChipViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChipViewHolder holder, int position) {
        ChipItem item = items.get(position);
        holder.textView.setText(item.name);
        holder.iconView.setImageResource(item.iconRes);
        holder.itemView.setSelected(item.selected);

        updateChipAppearance(holder, item.selected);

        holder.itemView.setOnClickListener(v -> {
            HapticUtils.tick(v);
            boolean wasSelected = item.selected;

            if (singleSelect) {
                // Deselect previous
                if (selectedPosition >= 0 && selectedPosition != position) {
                    items.get(selectedPosition).selected = false;
                    notifyItemChanged(selectedPosition);
                }
                item.selected = !wasSelected;
                selectedPosition = item.selected ? position : -1;
            } else {
                item.selected = !wasSelected;
            }

            updateChipAppearance(holder, item.selected);
            if (listener != null) {
                listener.onChipClick(position, item);
            }
        });
    }

    private void updateChipAppearance(ChipViewHolder holder, boolean selected) {
        if (selected) {
            holder.itemView.setBackgroundResource(R.drawable.bg_onboarding_chip_selected);
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
            holder.iconView.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.white));
        } else {
            holder.itemView.setBackgroundResource(R.drawable.bg_onboarding_chip_unselected);
            holder.textView.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.onboarding_text_primary));
            holder.iconView.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.onboarding_text_primary));
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public List<ChipItem> getItems() {
        return items;
    }

    public static class ChipViewHolder extends RecyclerView.ViewHolder {
        final ImageView iconView;
        final TextView textView;

        ChipViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.chip_icon);
            textView = itemView.findViewById(R.id.chip_text);
        }
    }

    public static class ChipItem {
        public final String id;
        public final String name;
        public final int iconRes;
        public boolean selected;

        public ChipItem(String id, String name, int iconRes) {
            this.id = id;
            this.name = name;
            this.iconRes = iconRes;
            this.selected = false;
        }
    }
}
