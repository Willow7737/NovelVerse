package com.novelverse.app.presentation.profile.follows;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.novelverse.app.R;
import com.novelverse.app.domain.models.User;

public class UserFollowAdapter extends ListAdapter<User, UserFollowAdapter.UserViewHolder> {

    private OnItemClickListener   itemClickListener;
    private OnFollowClickListener followClickListener;

    public UserFollowAdapter() {
        super(new UserDiffCallback());
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    public void setOnFollowClickListener(OnFollowClickListener listener) {
        this.followClickListener = listener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_follow, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    public class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView avatarImage;
        private final TextView  displayNameText;
        private final TextView  usernameText;
        private final TextView  followButton;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage     = itemView.findViewById(R.id.avatar_image);
            displayNameText = itemView.findViewById(R.id.display_name_text);
            usernameText    = itemView.findViewById(R.id.username_text);
            followButton    = itemView.findViewById(R.id.btn_follow);

            itemView.setOnClickListener(v -> {
                // FIX: use getBindingAdapterPosition() — getAdapterPosition() is deprecated
                // and can return NO_POSITION during animations
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && itemClickListener != null) {
                    itemClickListener.onItemClick(getItem(pos));
                }
            });

            followButton.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION || followClickListener == null) return;

                User user = getItem(pos);
                // FIX: derive the desired new state from the User model, not the button text.
                // Button text can be stale; user.isFollowing() is the source of truth.
                boolean nowFollowing = !user.isFollowing();
                followClickListener.onFollowClick(user, nowFollowing);
                // Optimistic UI update — keep button in sync immediately
                setFollowingState(nowFollowing);
            });
        }

        public void bind(User user) {
            displayNameText.setText(user.getDisplayNameOrUsername());
            usernameText.setText("@" + user.getUsername());

            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(user.getAvatarUrl())
                        .placeholder(R.drawable.ic_avatar_placeholder)
                        .circleCrop()
                        .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_avatar_placeholder);
            }

            // FIX: use the actual isFollowing state from the model instead of hardcoding false
            setFollowingState(user.isFollowing());
        }

        private void setFollowingState(boolean isFollowing) {
            if (isFollowing) {
                followButton.setText(R.string.following);
                followButton.setBackgroundResource(R.drawable.bg_btn_secondary);
                followButton.setTextColor(
                        itemView.getContext().getColor(R.color.primary));
            } else {
                followButton.setText(R.string.follow);
                followButton.setBackgroundResource(R.drawable.bg_btn_primary);
                followButton.setTextColor(
                        itemView.getContext().getColor(R.color.surface));
            }
        }
    }

    static class UserDiffCallback extends DiffUtil.ItemCallback<User> {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            // FIX: include isFollowing so DiffUtil re-binds when follow state changes
            return oldItem.getUsername().equals(newItem.getUsername())
                    && oldItem.getDisplayNameOrUsername().equals(newItem.getDisplayNameOrUsername())
                    && oldItem.isFollowing() == newItem.isFollowing();
        }
    }

    public interface OnItemClickListener {
        void onItemClick(User user);
    }

    public interface OnFollowClickListener {
        void onFollowClick(User user, boolean follow);
    }
}
