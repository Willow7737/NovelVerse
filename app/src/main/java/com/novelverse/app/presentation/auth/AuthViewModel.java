package com.novelverse.app.presentation.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.novelverse.app.data.repository.UserRepository;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.domain.utils.Resource;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * ViewModel for authentication operations
 */
@HiltViewModel
public class AuthViewModel extends ViewModel {

    private final UserRepository userRepository;

    private final MutableLiveData<Resource<User>> authResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> resetResult = new MutableLiveData<>();
    private final MutableLiveData<Resource<Boolean>> verificationResult = new MutableLiveData<>();

    @Inject
    public AuthViewModel(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LiveData<Resource<User>> getAuthResult() {
        return authResult;
    }

    public LiveData<Resource<Boolean>> getResetResult() {
        return resetResult;
    }

    public LiveData<Resource<Boolean>> getVerificationResult() {
        return verificationResult;
    }

    public LiveData<User> getCurrentUser() {
        return userRepository.getCurrentUser();
    }

    public LiveData<Boolean> isLoggedIn() {
        return userRepository.isLoggedIn();
    }

    /**
     * Sign up with email and password
     */
    public void signUp(String email, String password, String username) {
        authResult.setValue(Resource.loading(null));

        userRepository.signUp(email, password, username, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                authResult.postValue(Resource.success(user));
            }

            @Override
            public void onError(String error) {
                authResult.postValue(Resource.error(error, null));
            }
        });
    }

    /**
     * Sign in with email and password
     */
    public void signIn(String email, String password) {
        authResult.setValue(Resource.loading(null));

        userRepository.signIn(email, password, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                authResult.postValue(Resource.success(user));
            }

            @Override
            public void onError(String error) {
                authResult.postValue(Resource.error(error, null));
            }
        });
    }

    /**
     * Sign in with Google.
     *
     * FIX: now accepts the idToken obtained from GoogleSignInAccount so it can
     * be forwarded all the way down to Supabase. The old no-arg version never
     * passed the token anywhere, causing every Google sign-in to fail silently.
     */
    public void signInWithGoogle(String idToken) {
        authResult.setValue(Resource.loading(null));

        userRepository.signInWithGoogle(idToken, new UserRepository.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                authResult.postValue(Resource.success(user));
            }

            @Override
            public void onError(String error) {
                authResult.postValue(Resource.error(error, null));
            }
        });
    }

    /**
     * Continue as guest
     */
    public void continueAsGuest() {
        userRepository.continueAsGuest();
        authResult.setValue(Resource.success(null));
    }

    /**
     * Reset password
     */
    public void resetPassword(String email) {
        resetResult.setValue(Resource.loading(null));

        userRepository.resetPassword(email, (success, error) -> {
            if (success) {
                resetResult.postValue(Resource.success(true));
            } else {
                resetResult.postValue(Resource.error(error, false));
            }
        });
    }

    /**
     * Resend verification email
     */
    public void resendVerification(String email) {
        verificationResult.setValue(Resource.loading(null));

        userRepository.resendVerification(email, (success, error) -> {
            if (success) {
                verificationResult.postValue(Resource.success(true));
            } else {
                verificationResult.postValue(Resource.error(error, false));
            }
        });
    }

    /**
     * Validate email format
     */
    public boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validate password strength
     */
    public boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    /**
     * Validate username
     */
    public boolean isValidUsername(String username) {
        return username != null && username.length() >= 3 && username.length() <= 20;
    }

    /**
     * Update user profile (role, bio, display name, etc.)
     */
    public void updateProfile(User user, UserRepository.SimpleCallback callback) {
        userRepository.updateProfile(user, callback);
    }

    /**
     * Update current user's reading preferences (font size, line spacing, theme).
     * Persists to UserPreferences (survives restart) and Room.
     */
    public void updateReadingPreferences(int fontSize, float lineSpacing, String theme) {
        userRepository.updateReadingPreferences(fontSize, lineSpacing, theme);
    }

    /**
     * Sign out current user
     */
    public void signOut(UserRepository.SimpleCallback callback) {
        userRepository.signOut(callback);
    }

    /**
     * Change the current user's password via Supabase Auth.
     */
    public void changePassword(String newPassword, UserRepository.SimpleCallback callback) {
        userRepository.changePassword(newPassword, callback);
    }

    /**
     * Upload avatar bytes to Supabase Storage and return the public URL via callback.
     */
    public void uploadAvatar(String userId, byte[] bytes,
                             com.novelverse.app.domain.utils.BiCallback<String> cb) {
        userRepository.uploadAvatar(userId, bytes, cb);
    }

    /**
     * Generic file upload to any Supabase Storage bucket.
     * Used for cover photos (bucket = "cover-photos", path = "{uid}/cover.jpg").
     */
    public void uploadFile(String bucket, String path, byte[] bytes, String mimeType,
                           com.novelverse.app.domain.utils.BiCallback<String> cb) {
        userRepository.uploadFile(bucket, path, bytes, mimeType, cb);
    }

    /**
     * Add points to the current user's balance.
     */
    public void addPoints(int amount, String reason, UserRepository.SimpleCallback callback) {
        userRepository.addPoints(amount, reason, callback);
    }

    /**
     * Refresh the current user from Supabase (e.g. after a points credit).
     */
    public void refreshCurrentUser(UserRepository.SimpleCallback callback) {
        userRepository.refreshCurrentUser(callback != null ? callback : (s, e) -> {});
    }
}
