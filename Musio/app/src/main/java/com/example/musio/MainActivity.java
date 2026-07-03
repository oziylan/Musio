/*
from Documentation Android :
Permitions :
https://developer.android.com/training/permissions/requesting
https://developer.android.com/develop/ui/views/notifications/notification-permission

RecyclerView : https://developer.android.com/develop/ui/views/layout/recyclerview

AlertDialog : https://developer.android.com/develop/ui/views/components/dialogs

Button : https://developer.android.com/develop/ui/views/components/button

icons : https://fonts.google.com/icons
 */

package com.example.musio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musio.adapters.MusicAdapter;
import com.example.musio.data.FirestoreRepository;
import com.example.musio.models.Music;
import com.example.musio.services.PlaylistService;
import com.example.musio.utils.MusicUtils;
import com.example.musio.utils.NotificationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements
        MusicAdapter.OnPlayClickListener,
        MusicAdapter.OnActionClickListener,
        MusicAdapter.OnLikeClickListener {

    private static final String TAG = "MainActivity";
    private static final int MUSIC_ACTIVITY_REQUEST_CODE = 1;

    private FirestoreRepository firestoreRepository;
    private RecyclerView recyclerView;
    private MusicAdapter musicAdapter;
    private List<Music> musicList;
    private SearchView searchView;
    private TextView headerTitle;
    private ImageView logoImageView;
    private List<Music> originalMusicList;
    private TextView emptyView;
    private TextView noResultsView;
    private ProgressBar progressBar;
    public static boolean locationPermissionGranted = false;
    public static boolean notificationPermissionGranted = false;
    private static final int LOCATION_REQUEST_CODE = 1001;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                locationPermissionGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                        result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);

                if (locationPermissionGranted) {
                    Log.d(TAG, "Permissions de localisation accordées");
                    // Check the notifications again after obtaining the location
                    checkNotificationPermission();
                } else {
                    Toast.makeText(this, "Fonctionnalités de localisation limitées", Toast.LENGTH_LONG).show();
                }
            });

    // Launcher for notifications
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                notificationPermissionGranted = granted;
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Authentication check
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            redirectToHome();
            return;
        }

        setContentView(R.layout.activity_main);
        initializeViews();
        initializeServices();
        checkAllPermissions();
        setupRecyclerView();
        setupSearchView();
        setupNavigationButtons();
        displayUsername();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recyclerView);
        searchView = findViewById(R.id.searchView);
        headerTitle = findViewById(R.id.header_title);
        logoImageView = findViewById(R.id.app_logo);
        emptyView = findViewById(R.id.emptyView);
        noResultsView = findViewById(R.id.noResultsView);
        progressBar = findViewById(R.id.progressBar);
    }

    //from deepSeek
    private void initializeServices() {
        startService(new Intent(this, PlaylistService.class));
        NotificationUtils.createNotificationChannel(this);
        firestoreRepository = new FirestoreRepository();

        try {
            Picasso picasso = new Picasso.Builder(this)
                    .indicatorsEnabled(false)
                    .loggingEnabled(BuildConfig.DEBUG)
                    .build();
            Picasso.setSingletonInstance(picasso);
        } catch (IllegalStateException e) {
            Log.d(TAG, "Picasso already initialized");
        }
    }


    private void checkAllPermissions() {
        // First check the location
        boolean hasLocation = checkLocationPermission();

        // Check notifications independently
        checkNotificationPermission();

        // If no permission is granted, request the location first
        if (!hasLocation && !notificationPermissionGranted) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }, 300);
    }
    //from DeepSeek
    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionGranted = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;

            if (!notificationPermissionGranted && !isFinishing()) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!notificationPermissionGranted) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    }
                }, 1000); // Longer delay for a better UX
            }
        } else {
            notificationPermissionGranted = true;
        }
    }

    private boolean checkLocationPermission() {
        boolean hasFine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        locationPermissionGranted = hasFine || hasCoarse;

        if (!locationPermissionGranted) {
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                locationPermissionLauncher.launch(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
            }, 300);
        }

        return locationPermissionGranted;
    }
    //from ChatGPT
    private void setupRecyclerView() {
        musicList = new ArrayList<>();
        musicAdapter = new MusicAdapter(musicList, this, this, this, this, R.layout.item_music_main);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(musicAdapter);
        setupRealTimeUpdates();
    }

    private void setupRealTimeUpdates() {
        // Display the loader during the initial loading
        showLoading(true);

        firestoreRepository.setupMusicProposalsListener(new FirestoreRepository.FetchMusicCallback() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onFetchSuccess(List<Music> musics) {
                musicList.clear();
                musicList.addAll(musics);
                sortByLikes();
                musicAdapter.notifyDataSetChanged();

                // Hide the loader once the data has been loaded
                showLoading(false);

                // Show or hide the appropriate messages
                updateViewVisibility();
            }

            @Override
            public void onFetchFailure(Exception e) {
                // Hide the loader in case of error
                showLoading(false);
                Toast.makeText(MainActivity.this, "Erreur de chargement des musiques", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "Error loading music", e);
            }
        });
    }
    //from DeepSeek
    private void updateViewVisibility() {
        if (musicList.isEmpty()) {
            // Check if it's an empty search or the initial empty list
            String query = searchView.getQuery().toString();
            if (!query.isEmpty()) {
                // Search without results
                noResultsView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
            } else {
                // Initial empty list
                emptyView.setVisibility(View.VISIBLE);
                noResultsView.setVisibility(View.GONE);
                recyclerView.setVisibility(View.GONE);
            }
        } else {
            // There are results
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            noResultsView.setVisibility(View.GONE);
        }
    }
    //from ChatGPT
    private void showLoading(boolean show) {
        runOnUiThread(() -> {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.GONE);
                noResultsView.setVisibility(View.GONE);
            } else {
                updateViewVisibility();
            }
        });
    }

    private void sortByLikes() {
        musicList.sort((m1, m2) -> Integer.compare(m2.getLikes(), m1.getLikes()));
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterMusicList(newText);
                return true;
            }
        });
        searchView.setOnSearchClickListener(v -> {
            // Hide the title and the logo when the SearchView is activated
            headerTitle.setVisibility(View.GONE);
            logoImageView.setVisibility(View.GONE);
        });

        searchView.setOnCloseListener(() -> {
            // Re-display the title and logo when the SearchView is closed
            headerTitle.setVisibility(View.VISIBLE);
            logoImageView.setVisibility(View.VISIBLE);
            return false;
        });
    }
    //from chatGPT
    @SuppressLint("NotifyDataSetChanged")
    private void filterMusicList(String query) {
        if (originalMusicList == null) {
            originalMusicList = new ArrayList<>(musicList);
        }

        if (query.isEmpty()) {
            // Reset the list
            musicList.clear();
            musicList.addAll(originalMusicList);
        } else {
            // Filter the list
            List<Music> filteredList = new ArrayList<>();
            String lowerCaseQuery = query.toLowerCase();

            for (Music music : originalMusicList) {
                if (music.getTrackName().toLowerCase().contains(lowerCaseQuery) ||
                        music.getArtistName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(music);
                }
            }

            musicList.clear();
            musicList.addAll(filteredList);
        }

        // Update the display
        updateViewVisibility();
        musicAdapter.notifyDataSetChanged();
    }

    private void setupNavigationButtons() {
        findViewById(R.id.bt_music).setOnClickListener(v ->
                startActivityForResult(new Intent(this, MusicActivity.class), MUSIC_ACTIVITY_REQUEST_CODE));

        findViewById(R.id.bt_logout).setOnClickListener(v -> logoutUser());

        findViewById(R.id.bt_map).setOnClickListener(v -> {
            if (locationPermissionGranted) {
                startActivity(new Intent(this, MapsActivity.class));
            } else {
                Toast.makeText(this, "Activez la localisation pour utiliser cette fonction", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Déconnexion")
                .setMessage("Voulez-vous vraiment vous déconnecter ?")
                .setPositiveButton("Oui", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    redirectToHome();
                })
                .setNegativeButton("Non", null)
                .show();
    }
    //from DeepSeek to manage stack
    private void redirectToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    //from DeepSeek
    private void displayUsername() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            firestoreRepository.getUserName(currentUser.getUid(), new FirestoreRepository.GetUserCallback() {
                @Override
                public void onFetchSuccess(String username) {
                    if (username != null && !username.isEmpty()) {
                        username = username.substring(0, 1).toUpperCase() + username.substring(1).toLowerCase();
                        TextView titleTextView = findViewById(R.id.header_title);
                        String title = "Musio pour " + username;
                        SpannableString spannableString = new SpannableString(title);
                        spannableString.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 5, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        titleTextView.setText(spannableString);
                    }
                }

                @Override
                public void onFetchFailure(Exception e) {
                    Log.e(TAG, "Error fetching username", e);
                }
            });
        }
    }

    @Override
    public void onPlayClick(String trackName, String artistName) {
        String youtubeUrl = MusicUtils.generateYouTubeUrl(trackName, artistName);
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)));
    }

    @Override
    public void onActionClick(Music music) {
        Toast.makeText(this, "Musique sélectionnée : " + music.getTrackName(), Toast.LENGTH_SHORT).show();
    }
    //from ChatGPT
    @Override
    public void onLikeClick(Music music, boolean isLiked) {
        int newLikes = isLiked ? music.getLikes() + 1 : music.getLikes() - 1;
        firestoreRepository.updateLikes(music.getDocumentId(), newLikes, isLiked,
                new FirestoreRepository.UpdateLikesCallback() {
                    @SuppressLint("NotifyDataSetChanged")
                    @Override
                    public void onUpdateSuccess() {
                        music.setLikes(newLikes);
                        music.setLiked(isLiked);
                        sortByLikes();
                        musicAdapter.notifyDataSetChanged();
                        String message = isLiked ? "Ajouté aux favoris" : "Retiré des favoris";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onUpdateFailure(Exception e) {
                        Toast.makeText(MainActivity.this, "Erreur de mise à jour", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error updating likes", e);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}