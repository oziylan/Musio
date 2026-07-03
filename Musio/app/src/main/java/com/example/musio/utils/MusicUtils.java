package com.example.musio.utils;

import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MusicUtils {

    /**
     * from DeepSeek
     * Generates a YouTube search link for a given track and artist.
     */
    public static String generateYouTubeUrl(String trackName, String artistName) {
        String query = Uri.encode(trackName + " " + artistName);
        return "https://www.youtube.com/results?search_query=" + query + "&sp=EgIQAQ%3D%3D";
    }

    /**
     * from DeepSeek
     * Retrieves the first musical genre of an artist from his ID.
     */
    public static String getArtistGenre(String artistId, String accessToken) {
        try {
            URL url = new URL("https://api.spotify.com/v1/artists/" + artistId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONObject artistJson = new JSONObject(response.toString());
                JSONArray genres = artistJson.optJSONArray("genres");
                if (genres != null && genres.length() > 0) {
                    return genres.getString(0); // Retourne le premier genre
                }
            } else {
                Log.e("MusicUtils", "Failed to fetch artist info: " + responseCode);
            }
        } catch (Exception e) {
            Log.e("MusicUtils", "Error fetching artist info: " + e.getMessage(), e);
        }
        return "Unknown"; // Returns a default value if no gender is found
    }
}