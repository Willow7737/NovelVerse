package com.novelverse.app.presentation.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.novelverse.app.R;

import java.util.List;

/**
 * RecyclerView adapter for ViewPager2 onboarding slides.
 *
 * Uses a ViewHolder pattern with fresh data binding on every bind — this ensures
 * text from previously shown slides never bleeds through (fixes overlap issue).
 */
public class OnboardingSlideAdapter extends RecyclerView.Adapter<OnboardingSlideAdapter.SlideViewHolder> {

    private final List<OnboardingSlide> slides;

    public OnboardingSlideAdapter(@NonNull List<OnboardingSlide> slides) {
        this.slides = slides;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_onboarding_slide, parent, false);
        return new SlideViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        OnboardingSlide slide = slides.get(position);

        // Always bind fresh data — never rely on recycled view state.
        // This is the critical fix for overlapping text from different slides.
        holder.imageView.setImageResource(slide.getImageResId());
        holder.headline.setText(slide.getHeadlineResId());
        holder.subtitle.setText(slide.getSubtitleResId());
    }

    @Override
    public int getItemCount() {
        return slides.size();
    }

    static final class SlideViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final TextView headline;
        final TextView subtitle;

        SlideViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.slide_image);
            headline = itemView.findViewById(R.id.slide_headline);
            subtitle = itemView.findViewById(R.id.slide_subtitle);
        }
    }
}
