package com.novelverse.app.presentation.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.novelverse.app.R;
import java.util.Arrays;
import java.util.List;

/** Task 42: Genre grid RecyclerView adapter */
public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.VH> {

    public interface OnGenreClickListener { void onClick(String genre); }

    private static final List<String[]> GENRES = Arrays.asList(
        new String[]{"Fantasy",   "#9B59B6", "ic_genre_fantasy"},
        new String[]{"Romance",   "#FF6B9D", "ic_genre_romance"},
        new String[]{"Sci-Fi",    "#3498DB", "ic_genre_scifi"},
        new String[]{"Mystery",   "#E74C3C", "ic_genre_mystery"},
        new String[]{"Thriller",  "#F39C12", "ic_genre_thriller"},
        new String[]{"Horror",    "#2C3E50", "ic_genre_horror"},
        new String[]{"Adventure", "#27AE60", "ic_genre_adventure"},
        new String[]{"Historical","#8B6914", "ic_genre_historical"},
        new String[]{"Comedy",    "#F1C40F", "ic_genre_comedy"},
        new String[]{"Drama",     "#E67E22", "ic_genre_drama"},
        new String[]{"Action",    "#C0392B", "ic_genre_action"},
        new String[]{"Young Adult","#6C3483","ic_genre_young_adult"}
    );

    private final OnGenreClickListener listener;
    public GenreAdapter(OnGenreClickListener l) { this.listener = l; }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_genre_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        String[] g = GENRES.get(pos);
        h.name.setText(g[0]);
        try {
            h.itemView.setBackgroundColor(android.graphics.Color.parseColor(g[1]));
        } catch (Exception ignored) {}
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onClick(g[0]); });
    }

    @Override public int getItemCount() { return GENRES.size(); }

    static class VH extends RecyclerView.ViewHolder {
        TextView name;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.genre_name);
        }
    }
}
