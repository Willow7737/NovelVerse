package com.novelverse.app.presentation.onboarding;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.novelverse.app.R;

/**
 * Base class for all onboarding prompt bottom sheets.
 *
 * <p>Subclasses only need to implement four methods:
 * <ul>
 *   <li>{@link #getSheetKey()} — unique key used by {@link OnboardingManager}.</li>
 *   <li>{@link #getImageRes()} — drawable resource for the tutorial illustration.</li>
 *   <li>{@link #getTitle()} — headline text.</li>
 *   <li>{@link #getBody()} — supporting text below the headline.</li>
 * </ul>
 *
 * <p>The "Don't remind me again" checkbox and "Got it" button are handled here.
 * When the user taps "Got it", {@link OnboardingManager#dismiss(String)} is called
 * automatically. If the checkbox is ticked, dismiss is called immediately on tick.
 */
public abstract class BaseOnboardingSheet extends BottomSheetDialogFragment {

    // ── Abstract contract ─────────────────────────────────────────────────

    /** Unique key for {@link OnboardingManager} tracking. */
    @NonNull
    public abstract String getSheetKey();

    /** Tutorial illustration shown at the top of the sheet. */
    @DrawableRes
    public abstract int getImageRes();

    /** Bold headline. */
    @NonNull
    public abstract String getTitle();

    /** Supporting body copy. */
    @NonNull
    public abstract String getBody();

    // ── Lifecycle ─────────────────────────────────────────────────────────

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_onboarding, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView image   = view.findViewById(R.id.onboarding_image);
        TextView  title   = view.findViewById(R.id.onboarding_title);
        TextView  body    = view.findViewById(R.id.onboarding_body);
        CheckBox  check   = view.findViewById(R.id.onboarding_dont_show_checkbox);
        TextView  cta     = view.findViewById(R.id.onboarding_cta_button);

        image.setImageResource(getImageRes());
        title.setText(getTitle());
        body.setText(getBody());

        // Ticking the checkbox immediately marks this sheet as dismissed
        // for the session so the manager won't queue it again in the same run.
        check.setOnCheckedChangeListener((btn, checked) -> {
            if (checked) OnboardingManager.get(requireContext()).dismiss(getSheetKey());
        });

        cta.setOnClickListener(v -> {
            // Always dismiss on "Got it", whether checkbox is ticked or not.
            OnboardingManager.get(requireContext()).dismiss(getSheetKey());
            dismissAllowingStateLoss();
        });
    }
}
