package com.novelverse.app.presentation.profile.edit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.dhaval2404.imagepicker.ImagePicker;
import com.novelverse.app.R;
import com.novelverse.app.domain.models.User;
import com.novelverse.app.presentation.auth.AuthViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.ui.LoadingSpinner;
import com.novelverse.app.ui.banner.BannerManager;
import com.novelverse.app.ui.banner.BannerConfig;
import com.novelverse.app.ui.banner.BannerType;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProfileActivity extends AppCompatActivity {

    private static final int REQ_AVATAR = 101;
    private static final int REQ_COVER  = 102;

    private ImageView avatarPreview;
    private TextView  avatarInitials;
    private ImageView coverPreview;

    private AuthViewModel authViewModel;
    private EditText editDisplayName, editUsername, editBio;

    // ── FIX: store pending bytes outside the views so Glide can't clobber them ─
    private byte[] pendingAvatarBytes;
    private byte[] pendingCoverBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        authViewModel   = new ViewModelProvider(this).get(AuthViewModel.class);

        avatarPreview   = findViewById(R.id.edit_avatar);
        avatarInitials  = findViewById(R.id.edit_avatar_initials);
        coverPreview    = findViewById(R.id.edit_cover_image);
        editDisplayName = findViewById(R.id.edit_display_name);
        editUsername    = findViewById(R.id.edit_username);
        editBio         = findViewById(R.id.edit_bio);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> pickAvatar());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfile());

        android.view.View btnChangeCover = findViewById(R.id.btn_change_cover);
        if (btnChangeCover != null) btnChangeCover.setOnClickListener(v -> pickCover());

        if (getIntent().getBooleanExtra("open_cover_picker", false)) pickCover();

        authViewModel.getCurrentUser().observe(this, this::populateFields);
    }

    // ── Picking ──────────────────────────────────────────────────────────────

    private void pickAvatar() {
        ImagePicker.with(this)
            .galleryOnly()
            .crop(1f, 1f)
            .compress(512)
            .maxResultSize(600, 600)
            .start(REQ_AVATAR);
    }

    private void pickCover() {
        ImagePicker.with(this)
            .galleryOnly()
            .crop(3f, 1f)
            .compress(1024)
            .maxResultSize(1200, 400)
            .start(REQ_COVER);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK || data == null) {
            if (resultCode == ImagePicker.RESULT_ERROR)
                BannerHelper.error(this, ImagePicker.getError(data));
            return;
        }
        Uri uri = data.getData();
        if (uri == null) return;

        if (requestCode == REQ_AVATAR) {
            handleAvatarUri(uri);
        } else if (requestCode == REQ_COVER) {
            handleCoverUri(uri);
        }
    }

    private void handleAvatarUri(Uri uri) {
        try {
            Bitmap bmp    = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp, 400, 400, true);

            com.bumptech.glide.Glide.with(this).load(uri).circleCrop().into(avatarPreview);
            if (avatarInitials != null) avatarInitials.setVisibility(android.view.View.GONE);
            avatarPreview.setVisibility(android.view.View.VISIBLE);

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 88, out);
            pendingAvatarBytes = out.toByteArray();          // ← FIX

        } catch (Exception e) {
            pendingAvatarBytes = null;                        // ← FIX: clear on failure
            BannerHelper.error(this, "Couldn't load photo");
        }
    }

    private void handleCoverUri(Uri uri) {
        try {
            Bitmap bmp    = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            Bitmap scaled = Bitmap.createScaledBitmap(bmp,
                Math.min(bmp.getWidth(), 1200), Math.min(bmp.getHeight(), 400), true);

            if (coverPreview != null) {
                com.bumptech.glide.Glide.with(this).load(uri).centerCrop().into(coverPreview);
                coverPreview.setVisibility(android.view.View.VISIBLE);

                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out);
                pendingCoverBytes = out.toByteArray();        // ← FIX
            }
        } catch (Exception e) {
            pendingCoverBytes = null;                         // ← FIX: clear on failure
            BannerHelper.error(this, "Couldn't load cover photo");
        }
    }

    // ── Populate ─────────────────────────────────────────────────────────────

    private void populateFields(User user) {
        if (user == null) return;

        if (user.getDisplayName() != null) editDisplayName.setText(user.getDisplayName());
        if (user.getUsername()    != null) editUsername.setText(user.getUsername());
        if (user.getBio()         != null) editBio.setText(user.getBio());

        String name = user.getDisplayName() != null ? user.getDisplayName()
                    : user.getUsername() != null     ? user.getUsername() : "";
        if (!name.isEmpty() && avatarInitials != null) {
            String initials = String.valueOf(Character.toUpperCase(name.charAt(0)));
            if (name.contains(" ")) {
                String[] parts = name.split(" ");
                if (parts.length > 1 && !parts[1].isEmpty())
                    initials += Character.toUpperCase(parts[1].charAt(0));
            }
            avatarInitials.setText(initials);
        }

        // ── FIX: don't overwrite a preview the user just picked ─────────────
        if (pendingAvatarBytes == null) {
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty() && avatarPreview != null) {
                com.bumptech.glide.Glide.with(this).load(avatarUrl)
                    .placeholder(R.drawable.ic_avatar_placeholder).circleCrop().into(avatarPreview);
                avatarPreview.setVisibility(android.view.View.VISIBLE);
                if (avatarInitials != null) avatarInitials.setVisibility(android.view.View.GONE);
            } else {
                if (avatarPreview  != null) avatarPreview.setVisibility(android.view.View.GONE);
                if (avatarInitials != null) avatarInitials.setVisibility(android.view.View.VISIBLE);
            }
        }

        if (pendingCoverBytes == null) {
            String coverUrl = user.getCoverUrl();
            if (coverPreview != null && coverUrl != null && !coverUrl.isEmpty()) {
                com.bumptech.glide.Glide.with(this).load(coverUrl)
                    .placeholder(R.drawable.bg_profile_banner).centerCrop().into(coverPreview);
                coverPreview.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    private void saveProfile() {
        User user = authViewModel.getCurrentUser().getValue();
        if (user == null) return;

        LoadingSpinner.show(this);
        user.setDisplayName(editDisplayName.getText().toString().trim());
        user.setUsername(editUsername.getText().toString().trim());
        user.setBio(editBio.getText().toString().trim());

        // ── FIX: read from instance variables, not view tags ────────────────
        final byte[] avatarBytes = pendingAvatarBytes;
        final byte[] coverBytes  = pendingCoverBytes;

        if (avatarBytes != null) {
            authViewModel.uploadAvatar(user.getId(), avatarBytes, (url, err) -> {
                if (url != null) {
                    user.setAvatarUrl(url);
                    pendingAvatarBytes = null;   // clear only after success
                } else {
                    android.util.Log.w("EditProfile", "Avatar upload failed: " + err);
                }
                uploadCoverThenSave(user, coverBytes);
            });
        } else {
            uploadCoverThenSave(user, coverBytes);
        }
    }

    private void uploadCoverThenSave(User user, byte[] coverBytes) {
        if (coverBytes != null) {
            authViewModel.uploadFile(
                "cover-photos", user.getId() + "/cover.jpg",
                coverBytes, "image/jpeg",
                (url, err) -> {
                    if (url != null) {
                        user.setCoverUrl(url);
                        pendingCoverBytes = null; // clear only after success
                    } else {
                        android.util.Log.w("EditProfile", "Cover upload failed: " + err);
                    }
                    persistProfile(user);
                });
        } else {
            persistProfile(user);
        }
    }

    private void persistProfile(User user) {
        authViewModel.updateProfile(user, (success, error) -> runOnUiThread(() -> {
            LoadingSpinner.hide(this);
            if (success) {
                BannerConfig config = new BannerConfig()
                    .setType(BannerType.SUCCESS)
                    .setTitle("Profile saved!")
                    .setMessage("Your changes are live.")
                    .setDuration(3000)
                    .setSwipeToDismiss(true);
                BannerManager.getInstance().queueForNextActivity(config);
                finish();
            } else {
                BannerHelper.error(this, "Save failed: " + error);
            }
        }));
    }
}
