package com.novelverse.app.presentation.author.novelmanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.novelverse.app.R;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.presentation.write.WriterViewModel;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.ui.LoadingSpinner;
import com.novelverse.app.ui.banner.BannerManager;
import com.novelverse.app.ui.banner.BannerConfig;
import com.novelverse.app.ui.banner.BannerType;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NovelEditorActivity extends AppCompatActivity {

    public static final String EXTRA_NOVEL_ID = "novel_id";

    private static final String[] GENRES_LIST = {
            "Fantasy", "Romance", "Sci-Fi", "Mystery", "Thriller",
            "Adventure", "Horror", "Comedy", "Action", "Drama",
            "Historical", "Young Adult", "Slice of Life", "Supernatural", "Wuxia"
    };

    private WriterViewModel vm;
    private String   novelId;
    private List<String> selectedGenres = new ArrayList<>();
    private String   selectedPricing = "free";
    private boolean  isSaving = false;

    private EditText  titleInput;
    private EditText  descInput;
    private ImageView coverPreview;
    private TextView  genreDisplayText;
    private TextView  priceFree, pricePaid, priceFreemium;

    private final ActivityResultLauncher<Intent> imagePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) handleCoverImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_editor);

        vm      = new ViewModelProvider(this).get(WriterViewModel.class);
        novelId = getIntent().getStringExtra(EXTRA_NOVEL_ID);
        boolean isEdit = novelId != null;

        titleInput      = findViewById(R.id.novel_title_input);
        descInput       = findViewById(R.id.novel_desc_input);
        coverPreview    = findViewById(R.id.cover_preview);
        genreDisplayText = findViewById(R.id.genre_display_text);
        priceFree       = findViewById(R.id.price_free);
        pricePaid       = findViewById(R.id.price_paid);
        priceFreemium   = findViewById(R.id.price_freemium);

        ((TextView) findViewById(R.id.editor_toolbar_title))
                .setText(isEdit ? "Edit Novel" : "New Novel");

        // Age rating spinner
        Spinner ageSpinner = findViewById(R.id.age_rating_spinner);
        if (ageSpinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    new String[]{"All Ages", "Teen (13+)", "Mature (17+)", "Adult (18+)"});
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            ageSpinner.setAdapter(adapter);
        }

        // Buttons
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        ((FrameLayout) findViewById(R.id.btn_upload_cover)).setOnClickListener(v -> pickImage());
        findViewById(R.id.genre_picker_container).setOnClickListener(v -> showGenrePicker());
        if (priceFree     != null) priceFree.setOnClickListener(v     -> selectPricing("free"));
        if (pricePaid     != null) pricePaid.setOnClickListener(v     -> selectPricing("paid"));
        if (priceFreemium != null) priceFreemium.setOnClickListener(v -> selectPricing("freemium"));
        selectPricing("free");

        findViewById(R.id.btn_save_draft).setOnClickListener(v -> saveNovel(false));
        findViewById(R.id.btn_publish).setOnClickListener(v   -> saveNovel(true));

        // NOTE: intentionally NOT using vm.isLoading() to dim both buttons —
        // each button manages its own state in saveNovel() to avoid both appearing locked.

        if (isEdit) loadNovel();
    }

    // ── Cover ──────────────────────────────────────────────────────────────────

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        imagePicker.launch(i);
    }

    private void handleCoverImage(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            coverPreview.setImageBitmap(bmp);
            coverPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            coverPreview.setTag(bitmapToBytes(bmp));
        } catch (Exception e) {
            BannerHelper.error(this, "Image error", e.getMessage());
        }
    }

    private byte[] bitmapToBytes(Bitmap bmp) {
        Bitmap scaled = Bitmap.createScaledBitmap(bmp, 800, 1100, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        scaled.compress(Bitmap.CompressFormat.JPEG, 88, out);
        return out.toByteArray();
    }

    // ── Genre picker ───────────────────────────────────────────────────────────

    private void showGenrePicker() {
        BottomSheetDialog sheet = new BottomSheetDialog(this, R.style.BottomSheetTheme);
        android.view.View root = getLayoutInflater().inflate(R.layout.bottom_sheet_genre_picker, null);
        LinearLayout container = root.findViewById(R.id.genre_chips_container);
        List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < GENRES_LIST.length; i += 2) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rLp.bottomMargin = dp(8); row.setLayoutParams(rLp);
            TextView c1 = buildGenreChip(GENRES_LIST[i], chips); row.addView(c1); chips.add(c1);
            if (i + 1 < GENRES_LIST.length) {
                TextView c2 = buildGenreChip(GENRES_LIST[i + 1], chips); row.addView(c2); chips.add(c2);
            }
            container.addView(row);
        }
        root.findViewById(R.id.btn_genre_done).setOnClickListener(v -> { sheet.dismiss(); updateGenreDisplay(); });
        sheet.setContentView(root);
        sheet.show();
    }

    private TextView buildGenreChip(String genre, List<TextView> allChips) {
        boolean active = selectedGenres.contains(genre);
        TextView chip = new TextView(this);
        chip.setText(genre); chip.setTextSize(14f); chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(6), dp(10), dp(6), dp(10));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.rightMargin = dp(6); chip.setLayoutParams(lp);
        applyChipStyle(chip, active);
        chip.setOnClickListener(v -> {
            if (selectedGenres.contains(genre)) {
                selectedGenres.remove(genre);
                applyChipStyle(chip, false);
            } else if (selectedGenres.size() < 5) {
                selectedGenres.add(genre);
                applyChipStyle(chip, true);
            } else {
                BannerHelper.warning(NovelEditorActivity.this, "Max 5 genres", "Remove one to add another.");
            }
        });
        return chip;
    }

    private void applyChipStyle(TextView chip, boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active ? Color.parseColor("#6366F1") : Color.parseColor("#F1F5F9"));
        bg.setCornerRadius(dp(8)); chip.setBackground(bg);
        chip.setTextColor(active ? Color.WHITE : resolveAttrColor(android.R.attr.textColorSecondary));
    }

    private void updateGenreDisplay() {
        if (genreDisplayText == null) return;
        if (selectedGenres.isEmpty()) {
            genreDisplayText.setText("Select genres");
            genreDisplayText.setTextColor(resolveAttrColor(android.R.attr.textColorTertiary));
        } else {
            genreDisplayText.setText(String.join(", ", selectedGenres));
            genreDisplayText.setTextColor(resolveAttrColor(android.R.attr.textColorPrimary));
        }
    }

    // ── Pricing ────────────────────────────────────────────────────────────────

    private void selectPricing(String pricing) {
        selectedPricing = pricing;
        styleBtn(priceFree,     "free".equals(pricing));
        styleBtn(pricePaid,     "paid".equals(pricing));
        styleBtn(priceFreemium, "freemium".equals(pricing));
    }

    private void styleBtn(TextView btn, boolean active) {
        if (btn == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active ? Color.parseColor("#6366F1") : Color.parseColor("#F1F5F9"));
        bg.setCornerRadius(dp(8)); btn.setBackground(bg);
        btn.setTextColor(active ? Color.WHITE : resolveAttrColor(android.R.attr.textColorSecondary));
    }

    // ── Save / Publish ─────────────────────────────────────────────────────────

    private void saveNovel(boolean publish) {
        if (isSaving) return; // prevent double-tap
        if (titleInput == null) return;
        String title = titleInput.getText().toString().trim();
        if (title.isEmpty()) {
            BannerHelper.warning(this, "Title required", "Give your novel a title."); return;
        }

        // Lock UI — only dim the tapped button so the user knows which action is in-flight
        isSaving = true;
        LoadingSpinner.show(this);
        View btnPublish   = findViewById(R.id.btn_publish);
        View btnDraft     = findViewById(R.id.btn_save_draft);
        View activeBtn    = publish ? btnPublish : btnDraft;
        View inactiveBtn  = publish ? btnDraft   : btnPublish;
        if (activeBtn   != null) { activeBtn.setAlpha(0.5f);  activeBtn.setEnabled(false); }
        if (inactiveBtn != null) { inactiveBtn.setEnabled(false); }

        Novel novel = new Novel();
        novel.setTitle(title);
        novel.setDescription(descInput != null ? descInput.getText().toString().trim() : "");
        novel.setStatus(publish ? "ongoing" : "draft");
        novel.setPriceType(selectedPricing);
        novel.setPublished(publish);
        if (!selectedGenres.isEmpty()) novel.setGenres(selectedGenres);

        NovelRepository.Callback<Novel> cb = new NovelRepository.Callback<Novel>() {
            @Override public void onSuccess(Novel r) {
                // Upload cover if user picked one
                Object tag = coverPreview != null ? coverPreview.getTag() : null;
                if (tag instanceof byte[] && r.getId() != null) {
                    vm.uploadCover(r.getId(), (byte[]) tag, new NovelRepository.Callback<String>() {
                        @Override public void onSuccess(String url) {
                            // FIX: set the URL on the novel object so callers
                            // (MyWorksActivity, WriteFragment) show the cover immediately
                            r.setCoverImageUrl(url);
                            afterSave(r, publish);
                        }
                        @Override public void onError(String e) {
                            android.util.Log.w("NovelEditor", "Cover upload failed: " + e);
                            afterSave(r, publish); // proceed even without cover
                        }
                    });
                } else {
                    afterSave(r, publish);
                }
            }
            @Override public void onError(String e) {
                runOnUiThread(() -> {
                    resetButtons(activeBtn, inactiveBtn);
                    BannerHelper.error(NovelEditorActivity.this, "Save failed", e);
                });
            }
        };

        if (novelId != null) {
            novel.setId(novelId);
            vm.updateNovel(novel, cb);
        } else {
            vm.createNovel(novel, cb);
        }
    }

    /** Re-enable both buttons after a save attempt (success or failure). */
    private void resetButtons(View activeBtn, View inactiveBtn) {
        isSaving = false;
        LoadingSpinner.hide(this);
        if (activeBtn   != null) { activeBtn.setAlpha(1f);  activeBtn.setEnabled(true); }
        if (inactiveBtn != null) { inactiveBtn.setEnabled(true); }
    }

    private void afterSave(Novel novel, boolean published) {
        runOnUiThread(() -> {
            isSaving = false;
            LoadingSpinner.hide(this);
            // Queue the banner for the PARENT activity (WriteFragment host), not this one.
            // We call finish() immediately — onActivityResumed fires on the parent and drains
            // the queue, so the banner renders there instead of being destroyed with this activity.
            BannerConfig config = new BannerConfig()
                    .setType(published ? BannerType.SUCCESS : BannerType.INFO)
                    .setTitle(published ? "Published!" : "Draft saved")
                    .setMessage("\"" + novel.getTitle() + "\" " + (published ? "is now live." : "saved as draft."))
                    .setDuration(3500)
                    .setSwipeToDismiss(true);
            BannerManager.getInstance().queueForNextActivity(config);
            finish();
        });
    }

    private void loadNovel() {
        vm.getNovelById(novelId).observe(this, novel -> {
            if (novel == null) return;
            if (titleInput != null) titleInput.setText(novel.getTitle());
            if (descInput  != null) descInput.setText(novel.getDescription());

            // Restore genres
            if (novel.getGenres() != null && !novel.getGenres().isEmpty()) {
                selectedGenres = new java.util.ArrayList<>(novel.getGenres());
                updateGenreDisplay();
            }

            // Restore pricing
            selectPricing(novel.getPriceType() != null ? novel.getPriceType() : "free");

            // Load existing cover
            if (novel.getCoverImageUrl() != null && coverPreview != null) {
                com.bumptech.glide.Glide.with(this)
                    .load(novel.getCoverImageUrl())
                    .centerCrop()
                    .into(coverPreview);
            }
        });
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
