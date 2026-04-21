package com.novelverse.app.presentation.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.novelverse.app.ui.banner.BannerHelper;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class BaseFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(getLayoutResId(), container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initViews(view);
        initObservers();
        initListeners();
    }

    @LayoutRes
    protected abstract int getLayoutResId();
    protected abstract void initViews(View view);
    protected abstract void initObservers();
    protected abstract void initListeners();

    protected void showMessage(String message) {
        if (isAdded()) BannerHelper.info(requireActivity(), message);
    }

    protected void showError(String message) {
        if (isAdded()) BannerHelper.error(requireActivity(), message);
    }

    protected void showSuccess(String message) {
        if (isAdded()) BannerHelper.success(requireActivity(), message);
    }

    protected void showWarning(String message) {
        if (isAdded()) BannerHelper.warning(requireActivity(), message);
    }
}
