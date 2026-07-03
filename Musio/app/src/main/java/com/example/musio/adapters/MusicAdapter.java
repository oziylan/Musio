package com.example.musio.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musio.R;
import com.example.musio.models.Music;
import com.squareup.picasso.Picasso;

import java.util.List;

public class MusicAdapter extends RecyclerView.Adapter<MusicAdapter.MusicViewHolder> {

    public interface OnPlayClickListener {
        void onPlayClick(String trackName, String artistName);
    }

    public interface OnActionClickListener {
        void onActionClick(Music music);
    }

    public interface OnLikeClickListener {
        void onLikeClick(Music music, boolean isLiked); // from DeepSeek - Adding the isLiked parameter
    }

    private final List<Music> musicList;
    private final Context context;
    private final OnPlayClickListener onPlayClickListener;
    private final OnActionClickListener onActionClickListener;
    private final OnLikeClickListener onLikeClickListener;
    private final int layoutResource;

    public MusicAdapter(List<Music> musicList, Context context, OnPlayClickListener onPlayClickListener,
                        OnActionClickListener onActionClickListener, OnLikeClickListener onLikeClickListener,
                        int layoutResource) {
        this.musicList = musicList;
        this.context = context;
        this.onPlayClickListener = onPlayClickListener;
        this.onActionClickListener = onActionClickListener;
        this.onLikeClickListener = onLikeClickListener;
        this.layoutResource = layoutResource;
    }

    @NonNull
    @Override
    public MusicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
        return new MusicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MusicViewHolder holder, int position) {
        Music music = musicList.get(position);
        holder.bind(music);

        // from DeepSeek - Updating the status of the like button
        ImageButton btnLike = holder.itemView.findViewById(R.id.btnLike);
        if (btnLike != null) {
            btnLike.setSelected(music.isLiked()); // Sets the selected state for the selector
        }

        TextView textViewLikes = holder.itemView.findViewById(R.id.textViewLikes);
        if (textViewLikes != null) {
            textViewLikes.setText(String.valueOf(music.getLikes()));
        }

        View btnPlay = holder.itemView.findViewById(R.id.btnPlay);
        if (btnPlay != null) {
            btnPlay.setOnClickListener(v -> {
                if (onPlayClickListener != null) {
                    onPlayClickListener.onPlayClick(music.getTrackName(), music.getArtistName());
                }
            });
        }

        // from DeepSeek - Modification of the click handler for the like
        if (btnLike != null) {
            btnLike.setOnClickListener(v -> {
                if (onLikeClickListener != null) {
                    boolean newLikeState = !music.isLiked();
                    onLikeClickListener.onLikeClick(music, newLikeState);

                    // Immediate update of the UI
                    music.setLiked(newLikeState);
                    btnLike.setSelected(newLikeState);

                    // Updating the counter
                    int newLikes = newLikeState ? music.getLikes() + 1 : music.getLikes() - 1;
                    music.setLikes(newLikes);
                    if (textViewLikes != null) {
                        textViewLikes.setText(String.valueOf(newLikes));
                    }
                }
            });
        }

        View btnAdd = holder.itemView.findViewById(R.id.btnAdd);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> {
                if (onActionClickListener != null) {
                    onActionClickListener.onActionClick(music);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }

    public void updateList(List<Music> newMusicList) {
        musicList.clear();
        musicList.addAll(newMusicList);
        notifyDataSetChanged();
    }

    public static class MusicViewHolder extends RecyclerView.ViewHolder {

        private final ImageView imageView;
        private final TextView textViewName;
        private final TextView textViewArtist;

        public MusicViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageViewCover);
            textViewName = itemView.findViewById(R.id.textViewName);
            textViewArtist = itemView.findViewById(R.id.textViewArtist);
        }

        public void bind(Music music) {
            textViewName.setText(music.getTrackName());
            textViewArtist.setText(music.getArtistName());

            if (music.getImageUrl() != null && !music.getImageUrl().isEmpty()) {
                try {
                    Picasso.get()
                            .load(music.getImageUrl()) // Loads the image from the URL
                            .placeholder(R.drawable.music_note_24px) // Temporary image while loading
                            .error(R.drawable.music_note_24px) // Default image if an error occurs
                            .into(imageView); // Assign the image to the ImageView
                } catch (Exception e) {
                    Log.e("MusicAdapter", "Erreur avec Picasso : " + e.getMessage(), e);
                    imageView.setImageResource(R.drawable.music_note_24px); // Default image if an exception is thrown
                }
            } else {
                imageView.setImageResource(R.drawable.music_note_24px); // Default image if the URL is null or empty
            }
        }

    }
}