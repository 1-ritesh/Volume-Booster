package com.volumebooster.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.volumebooster.app.R;
import com.volumebooster.app.models.Song;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public interface OnSongClickListener {
        void onSongClick(int position);
    }

    private final List<Song> songs;
    private final OnSongClickListener listener;
    private int selectedIndex = -1;

    public SongAdapter(List<Song> songs, OnSongClickListener listener) {
        this.songs    = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songs.get(position);
        holder.tvTitle.setText(song.getTitle());
        holder.tvArtist.setText(song.getArtist());
        holder.tvDuration.setText(formatDuration(song.getDuration()));

        holder.itemView.setSelected(position == selectedIndex);
        holder.itemView.setOnClickListener(v -> {
            selectedIndex = holder.getAdapterPosition();
            notifyDataSetChanged();
            listener.onSongClick(selectedIndex);
        });
    }

    @Override
    public int getItemCount() { return songs.size(); }

    public void setSelectedIndex(int index) {
        selectedIndex = index;
        notifyDataSetChanged();
    }

    private String formatDuration(long ms) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(ms);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvArtist, tvDuration;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle    = itemView.findViewById(R.id.tvSongTitle);
            tvArtist   = itemView.findViewById(R.id.tvArtist);
            tvDuration = itemView.findViewById(R.id.tvDuration);
        }
    }
}
