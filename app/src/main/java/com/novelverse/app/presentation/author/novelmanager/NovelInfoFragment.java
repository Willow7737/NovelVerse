package com.novelverse.app.presentation.author.novelmanager;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.novelverse.app.R;
import com.novelverse.app.data.repository.NovelRepository;
import com.novelverse.app.domain.models.Novel;
import com.novelverse.app.presentation.write.WriterViewModel;
import com.novelverse.app.ui.LoadingSpinner;
import com.novelverse.app.ui.banner.BannerHelper;
import com.novelverse.app.ui.banner.BannerManager;
import com.novelverse.app.ui.banner.BannerConfig;
import com.novelverse.app.ui.banner.BannerType;

import java.io.ByteArrayOutputStream;

public class NovelInfoFragment extends Fragment {

    private static final String ARG_NOVEL_ID = "novel_id";
    private WriterViewModel vm;
    private Novel           currentNovel;
    private ImageView       coverView;
    private EditText        titleField, descField;
    private TextView        statusOngoing, statusHiatus, statusCompleted;
    private String          selectedStatus = "ongoing";
    private byte[]          pendingCoverBytes;

    private final ActivityResultLauncher<Intent> imagePicker =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() != android.app.Activity.RESULT_OK || result.getData() == null) return;
            Uri uri = result.getData().getData();
            if (uri == null) return;
            try {
                Bitmap raw = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
                // Scale to cover dimensions
                Bitmap scaled = Bitmap.createScaledBitmap(raw, 800, 1100, true);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                scaled.compress(Bitmap.CompressFormat.JPEG, 88, out);
                pendingCoverBytes = out.toByteArray();
                if (coverView != null) {
                    Glide.with(this).load(pendingCoverBytes).centerCrop().into(coverView);
                }
                BannerHelper.info(requireActivity(), "Cover selected", "Tap Save Changes to apply.");
            } catch (Exception e) {
                BannerHelper.error(requireActivity(), "Couldn't load image");
            }
        });

    public static NovelInfoFragment newInstance(String novelId) {
        NovelInfoFragment f = new NovelInfoFragment();
        Bundle b = new Bundle();
        b.putString(ARG_NOVEL_ID, novelId);
        f.setArguments(b);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater i, @Nullable ViewGroup c, @Nullable Bundle s) {
        return i.inflate(R.layout.fragment_novel_info, c, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String novelId = getArguments() != null ? getArguments().getString(ARG_NOVEL_ID) : null;
        coverView      = view.findViewById(R.id.info_cover);
        titleField     = view.findViewById(R.id.info_title);
        descField      = view.findViewById(R.id.info_description);
        statusOngoing  = view.findViewById(R.id.status_ongoing);
        statusHiatus   = view.findViewById(R.id.status_hiatus);
        statusCompleted= view.findViewById(R.id.status_completed);

        statusOngoing.setOnClickListener(v  -> selectStatus("ongoing"));
        statusHiatus.setOnClickListener(v   -> selectStatus("hiatus"));
        statusCompleted.setOnClickListener(v-> selectStatus("completed"));

        view.findViewById(R.id.btn_change_cover).setOnClickListener(v -> pickImage());
        view.findViewById(R.id.btn_save_info).setOnClickListener(v -> saveInfo());

        if (novelId == null) return;
        vm = new ViewModelProvider(requireActivity()).get(WriterViewModel.class);
        vm.getNovelById(novelId).observe(getViewLifecycleOwner(), this::populateFields);
    }

    private void populateFields(Novel novel) {
        if (novel == null || currentNovel != null) return; // don't re-populate while editing
        currentNovel = novel;
        if (titleField != null) titleField.setText(novel.getTitle());
        if (descField  != null) descField.setText(novel.getDescription());
        String status = novel.getStatus() != null ? novel.getStatus() : "ongoing";
        selectStatus(status);
        if (coverView != null && novel.getCoverImageUrl() != null && !novel.getCoverImageUrl().isEmpty()) {
            Glide.with(this).load(novel.getCoverImageUrl())
                    .placeholder(R.drawable.img_placeholder_cover).centerCrop().into(coverView);
        }
    }

    private void selectStatus(String status) {
        selectedStatus = status;
        applyChip(statusOngoing,  "ongoing".equals(status));
        applyChip(statusHiatus,   "hiatus".equals(status));
        applyChip(statusCompleted,"completed".equals(status));
    }

    private void applyChip(TextView tv, boolean active) {
        if (tv == null) return;
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        if (active) {
            bg.setColor(resolveAttrColor(com.google.android.material.R.attr.colorPrimary));
            tv.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnPrimary));
        } else {
            bg.setColor(resolveAttrColor(com.google.android.material.R.attr.colorSurfaceVariant));
            bg.setStroke(dp(1), resolveAttrColor(com.google.android.material.R.attr.colorOutline));
            tv.setTextColor(resolveAttrColor(com.google.android.material.R.attr.colorOnSurfaceVariant));
        }
        tv.setBackground(bg);
    }

    private void pickImage() {
        Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        imagePicker.launch(i);
    }

    private void saveInfo() {
        if (currentNovel == null) return;
        if (getActivity() == null) return;
        String title = titleField != null ? titleField.getText().toString().trim() : "";
        if (title.isEmpty()) { BannerHelper.warning(requireActivity(), "Title required"); return; }

        LoadingSpinner.show(requireActivity());

        currentNovel.setTitle(title);
        currentNovel.setDescription(descField != null ? descField.getText().toString().trim() : "");
        currentNovel.setStatus(selectedStatus);

        if (pendingCoverBytes != null) {
            vm.uploadCover(currentNovel.getId(), pendingCoverBytes, new NovelRepository.Callback<String>() {
                @Override public void onSuccess(String url) {
                    currentNovel.setCoverImageUrl(url);
                    persistInfo();
                }
                @Override public void onError(String e) { persistInfo(); }
            });
        } else {
            persistInfo();
        }
    }

    private void persistInfo() {
        vm.updateNovel(currentNovel, new NovelRepository.Callback<Novel>() {
            @Override public void onSuccess(Novel r) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    LoadingSpinner.hide(requireActivity());
                    BannerConfig cfg = new BannerConfig()
                            .setType(BannerType.SUCCESS).setTitle("Saved!")
                            .setMessage("Novel info updated.").setDuration(2500);
                    BannerManager.getInstance().show(requireActivity(), cfg);
                    currentNovel = null; // allow re-populate on next observe
                });
            }
            @Override public void onError(String e) {
                if (getActivity() == null) return;
                requireActivity().runOnUiThread(() -> {
                    LoadingSpinner.hide(requireActivity());
                    BannerHelper.error(requireActivity(), "Save failed", e);
                });
            }
        });
    }

    private int dp(int v) { return Math.round(v * requireContext().getResources().getDisplayMetrics().density); }
    private int resolveAttrColor(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

}
