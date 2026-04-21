package com.novelverse.app.presentation.auth.register;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
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
public class RegisterFragment extends Fragment {

    private AuthViewModel viewModel;

    private TextInputLayout usernameLayout;
    private TextInputEditText usernameInput;
    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private TextInputLayout passwordLayout;
    private TextInputEditText passwordInput;
    private TextInputLayout confirmPasswordLayout;
    private TextInputEditText confirmPasswordInput;
    private CheckBox termsCheckbox;
    private MaterialButton registerButton;
    private View loginLink;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
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
        usernameLayout = view.findViewById(R.id.username_layout);
        usernameInput = view.findViewById(R.id.username_input);
        emailLayout = view.findViewById(R.id.email_layout);
        emailInput = view.findViewById(R.id.email_input);
        passwordLayout = view.findViewById(R.id.password_layout);
        passwordInput = view.findViewById(R.id.password_input);
        confirmPasswordLayout = view.findViewById(R.id.confirm_password_layout);
        confirmPasswordInput = view.findViewById(R.id.confirm_password_input);
        termsCheckbox = view.findViewById(R.id.terms_checkbox);
        registerButton = view.findViewById(R.id.register_button);
        loginLink = view.findViewById(R.id.login_link);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> attemptRegister());
        
        loginLink.setOnClickListener(v -> {
            if (getActivity() instanceof AuthActivity) {
                getActivity().onBackPressed();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getAuthResult().observe(getViewLifecycleOwner(), result -> {
            if (result == null) return;

            switch (result.getStatus()) {
                case LOADING:
                    registerButton.setEnabled(false);
                    registerButton.setText(R.string.loading);
                    break;

                case SUCCESS:
                    // A SUCCESS with a null user means email confirmation is required.
                    // The server message is surfaced as an error string by handleAuthResponse;
                    // this branch is only reached if Supabase returns a full session immediately
                    // (e.g. email confirmation disabled in the Supabase dashboard).
                    registerButton.setEnabled(true);
                    registerButton.setText(R.string.register);
                    // AuthActivity's own observer will navigate to Home if session is present.
                    break;

                case ERROR:
                    registerButton.setEnabled(true);
                    registerButton.setText(R.string.register);

                    String message = result.getMessage();
                    if (message != null && message.startsWith("Account created!")) {
                        // Email confirmation pending — show a dialog instead of a snackbar
                        showEmailConfirmationDialog(message);
                    } else {
                        showError(message);
                    }
                    break;
            }
        });
    }

    private void showEmailConfirmationDialog(String message) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Check your email")
                .setMessage(message)
                .setPositiveButton("Go to Login", (dialog, which) -> {
                    if (getActivity() instanceof AuthActivity) {
                        getActivity().onBackPressed();
                    }
                })
                .setCancelable(false)
                .show();
    }

    private void attemptRegister() {
        String username = usernameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();

        // Validate input
        boolean isValid = true;

        if (username.isEmpty()) {
            usernameLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!viewModel.isValidUsername(username)) {
            usernameLayout.setError(getString(R.string.error_invalid_username));
            isValid = false;
        } else {
            usernameLayout.setError(null);
        }

        if (email.isEmpty()) {
            emailLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!viewModel.isValidEmail(email)) {
            emailLayout.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            emailLayout.setError(null);
        }

        if (password.isEmpty()) {
            passwordLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!viewModel.isValidPassword(password)) {
            passwordLayout.setError(getString(R.string.error_invalid_password));
            isValid = false;
        } else {
            passwordLayout.setError(null);
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.setError(getString(R.string.error_field_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            confirmPasswordLayout.setError(getString(R.string.error_passwords_dont_match));
            isValid = false;
        } else {
            confirmPasswordLayout.setError(null);
        }

        if (!termsCheckbox.isChecked()) {
            BannerHelper.warning(requireActivity(), "Please accept the Terms of Service to continue.");
            isValid = false;
        }

        if (isValid) {
            viewModel.signUp(email, password, username);
        }
    }

    private void showError(String message) {
        if (message != null) {
            BannerHelper.error(requireActivity(), message);
        }
    }
}
