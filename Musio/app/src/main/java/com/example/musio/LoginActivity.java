/*
 * From Firebase authentication documentation :
 * https://firebase.google.com/docs/auth/android/password-auth?authuser=4
 * https://github.com/firebase/snippets-android/blob/90d99933a308e9ec8dd5cb0f164a3640e5331f09/auth/app/src/main/java/com/google/firebase/quickstart/auth/EmailPasswordActivity.java#L62-L79
 * From StackOverflow :
 * Animation : https://stackoverflow.com/questions/5151591/android-left-to-right-slide-animation
 */

package com.example.musio;

import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.loginButton);
        TextView forgotPassword = findViewById(R.id.forgotPassword);
        Button signupRedirect = findViewById(R.id.signupRedirect);

        // TextWatcher to enable/disable the button
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInputs();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        emailEditText.addTextChangedListener(textWatcher);
        passwordEditText.addTextChangedListener(textWatcher);

        loginButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            signIn(email, password);
        });

        forgotPassword.setOnClickListener(v -> {
            showForgotPasswordDialog();
        });

        signupRedirect.setOnClickListener(v -> {
            startActivity(new Intent(this, SignUpActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });
    }

    private void validateInputs() {
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        Button loginButton = findViewById(R.id.loginButton);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        loginButton.setEnabled(!email.isEmpty() && !password.isEmpty());
    }
    //from Firebase Auth
    private void signIn(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null)
                            retrieveUserInfo(user.getUid());
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this,
                                "Échec de la connexion: " + getErrorMessage(task.getException()),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
    //from DeepSeek
    private String getErrorMessage(Exception exception) {
        if (exception instanceof FirebaseAuthInvalidUserException) {
            return "Aucun compte trouvé avec cet email";
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            return "Email ou mot de passe incorrect";
        } else {
            return "Erreur inconnue";
        }
    }
    //from DeepSeek
    private void showForgotPasswordDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Votre email");

        new AlertDialog.Builder(this)
                .setTitle("Réinitialisation du mot de passe")
                .setMessage("Entrez votre email pour recevoir un lien de réinitialisation")
                .setView(input)
                .setPositiveButton("Envoyer", (dialog, which) -> {
                    String email = input.getText().toString().trim();
                    if (!email.isEmpty()) {
                        sendPasswordResetEmail(email);
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }
    //from Firebase Auth documentation
    private void sendPasswordResetEmail(String email) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Email de réinitialisation envoyé",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this,
                                "Échec d'envoi de l'email: " + task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
    //from Copilot
    private void retrieveUserInfo(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String username = document.getString("username");
                            Log.d(TAG, "User Info: " + username);
                            startMainActivity();
                        } else {
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.w(TAG, "Error getting document", task.getException());
                    }
                });
    }

    private void startMainActivity() {
        // Building a new clean stack
        Intent mainIntent = new Intent(this, MainActivity.class);
        Intent homeIntent = new Intent(this, HomeActivity.class);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        TaskStackBuilder.create(this)
                .addNextIntent(homeIntent)
                .addNextIntent(mainIntent)
                .startActivities();

        // Animation from stackoverflow
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finishAffinity(); // Nettoie toutes les activités d'authentification
    }
}