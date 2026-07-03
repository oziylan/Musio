/*
from Documentation Spotify :
https://developer.spotify.com/documentation/android/tutorials/getting-started
https://developer.spotify.com/documentation/android/tutorials/authorization
 */
package com.example.musio.utils;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NetworkUtils {

    private static final String TAG = "NetworkUtils";
    private static final String SPOTIFY_AUTH_URL = "https://accounts.spotify.com/api/token";
    private static final String SPOTIFY_API_URL = "https://api.spotify.com/v1";

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .callTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    /**
     * From DeepSeek
     * Obtains an access token for the Spotify API.
     */
    public static String getAccessToken(String clientId, String clientSecret) {
        if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
            Log.e(TAG, "Client ID ou Client Secret est null ou vide.");
            return null;
        }

        // Encode Client ID and Client Secret in Base64
        String credentials = clientId + ":" + clientSecret;
        String auth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);

        // Create the body of the request
        RequestBody requestBody = new FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build();

        // Create the Request
        Request request = new Request.Builder()
                .url(SPOTIFY_AUTH_URL)
                .post(requestBody)
                .header("Authorization", auth)
                .build();

        // Execute the request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Erreur lors de la récupération du token : Code " + response.code() + ", Message: " + response.message());
                return null;
            }

            // Parse the JSON response
            String responseData = response.body().string();
            JSONObject json = new JSONObject(responseData);
            return json.optString("access_token", null);
        } catch (IOException e) {
            Log.e(TAG, "Erreur réseau lors de la récupération du token : " + e.getMessage());
        } catch (JSONException e) {
            Log.e(TAG, "Erreur JSON lors du parsing du token : " + e.getMessage());
        }
        return null;
    }

    /**
     * Search for songs on Spotify.
     */
    public static String searchTracks(String query, String accessToken) {
        if (query == null || query.isEmpty()) {
            Log.e(TAG, "La requête de recherche est vide ou null.");
            return null;
        }

        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Le token d'accès est null ou vide.");
            return null;
        }

        // Create the request
        Request request = new Request.Builder()
                .url(SPOTIFY_API_URL + "/search?q=" + query + "&type=track")
                .header("Authorization", "Bearer " + accessToken)
                .build();

        // Execute the request
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                Log.e(TAG, "Erreur lors de la recherche de morceaux : Code " + response.code() + ", Message: " + response.message());
                return null;
            }
            return response.body().string();
        } catch (IOException e) {
            Log.e(TAG, "Erreur réseau lors de la recherche de morceaux : " + e.getMessage());
        }
        return null;
    }
}
