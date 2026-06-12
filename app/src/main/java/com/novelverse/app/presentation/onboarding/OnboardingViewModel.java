package com.novelverse.app.presentation.onboarding;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.novelverse.app.data.local.preferences.UserPreferences;
import com.novelverse.app.data.repository.UserRepository;
import com.novelverse.app.domain.models.User;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for the onboarding flow. Manages state across all onboarding screens
 * including carousel position, selected genres, attribution source, and profile setup.
 */
@HiltViewModel
public class OnboardingViewModel extends AndroidViewModel {

    public static final int STEP_CAROUSEL = 0;
    public static final int STEP_GET_STARTED = 1;
    public static final int STEP_EMAIL_SIGN_UP = 2;
    public static final int STEP_LOGIN = 3;
    public static final int STEP_INTERESTS = 4;
    public static final int STEP_ATTRIBUTION = 5;
    public static final int STEP_PROFILE_SETUP = 6;
    public static final int STEP_COMPLETE = 7;
    public static final int STEP_FORGOT_PASSWORD = 8;
    public static final int STEP_NEW_PASSWORD = 9;

    private final UserPreferences userPreferences;
    private final UserRepository userRepository;
    private final Gson gson = new Gson();

    private final MutableLiveData<Integer> currentStep = new MutableLiveData<>(STEP_CAROUSEL);
    private final MutableLiveData<List<String>> selectedGenres = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> attributionSource = new MutableLiveData<>(null);
    private final MutableLiveData<String> displayName = new MutableLiveData<>("");
    private final MutableLiveData<String> rolePreference = new MutableLiveData<>("reader");
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>(null);
    private final MutableLiveData<Boolean> onboardingComplete = new MutableLiveData<>(false);

    @Inject
    public OnboardingViewModel(@NonNull Application application,
                               UserPreferences userPreferences,
                               UserRepository userRepository) {
        super(application);
        this.userPreferences = userPreferences;
        this.userRepository = userRepository;
        restoreState();
    }

    // ── State restoration ───────────────────────────────────────────────────

    private void restoreState() {
        int savedStep = userPreferences.getOnboardingStep();
        if (savedStep > STEP_CAROUSEL && savedStep < STEP_COMPLETE) {
            currentStep.setValue(savedStep);
        }

        String interestsJson = userPreferences.getOnboardingInterestsJson();
        if (interestsJson != null) {
            try {
                List<String> genres = gson.fromJson(interestsJson, new TypeToken<List<String>>(){}.getType());
                if (genres != null) {
                    selectedGenres.setValue(genres);
                }
            } catch (Exception ignored) {
            }
        }

        String attribution = userPreferences.getAttributionSource();
        if (attribution != null) {
            attributionSource.setValue(attribution);
        }
    }

    private void persistState() {
        Integer step = currentStep.getValue();
        if (step != null) {
            userPreferences.setOnboardingStep(step);
        }

        List<String> genres = selectedGenres.getValue();
        if (genres != null) {
            userPreferences.setOnboardingInterestsJson(gson.toJson(genres));
        }

        String attribution = attributionSource.getValue();
        if (attribution != null) {
            userPreferences.setAttributionSource(attribution);
        }
    }

    // ── LiveData getters ────────────────────────────────────────────────────

    public LiveData<Integer> getCurrentStep() {
        return currentStep;
    }

    public LiveData<List<String>> getSelectedGenres() {
        return selectedGenres;
    }

    public LiveData<String> getAttributionSource() {
        return attributionSource;
    }

    public LiveData<String> getDisplayName() {
        return displayName;
    }

    public LiveData<String> getRolePreference() {
        return rolePreference;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getOnboardingComplete() {
        return onboardingComplete;
    }

    // ── Step navigation ─────────────────────────────────────────────────────

    public void setStep(int step) {
        currentStep.setValue(step);
        persistState();
    }

    public void nextStep() {
        Integer step = currentStep.getValue();
        if (step == null) step = STEP_CAROUSEL;
        if (step < STEP_COMPLETE) {
            currentStep.setValue(step + 1);
            persistState();
        }
    }

    public void previousStep() {
        Integer step = currentStep.getValue();
        if (step == null) step = STEP_CAROUSEL;
        if (step > STEP_CAROUSEL) {
            currentStep.setValue(step - 1);
            persistState();
        }
    }

    // ── Genre selection ─────────────────────────────────────────────────────

    public void toggleGenre(String genreSlug) {
        List<String> current = selectedGenres.getValue();
        if (current == null) current = new ArrayList<>();
        List<String> updated = new ArrayList<>(current);
        if (updated.contains(genreSlug)) {
            updated.remove(genreSlug);
        } else {
            updated.add(genreSlug);
        }
        selectedGenres.setValue(updated);
        persistState();
    }

    public boolean isGenreSelected(String genreSlug) {
        List<String> current = selectedGenres.getValue();
        return current != null && current.contains(genreSlug);
    }

    public int getSelectedGenreCount() {
        List<String> current = selectedGenres.getValue();
        return current != null ? current.size() : 0;
    }

    public void clearSelectedGenres() {
        selectedGenres.setValue(new ArrayList<>());
        persistState();
    }

    // ── Attribution ─────────────────────────────────────────────────────────

    public void setAttribution(String source) {
        attributionSource.setValue(source);
        persistState();
    }

    // ── Profile setup ─────────────────────────────────────────────────────────

    public void setDisplayName(String name) {
        displayName.setValue(name);
    }

    public void setRolePreference(String role) {
        rolePreference.setValue(role);
    }

    // ── Auth delegation ───────────────────────────────────────────────────────

    public void signInWithEmail(String email, String password, UserRepository.AuthCallback callback) {
        isLoading.setValue(true);
        userRepository.signIn(email, password, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
                callback.onError(error);
            }
        });
    }

    public void signUpWithEmail(String email, String password, String username, UserRepository.AuthCallback callback) {
        isLoading.setValue(true);
        userRepository.signUp(email, password, username, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
                callback.onError(error);
            }
        });
    }

    public void signInWithGoogle(String idToken, String rawNonce, UserRepository.AuthCallback callback) {
        isLoading.setValue(true);
        userRepository.signInWithGoogle(idToken, rawNonce, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
                callback.onError(error);
            }
        });
    }

    /**
     * Exchanges a Supabase PKCE authorization code for a session.
     * Called from OnboardingActivity after receiving the OAuth callback deep-link.
     */
    public void exchangeOAuthCode(String authCode, String codeVerifier, UserRepository.AuthCallback callback) {
        isLoading.setValue(true);
        userRepository.exchangeOAuthCode(authCode, codeVerifier, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                isLoading.postValue(false);
                callback.onSuccess(user);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
                callback.onError(error);
            }
        });
    }

    public void continueAsGuest() {
        userRepository.continueAsGuest();
    }

    // ── Completion ────────────────────────────────────────────────────────────

    public void completeOnboarding() {
        isLoading.setValue(true);

        // Save genres to UserPreferences
        List<String> genres = selectedGenres.getValue();
        if (genres != null && !genres.isEmpty()) {
            userPreferences.setPreferredGenres(genres);
        }

        // Update profile with display name, role, and metadata
        User currentUser = userRepository.getCurrentUser().getValue();
        if (currentUser != null && !userRepository.isGuest()) {
            String name = displayName.getValue();
            if (name != null && !name.trim().isEmpty()) {
                currentUser.setDisplayName(name.trim());
            }

            String role = rolePreference.getValue();
            if (role != null) {
                if ("author".equals(role) || "both".equals(role)) {
                    currentUser.setRole("author");
                } else {
                    currentUser.setRole("reader");
                }
            }

            userRepository.updateProfile(currentUser, (success, error) -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    finalizeOnboarding();
                });
            });
        } else {
            finalizeOnboarding();
        }
    }

    private void finalizeOnboarding() {
        userPreferences.setOnboardingCompleted(true);
        userPreferences.setHasCompletedInterests(true);
        userPreferences.setOnboardingStep(STEP_COMPLETE);
        isLoading.setValue(false);
        onboardingComplete.setValue(true);
    }

    public void skipProfileSetup() {
        userPreferences.setProfileSetupSkipped(true);
        completeOnboarding();
    }

    public void clearError() {
        errorMessage.setValue(null);
    }

    // ── Validation helpers ──────────────────────────────────────────────────

    public boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isValidPassword(String password) {
        if (password == null || password.length() < 8) return false;
        boolean hasUpper = false, hasLower = false, hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            if (Character.isLowerCase(c)) hasLower = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasUpper && hasLower && hasDigit;
    }

    public boolean isValidUsername(String username) {
        return username != null && username.length() >= 3 && username.length() <= 20;
    }

    public boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
    // ── Password reset ───────────────────────────────────────────────────────

    public void resetPassword(String email,
                              com.novelverse.app.data.repository.UserRepository.SimpleCallback cb) {
        isLoading.postValue(true);
        userRepository.resetPassword(email, (success, error) -> {
            isLoading.postValue(false);
            cb.onResult(success, error);
        });
    }

    public void updatePasswordWithToken(String accessToken, String newPassword,
                                        com.novelverse.app.data.repository.UserRepository.SimpleCallback cb) {
        isLoading.postValue(true);
        userRepository.updatePasswordWithToken(accessToken, newPassword, (success, error) -> {
            isLoading.postValue(false);
            cb.onResult(success, error);
        });
    }

}

