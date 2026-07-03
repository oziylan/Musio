package com.example.musio;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.example.musio.data.FirestoreRepository;
import com.example.musio.models.Music;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FloatingActionButton fabLocation;
    private ImageButton btnBack, btnLegend;
    private FirestoreRepository firestoreRepository;
    private BottomSheetDialog legendBottomSheet;
    private boolean locationPermissionGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationPermissionGranted = checkLocationPermission();

        // night/day mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);

        // Init FirestoreRepository
        firestoreRepository = new FirestoreRepository();

        // Init buttons
        btnBack = findViewById(R.id.btnBack);
        btnLegend = findViewById(R.id.btnLegend);
        fabLocation = findViewById(R.id.fabLocation);

        // Config listeners of buttons
        btnBack.setOnClickListener(v -> finish());
        btnLegend.setOnClickListener(v -> showLegend());
        fabLocation.setOnClickListener(v -> {
            if (locationPermissionGranted) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Autorisez la localisation pour activer cette fonction", Toast.LENGTH_SHORT).show();
            }
        });

        // Init client localisation
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private boolean checkLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setAllGesturesEnabled(true);

        // Apply mapstyle
        mMap.setMapStyle(null);
        applyMapStyle();

        // Enable localisation
        if (locationPermissionGranted) {
            enableLocationFeatures();
        } else {
            fabLocation.setEnabled(false);
            Toast.makeText(this,
                    "Fonctionnalités de localisation limitées",
                    Toast.LENGTH_SHORT).show();
        }

        // Load & show markers
        loadMusicLocations();
    }

    private void loadMusicLocations() {
        firestoreRepository.fetchMusicProposals(new FirestoreRepository.FetchMusicCallback() {
            @Override
            public void onFetchSuccess(List<Music> musics) {
                runOnUiThread(() -> {
                    if (mMap != null) {
                        mMap.clear();
                        for (Music music : musics) {
                            if (music.getLatitude() != 0.0 && music.getLongitude() != 0.0) {
                                LatLng musicLocation = new LatLng(music.getLatitude(), music.getLongitude());
                                float color = getGenreColor(music.getGenre());

                                mMap.addMarker(new MarkerOptions()
                                        .position(musicLocation)
                                        .title(music.getTrackName())
                                        .snippet(music.getArtistName())
                                        .icon(BitmapDescriptorFactory.defaultMarker(color)));
                            }
                        }

                        if (!musics.isEmpty() && musics.get(0).getLatitude() != 0.0) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(musics.get(0).getLatitude(), musics.get(0).getLongitude()), 17));
                        }
                    }
                });
            }

            @Override
            public void onFetchFailure(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MapsActivity.this, "Erreur de chargement des positions", Toast.LENGTH_SHORT).show();
                    Log.e("MapsActivity", "Erreur Firestore: ", e);
                });
            }
        });
    }

    private void applyMapStyle() {
        try {
            int styleRes = isNightMode() ? R.raw.night_map_style : R.raw.day_map_style;
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, styleRes));

            if (!success) {
                Log.e("MapsActivity", "Échec de l'application du style");
                mMap.setMapStyle(null);
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapsActivity", "Style introuvable", e);
            mMap.setMapStyle(null);
        }
    }

    private boolean isNightMode() {
        int nightModeFlags = getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    @SuppressLint("MissingPermission")
    private void enableLocationFeatures() {
        if (locationPermissionGranted) {
            mMap.setMyLocationEnabled(true);
            fabLocation.setEnabled(true);
            getCurrentLocation();
        }
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        if (!locationPermissionGranted) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null && mMap != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17));
                    } else {
                        Toast.makeText(this,
                                "Localisation non disponible. Activez votre GPS",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Erreur de localisation",
                            Toast.LENGTH_SHORT).show();
                    Log.e("MapsActivity", "Location error", e);
                });
    }

    private void showLegend() {
        View bottomSheetView = getLayoutInflater().inflate(R.layout.legend_bottom_sheet, null);
        legendBottomSheet = new BottomSheetDialog(this);
        legendBottomSheet.setContentView(bottomSheetView);

        ImageButton closeButton = bottomSheetView.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> legendBottomSheet.dismiss());

        legendBottomSheet.show();
    }

    @Override
    protected void onDestroy() {
        if (legendBottomSheet != null && legendBottomSheet.isShowing()) {
            legendBottomSheet.dismiss();
        }
        super.onDestroy();
    }

    private float getGenreColor(String genre) {
        String genreLower = genre.toLowerCase();

        if (genreLower.contains("pop")) {
            return BitmapDescriptorFactory.HUE_ROSE;
        } else if (genreLower.contains("rock")) {
            return BitmapDescriptorFactory.HUE_RED;
        } else if (genreLower.contains("hip") || genreLower.contains("rap")) {
            return BitmapDescriptorFactory.HUE_ORANGE;
        } else if (genreLower.contains("electro") || genreLower.contains("edm") || genreLower.contains("techno")) {
            return BitmapDescriptorFactory.HUE_VIOLET;
        } else if (genreLower.contains("classique") || genreLower.contains("classical") || genreLower.contains("classic")) {
            return BitmapDescriptorFactory.HUE_YELLOW;
        } else if (genreLower.contains("jazz")) {
            return BitmapDescriptorFactory.HUE_GREEN;
        } else if (genreLower.contains("r&b") || genreLower.contains("rnb")) {
            return BitmapDescriptorFactory.HUE_BLUE;
        } else if (genreLower.contains("arabesk") || genreLower.contains("arabic")) {
            return BitmapDescriptorFactory.HUE_AZURE;
        } else if (genreLower.contains("metal")) {
            return BitmapDescriptorFactory.HUE_MAGENTA;
        } else {
            return BitmapDescriptorFactory.HUE_CYAN;
        }
    }
}