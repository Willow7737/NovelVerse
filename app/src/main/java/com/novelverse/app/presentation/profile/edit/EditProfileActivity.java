package com.novelverse.app.presentation.profile.edit;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

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

    private ImageView avatarPreview;
    private TextView  avatarInitials;

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> imagePicker =
        registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handlePickedImage(uri);
                }
            });

    private AuthViewModel authViewModel;
    private EditText editDisplayName, editUsername, editBio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Use the correct view IDs from the layout
        avatarPreview  = findViewById(R.id.edit_avatar);
        avatarInitials = findViewById(R.id.edit_avatar_initials);
        editDisplayName = findViewById(R.id.edit_display_name);
        editUsername    = findViewById(R.id.edit_username);
        editBio         = findViewById(R.id.edit_bio);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_change_avatar).setOnClickListener(v -> pickImage());
        findViewById(R.id.btn_save).setOnClickListener(v -> saveProfile());

        authViewModel.getCurrentUser().observe(this, this::populateFields);
    }

    // ── Image picking ─────────────────────────────────────────────────────────

    private void pickImage() {
        android.content.Intent i = new android.content.Intent(
                android.content.Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        imagePicker.launch(i);
    }

    private void handlePickedImage(Uri uri) {
        try {
            Bitmap raw = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            // Crop to a square first, then apply circle mask
            Bitmap squared = cropToSquare(raw);
            Bitmap circle  = toCircleBitmap(squared);
            Bitmap scaled  = Bitmap.createScaledBitmap(squared, 400, 400, true);

            // Show circle-cropped preview
            avatarPreview.setImageBitmap(circle);
            avatarPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            if (avatarInitials != null) avatarInitials.setVisibility(android.view.View.GONE);
            avatarPreview.setVisibility(android.view.View.VISIBLE);

            // Store full-quality bytes for upload (not the circle bitmap — server side is fine with square)
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, out);
            avatarPreview.setTag(out.toByteArray());

            BannerHelper.success(this, "Photo selected", "Tap Save to apply your new avatar.");
        } catch (Exception e) {
            BannerHelper.error(this, "Couldn't load photo");
        }
    }

    /** Crop a bitmap to a centered square. */
    private Bitmap cropToSquare(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int side = Math.min(w, h);
        int x = (w - side) / 2, y = (h - side) / 2;
        return Bitmap.createBitmap(src, x, y, side, side);
    }

    /**
     * Apply a circular mask to a square bitmap.
     * Result can be set directly on an ImageView for a perfect circle preview.
     */
    private Bitmap toCircleBitmap(Bitmap src) {
        int size = src.getWidth();
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader shader = new BitmapShader(src, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shader);
        float r = size / 2f;
        canvas.drawCircle(r, r, r, paint);
        return output;
    }

    // ── Populate from user ────────────────────────────────────────────────────

    private void populateFields(User user) {
        if (user == null) return;
        if (user.getDisplayName() != null) editDisplayName.setText(user.getDisplayName());
        if (user.getUsername()    != null) editUsername.setText(user.getUsername());
        if (user.getBio()         != null) editBio.setText(user.getBio());

        // Initials fallback
        String name = user.getDisplayName() != null ? user.getDisplayName()
                    : user.getUsername() != null ? user.getUsername() : "";
        if (!name.isEmpty() && avatarInitials != null) {
            String initials = String.valueOf(Character.toUpperCase(name.charAt(0)));
            if (name.contains(" ")) {
                String[] parts = name.split(" ");
                if (parts.length > 1 && !parts[1].isEmpty())
                    initials += Character.toUpperCase(parts[1].charAt(0));
            }
            avatarInitials.setText(initials);
        }

        // Load existing avatar — circle-crop via Glide
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty() && avatarPreview != null) {
            com.bumptech.glide.Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_avatar_placeholder)
                .circleCrop()
                .into(avatarPreview);
            avatarPreview.setVisibility(android.view.View.VISIBLE);
            if (avatarInitials != null) avatarInitials.setVisibility(android.view.View.GONE);
        } else {
            if (avatarPreview  != null) avatarPreview.setVisibility(android.view.View.GONE);
            if (avatarInitials != null) avatarInitials.setVisibility(android.view.View.VISIBLE);
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    private void saveProfile() {
        User user = authViewModel.getCurrentUser().getValue();
        if (user == null) return;
        LoadingSpinner.show(this);
        user.setDisplayName(editDisplayName.getText().toString().trim());
        user.setUsername(editUsername.getText().toString().trim());
        user.setBio(editBio.getText().toString().trim());

        Object tag = avatarPreview != null ? avatarPreview.getTag() : null;
        if (tag instanceof byte[]) {
            authViewModel.uploadAvatar(user.getId(), (byte[]) tag, (url, err) -> {
                if (url != null) {
                    user.setAvatarUrl(url);
                } else {
                    android.util.Log.w("EditProfile", "Avatar upload failed: " + err);
                    // Continue saving profile even if avatar upload fails
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
