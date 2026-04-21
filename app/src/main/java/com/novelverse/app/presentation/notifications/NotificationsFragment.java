package com.novelverse.app.presentation.notifications;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.novelverse.app.R;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Notifications screen.
 * Currently shows an empty-state placeholder — real notification data
 * will be wired in once the Supabase Realtime channel is connected.
 */
@AndroidEntryPoint
public class NotificationsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }
}
