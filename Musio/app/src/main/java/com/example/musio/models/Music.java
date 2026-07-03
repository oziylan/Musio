package com.example.musio.models;

import java.util.List;

public class Music {
    private String id;
    private String trackName;
    private String artistName;
    private String imageUrl;
    private String genre;
    private int likes;
    private String documentId;
    private double latitude;
    private double longitude;
    private boolean isLiked;
    private List<String> likedBy;

    // Empty constructor required for Firestore
    public Music() {}

    // Constructor with all fields
    public Music(String id, String trackName, String artistName, String imageUrl,
                 String genre, int likes, double latitude, double longitude) {
        this.id = id;
        this.trackName = trackName;
        this.artistName = artistName;
        this.imageUrl = imageUrl;
        this.genre = genre;
        this.likes = likes;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isLiked = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTrackName() {
        return trackName;
    }

    public void setTrackName(String trackName) {
        this.trackName = trackName;
    }

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public int getLikes() {
        return likes;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude=latitude;
    }
    public double getLongitude() {
        return longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude=longitude;
    }
    public boolean isLiked() {
        return isLiked;
    }
    public void setLiked(boolean liked) {
        isLiked = liked;
    }

    public void setLikedBy(List<String> likedBy) {
        this.likedBy = likedBy;
    }
}