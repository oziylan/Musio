/*
from Firestore Documentation :
https://firebase.google.com/docs/firestore
 */
package com.example.musio.data;

import android.util.Log;

import com.example.musio.models.Music;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository {

    private final FirebaseFirestore db;

    public FirestoreRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public FirebaseFirestore getFirestore() {
        return db;
    }

    // Method to recover the music from chatGPT and the documentation
    public void fetchMusicProposals(FetchMusicCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        db.collection("proposals")
                .orderBy("likes", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<Music> musics = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Music music = document.toObject(Music.class);
                                if (music.getTrackName() != null && music.getArtistName() != null) {
                                    music.setDocumentId(document.getId());
                                    music.setLatitude(document.getDouble("latitude") != null ? document.getDouble("latitude") : 0.0);
                                    music.setLongitude(document.getDouble("longitude") != null ? document.getDouble("longitude") : 0.0);
                                    music.setGenre(document.getString("genre") != null ? document.getString("genre") : "");

                                    // From DeepSeek - Check if the user has already liked this music
                                    if (currentUserId != null) {
                                        List<String> likedBy = (List<String>) document.get("likedBy");
                                        music.setLiked(likedBy != null && likedBy.contains(currentUserId));
                                    }

                                    musics.add(music);
                                } else {
                                    Log.w("FirestoreRepository", "Document mal formé ignoré : " + document.getId());
                                }
                            } catch (Exception e) {
                                Log.e("FirestoreRepository", "Erreur lors de la conversion du document : " + document.getId(), e);
                            }
                        }
                        callback.onFetchSuccess(musics);
                    } else {
                        callback.onFetchFailure(task.getException());
                    }
                });
    }

    // Method to add a music from chatGPT and the documentation
    public void addMusic(Music music, AddMusicCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("proposals")
                .add(music) // Add a new document
                .addOnSuccessListener(documentReference -> {
                    callback.onAddSuccess(documentReference);
                })
                .addOnFailureListener(e -> {
                    callback.onAddFailure(e);
                });
    }

    // Method to increment the likes of a music from ChatGPT and the documentation
    // From DeepSeek - Modified method to manage likes with userId
    public void updateLikes(String documentId, int newLikes, boolean isLiked, UpdateLikesCallback callback) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DocumentReference musicRef = db.collection("proposals").document(documentId);

        db.runTransaction(transaction -> {
                    // Get the current document
                    DocumentSnapshot snapshot = transaction.get(musicRef);

                    // Check if the user has already liked
                    List<String> likedBy = snapshot.get("likedBy") != null ?
                            (List<String>) snapshot.get("likedBy") : new ArrayList<>();

                    boolean alreadyLiked = likedBy.contains(userId);

                    if (isLiked && !alreadyLiked) {
                        // Add the like
                        likedBy.add(userId);
                        transaction.update(musicRef, "likes", newLikes);
                        transaction.update(musicRef, "likedBy", likedBy);
                    } else if (!isLiked && alreadyLiked) {
                        // Remove the like
                        likedBy.remove(userId);
                        transaction.update(musicRef, "likes", newLikes);
                        transaction.update(musicRef, "likedBy", likedBy);
                    }
                    return null;
                }).addOnSuccessListener(aVoid -> callback.onUpdateSuccess())
                .addOnFailureListener(callback::onUpdateFailure);
    }

    public void clearPlaylist(ClearPlaylistCallback callback) {
        db.collection("proposals")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            db.collection("proposals").document(document.getId()).delete();
                        }
                        callback.onClearSuccess();
                    } else {
                        callback.onClearFailure(task.getException());
                    }
                });
    }
    //from documentation and ChatGPT
    public void getUserName(String userId, GetUserCallback callback) {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        callback.onFetchSuccess(username);
                    } else {
                        callback.onFetchFailure(new Exception("Utilisateur non trouvé"));
                    }
                })
                .addOnFailureListener(e -> callback.onFetchFailure(e));
    }
    //from documentation and ChatGPT
    public void setupMusicProposalsListener(FetchMusicCallback callback) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        db.collection("proposals")
                .orderBy("likes", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        callback.onFetchFailure(error);
                        return;
                    }

                    List<Music> musics = new ArrayList<>();
                    for (QueryDocumentSnapshot document : value) {
                        try {
                            Music music = document.toObject(Music.class);
                            music.setDocumentId(document.getId());

                            // Retrieving the likedBy list
                            List<String> likedBy = (List<String>) document.get("likedBy");
                            if (likedBy == null) {
                                likedBy = new ArrayList<>();
                            }

                            // Sets whether the current user has liked this music
                            music.setLiked(currentUserId != null && likedBy.contains(currentUserId));
                            music.setLikedBy(likedBy);

                            musics.add(music);
                        } catch (Exception e) {
                            Log.e("FirestoreRepo", "Error parsing doc", e);
                        }
                    }
                    callback.onFetchSuccess(musics);
                });
    }

    // Interface to retrieve the username
    public interface GetUserCallback {
        void onFetchSuccess(String username);
        void onFetchFailure(Exception e);
    }


    // Interface to manage deletion callbacks
    public interface ClearPlaylistCallback {
        void onClearSuccess();
        void onClearFailure(Exception e);
    }

    // Interfaces to manage callbacks
    public interface FetchMusicCallback {
        void onFetchSuccess(List<Music> musics);

        void onFetchFailure(Exception e);
    }

    public interface AddMusicCallback {
        void onAddSuccess(DocumentReference documentReference);

        void onAddFailure(Exception e);
    }

    public interface UpdateLikesCallback {
        void onUpdateSuccess();

        void onUpdateFailure(Exception e);
    }
}
