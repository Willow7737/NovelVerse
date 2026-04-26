package com.novelverse.app.presentation.onboarding.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.github.dhaval2404.imagepicker.ImagePicker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.novelverse.app.R;
import com.novelverse.app.presentation.onboarding.OnboardingActivity;
import com.novelverse.app.presentation.onboarding.OnboardingAnalytics;
import com.novelverse.app.presentation.onboarding.OnboardingViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.utils.HapticUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Profile setup screen — avatar, display name, and role selection.
 */
@AndroidEntryPoint
public class ProfileSetupFragment extends Fragment {

    private OnboardingViewModel viewModel;
    @Inject
    OnboardingAnalytics analytics;

    private ImageView avatarImage;
    private TextInputEditText nameInput;
    private TextView pillReader;
    private TextView pillWriter;
    private TextView pillBoth;
    private MaterialButton completeButton;
    private View skipButton;

    private byte[] avatarBytes;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(OnboardingViewModel.class);

        avatarImage = view.findViewById(R.id.avatar_image);
        nameInput = view.findViewById(R.id.name_input);
        pillReader = view.findViewById(R.id.pill_reader);
        pillWriter = view.findViewById(R.id.pill_writer);
        pillBoth = view.findViewById(R.id.pill_both);
        completeButton = view.findViewById(R.id.btn_complete);
        skipButton = view.findViewById(R.id.btn_skip);

        avatarImage.setOnClickListener(v -> pickImage());

        pillReader.setOnClickListener(v -> selectRole("reader"));
        pillWriter.setOnClickListener(v -> selectRole("writer"));
        pillBoth.setOnClickListener(v -> selectRole("both"));

        completeButton.setOnClickListener(v -> {
            HapticUtils.medium(v);
            completeSetup();
        });

        skipButton.setOnClickListener(v -> {
            HapticUtils.light(v);
            if (analytics != null) analytics.logProfileSetupSkip();
            viewModel.skipProfileSetup();
        });

        selectRole("reader"); // default
    }

    private void pickImage() {
        HapticUtils.light(avatarImage);
        ImagePicker.with(this)
                .crop(1f, 1f)
                .compress(512)
                .maxResultSize(512, 512)
                .start();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                avatarImage.setImageURI(uri);
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                    avatarBytes = baos.toByteArray();
                } catch (IOException e) {
                    BannerHelper.error(requireActivity(), "Failed to load image.");
                }
            }
        }
    }

    private void selectRole(String role) {
        HapticUtils.tick(pillReader);
        viewModel.setRolePreference(role);

        pillReader.setBackgroundResource("reader".equals(role) ? R.drawable.bg_role_pill_selected : R.drawable.bg_role_pill_unselected);
        pillReader.setTextColor(ContextCompat.getColor(requireContext(), "reader".equals(role) ? R.color.white : R.color.onboarding_text_primary));

        pillWriter.setBackgroundResource("writer".equals(role) ? R.drawable.bg_role_pill_selected : R.drawable.bg_role_pill_unselected);
        pillWriter.setTextColor(ContextCompat.getColor(requireContext(), "writer".equals(role) ? R.color.white : R.color.onboarding_text_primary));

        pillBoth.setBackgroundResource("both".equals(role) ? R.drawable.bg_role_pill_selected : R.drawable.bg_role_pill_unselected);
        pillBoth.setTextColor(ContextCompat.getColor(requireContext(), "both".equals(role) ? R.color.white : R.color.onboarding_text_primary));
    }

    private void completeSetup() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        if (!name.isEmpty()) {
            viewModel.setDisplayName(name);
        }

        boolean hasPhoto = avatarBytes != null && avatarBytes.length > 0;
        String role = viewModel.getRolePreference().getValue();
        if (analytics != null) {
            analytics.logProfileSetupComplete(hasPhoto, !name.isEmpty(), role != null ? role : "reader");
        }

        viewModel.completeOnboarding();
    }
}
