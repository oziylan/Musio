/*
from Documentation Spotify :
https://developer.spotify.com/documentation/web-api/concepts/authorization
https://developer.spotify.com/documentation/web-api/reference/search
https://developer.spotify.com/documentation/web-api/reference/get-an-artist

https://developer.spotify.com/documentation/web-api/tutorials/client-credentials-flow

The documentation is for the web API but the implementation for mobile is done with chatGPT

from Documentation Android :
-Localisation :
https://developers.google.com/location-context/fused-location-provider
https://developer.android.com/develop/sensors-and-location/location/retrieve-current#last-known

-UX/UI :
https://developer.android.com/reference/android/widget/SearchView
https://developer.android.com/reference/android/widget/ProgressBar


from Documentation Firebase :
https://firebase.google.com/docs/firestore/query-data/queries
https://firebase.google.com/docs/firestore/manage-data/add-data
https://firebase.google.com/docs/firestore/manage-data/transactions#transactions

Some implementations with deepseek

 */
package com.example.musio;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musio.adapters.MusicAdapter;
import com.example.musio.data.FirestoreRepository;
import com.example.musio.models.Music;
import com.example.musio.utils.MusicUtils;
import com.example.musio.utils.NetworkUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MusicActivity extends AppCompatActivity implements
        MusicAdapter.OnPlayClickListener,
        MusicAdapter.OnActionClickListener {

    private FirestoreRepository firestoreRepository;
    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    private List<Music> musicList;
    private FusedLocationProviderClient fusedLocationClient;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private ProgressBar progressBar;
    private LinearLayout instructionContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music);

        // Initializations of components and services
        firestoreRepository = new FirestoreRepository();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastKnownLocation();

        // Configuring the RecyclerView and the adapter
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        instructionContainer = findViewById(R.id.instructionContainer); // Initialize container
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        musicList = new ArrayList<>();

        // Creation of the adapter with the necessary listeners
        musicAdapter = new MusicAdapter(
                musicList,          // Music list
                this,              // Background
                this,              // Listener for play
                this,              // Listener for add
                null,              // Listener for like (null because not used here)
                R.layout.item_music_activity // Specific layout
        );
        recyclerView.setAdapter(musicAdapter);

        // Back button - simply closes the activity
        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        // Search bar configuration
        SearchView searchView = findViewById(R.id.searchView);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Hides the instructions when the search starts
                instructionContainer.setVisibility(View.GONE);
                showLoading(true);
                searchMusic(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                btnBack.setVisibility(ImageButton.GONE);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                searchView.setLayoutParams(params);
                // Hides the instructions when the SearchView takes focus
                instructionContainer.setVisibility(View.GONE);
            } else {
                btnBack.setVisibility(ImageButton.VISIBLE);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                params.addRule(RelativeLayout.ALIGN_PARENT_END);
                params.setMargins(0, 0, 16, 0);
                searchView.setLayoutParams(params);
                // Redisplay the instructions only if the list is empty
                if (musicList.isEmpty()) {
                    instructionContainer.setVisibility(View.VISIBLE);
                }
            }
        });

        searchView.setIconifiedByDefault(true);
    }

    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
            // Manages the visibility of the instruction container
            instructionContainer.setVisibility(show || !musicList.isEmpty() ? View.GONE : View.VISIBLE);
        });
    }

    @SuppressLint("MissingPermission")
    private void getLastKnownLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        Log.d("MusicActivity", "Position: " + currentLatitude + "," + currentLongitude);
                    }
                });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void searchMusic(String query) {
        new Thread(() -> {
            try {
                String accessToken = NetworkUtils.getAccessToken(BuildConfig.SPOTIFY_CLIENT_ID, BuildConfig.SPOTIFY_CLIENT_SECRET);
                if (accessToken == null) {
                    showToast("Erreur d'authentification Spotify");
                    showLoading(false);
                    return;
                }

                String response = NetworkUtils.searchTracks(query, accessToken);
                if (response == null) {
                    showToast("Aucun résultat trouvé");
                    showLoading(false);
                    return;
                }

                JSONObject json = new JSONObject(response);
                JSONArray tracks = json.optJSONObject("tracks").optJSONArray("items");
                if (tracks == null || tracks.length() == 0) {
                    showToast("Aucune piste trouvée");
                    showLoading(false);
                    return;
                }

                List<Music> newMusicList = new ArrayList<>();
                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject track = tracks.getJSONObject(i);
                    String id = track.optString("id", "");
                    String name = track.optString("name", "Titre inconnu");
                    String artist = track.getJSONArray("artists").getJSONObject(0).optString("name", "Artiste inconnu");
                    String imageUrl = track.optJSONObject("album").getJSONArray("images").getJSONObject(0).optString("url", "");
                    String genre = MusicUtils.getArtistGenre(track.getJSONArray("artists").getJSONObject(0).optString("id", ""), accessToken);

                    Music music = new Music(id, name, artist, imageUrl, genre, 0, currentLatitude, currentLongitude);
                    newMusicList.add(music);
                }

                runOnUiThread(() -> {
                    musicList.clear();
                    musicList.addAll(newMusicList);
                    musicAdapter.notifyDataSetChanged();
                    showLoading(false);
                });

            } catch (Exception e) {
                Log.e("MusicActivity", "Erreur recherche: " + e.getMessage(), e);
                showToast("Erreur lors de la recherche");
                showLoading(false);
            }
        }).start();
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    private void addMusicToFirestore(Music music) {
        firestoreRepository.getFirestore().collection("proposals")
                .whereEqualTo("id", music.getId())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            firestoreRepository.addMusic(music, new FirestoreRepository.AddMusicCallback() {
                                @Override
                                public void onAddSuccess(DocumentReference docRef) {
                                    Toast.makeText(MusicActivity.this, "Ajout réussi!", Toast.LENGTH_SHORT).show();
                                    setResult(RESULT_OK);
                                    finish();
                                }

                                @Override
                                public void onAddFailure(Exception e) {
                                    Toast.makeText(MusicActivity.this, "Échec de l'ajout", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Toast.makeText(this, "Cette musique existe déjà", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Erreur de vérification", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onPlayClick(String trackName, String artistName) {
        String youtubeUrl = MusicUtils.generateYouTubeUrl(trackName, artistName);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)));
    }

    @Override
    public void onActionClick(Music music) {
        addMusicToFirestore(music);
    }
}