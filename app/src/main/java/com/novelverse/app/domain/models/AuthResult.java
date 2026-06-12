package com.novelverse.app.domain.models;

/**
 * Result wrapper for authentication operations
 */
public class AuthResult {

    private boolean success;
    private boolean pendingVerification;
    private User user;
    private String errorMessage;
    private String accessToken;
    private String refreshToken;
    private long expiresIn;

    private AuthResult(Builder builder) {
        this.success = builder.success;
        this.pendingVerification = builder.pendingVerification;
        this.user = builder.user;
        this.errorMessage = builder.errorMessage;
        this.accessToken = builder.accessToken;
        this.refreshToken = builder.refreshToken;
        this.expiresIn = builder.expiresIn;
    }

    public boolean isSuccess() {
        return success;
    }

    /** True when the account was created but email verification is still required. */
    public boolean isPendingVerification() {
        return pendingVerification;
    }

    public User getUser() {
        return user;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public static AuthResult success(User user, String accessToken, String refreshToken, long expiresIn) {
        return new Builder()
                .setSuccess(true)
                .setUser(user)
                .setAccessToken(accessToken)
                .setRefreshToken(refreshToken)
                .setExpiresIn(expiresIn)
                .build();
    }

    /** Account created successfully; email confirmation email has been sent. */
    public static AuthResult pendingVerification(User user) {
        return new Builder()
                .setSuccess(false)
                .setPendingVerification(true)
                .setUser(user)
                .build();
    }

    public static AuthResult error(String errorMessage) {
        return new Builder()
                .setSuccess(false)
                .setErrorMessage(errorMessage)
                .build();
    }

    public static class Builder {
        private boolean success;
        private boolean pendingVerification;
        private User user;
        private String errorMessage;
        private String accessToken;
        private String refreshToken;
        private long expiresIn;

        public Builder setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        public Builder setPendingVerification(boolean pendingVerification) {
            this.pendingVerification = pendingVerification;
            return this;
        }

        public Builder setUser(User user) {
            this.user = user;
            return this;
        }

        public Builder setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder setAccessToken(String accessToken) {
            this.accessToken = accessToken;
            return this;
        }

        public Builder setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
            return this;
        }

        public Builder setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
            return this;
        }

        public AuthResult build() {
            return new AuthResult(this);
        }
    }
}