package com.novelverse.app.presentation.onboarding.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.novelverse.app.R;

/**
 * Reusable fragment for a single onboarding carousel slide.
 */
public class OnboardingSlideFragment extends Fragment {

    private static final String ARG_IMAGE_RES = "image_res";
    private static final String ARG_HEADLINE = "headline";
    private static final String ARG_SUBTITLE = "subtitle";
    private static final String ARG_HIGHLIGHT_WORD = "highlight_word";
    private static final String ARG_HIGHLIGHT_COLOR = "highlight_color";
    private static final String ARG_SECOND_HIGHLIGHT = "second_highlight";
    private static final String ARG_SECOND_COLOR = "second_color";

    public static OnboardingSlideFragment newInstance(int imageRes, String headline, String subtitle,
                                                      String highlightWord, int highlightColor,
                                                      String secondHighlight, int secondColor) {
        OnboardingSlideFragment fragment = new OnboardingSlideFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE_RES, imageRes);
        args.putString(ARG_HEADLINE, headline);
        args.putString(ARG_SUBTITLE, subtitle);
        args.putString(ARG_HIGHLIGHT_WORD, highlightWord);
        args.putInt(ARG_HIGHLIGHT_COLOR, highlightColor);
        args.putString(ARG_SECOND_HIGHLIGHT, secondHighlight);
        args.putInt(ARG_SECOND_COLOR, secondColor);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_slide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args == null) return;

        ImageView imageView = view.findViewById(R.id.slide_image);
        TextView headlineView = view.findViewById(R.id.slide_headline);
        TextView subtitleView = view.findViewById(R.id.slide_subtitle);

        imageView.setImageResource(args.getInt(ARG_IMAGE_RES, R.drawable.ic_logo));

        String headline = args.getString(ARG_HEADLINE, "");
        String highlightWord = args.getString(ARG_HIGHLIGHT_WORD, null);
        int highlightColor = args.getInt(ARG_HIGHLIGHT_COLOR, Color.BLACK);
        String secondHighlight = args.getString(ARG_SECOND_HIGHLIGHT, null);
        int secondColor = args.getInt(ARG_SECOND_COLOR, Color.BLACK);

        if (highlightWord != null && !highlightWord.isEmpty()) {
            SpannableString spannable = new SpannableString(headline);
            applyHighlight(spannable, headline, highlightWord, highlightColor);
            if (secondHighlight != null && !secondHighlight.isEmpty()) {
                applyHighlight(spannable, headline, secondHighlight, secondColor);
            }
            headlineView.setText(spannable);
        } else {
            headlineView.setText(headline);
        }

        subtitleView.setText(args.getString(ARG_SUBTITLE, ""));
    }

    private void applyHighlight(SpannableString spannable, String fullText, String word, int color) {
        int start = fullText.toLowerCase().indexOf(word.toLowerCase());
        while (start >= 0) {
            int end = start + word.length();
            spannable.setSpan(new ForegroundColorSpan(color), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            start = fullText.toLowerCase().indexOf(word.toLowerCase(), end);
        }
    }
}
