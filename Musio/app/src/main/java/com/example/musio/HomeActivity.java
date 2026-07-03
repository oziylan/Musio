/*
 * From Firebase authentication documentation :
 * https://firebase.google.com/docs/auth/android/password-auth?authuser=4
 * https://github.com/firebase/snippets-android/blob/90d99933a308e9ec8dd5cb0f164a3640e5331f09/auth/app/src/main/java/com/google/firebase/quickstart/auth/EmailPasswordActivity.java#L62-L79
 *
 */
package com.example.musio;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_home);

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        findViewById(R.id.btnSignUp).setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Double vérification au cas où l'utilisateur se connecte puis revient en arrière
        if(FirebaseAuth.getInstance().getCurrentUser() != null) {
            startMainActivity();
        }
    }

    private void startMainActivity() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder.create(this)
                .addNextIntent(homeIntent)
                .addNextIntent(mainIntent)
                .startActivities();

        finish();
    }
}