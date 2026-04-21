package com.novelverse.app.presentation.auth.forgotpassword;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.novelverse.app.ui.banner.BannerHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.novelverse.app.R;
import com.novelverse.app.domain.utils.Resource;
import com.novelverse.app.presentation.auth.AuthActivity;
import com.novelverse.app.presentation.auth.AuthViewModel;

@AndroidEntryPoint
public class ForgotPasswordFragment extends Fragment {

    private AuthViewModel viewModel;

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private MaterialButton resetButton;
    private View backLink;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_forgot_password, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(AuthViewModel.class);

        initViews(view);
        setupListeners();
        observeViewModel();
    }

    private void initViews(View view) {
        emailLayout = view.findViewById(R.id.email_layout);
        emailInput = view.findViewById(R.id.email_input);
        resetButton = view.findViewById(R.id.reset_button);
        backLink = view.findViewById(R.id.back_link);
    }

    private void setupListeners() {
        resetButton.setOnClickListener(v -> attemptReset());
        
        backLink.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                getActivity().onBackPressed();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getResetResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            switch (result.getStatus()) {
                case SUCCESS:
                    showSuccess();
                    break;
                case ERROR:
                    showError(result.getMessage());
                    break;
            }
        });
    }

    private void attemptReset() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_field_required));
            return;
        }

        if (!viewModel.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            return;
        }

        emailLayout.setError(null);
        viewModel.resetPassword(email);
    }

    private void showSuccess() {
        BannerHelper.success(requireActivity(), "Reset link sent!",
                "Check your inbox and follow the instructions.");
        // Navigate back to login after a short delay
        requireView().postDelayed(() -> {
            if (getActivity() instanceof AuthActivity) {
                getActivity().onBackPressed();
            }
        }, 2000);
    }

    private void showError(String message) {
        if (message != null) {
            BannerHelper.error(requireActivity(), message);
        }
    }
}
