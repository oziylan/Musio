/*
 * From Firebase authentication documentation :
 * https://firebase.google.com/docs/auth/android/password-auth?authuser=4
 * https://github.com/firebase/snippets-android/blob/90d99933a308e9ec8dd5cb0f164a3640e5331f09/auth/app/src/main/java/com/google/firebase/quickstart/auth/EmailPasswordActivity.java#L62-L79
 * From StackOverflow :
 * Animation : https://stackoverflow.com/questions/5151591/android-left-to-right-slide-animation
 */
package com.example.musio;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "SignUpActivity";
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Vérification précoce de la connexion
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            safeRedirectToMain();
            return;
        }

        setContentView(R.layout.activity_signup);
        initializeFirebase();
        setupUI();
    }

    private void initializeFirebase() {
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    private void setupUI() {
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        EditText confirmPasswordEditText = findViewById(R.id.confirmPassword);
        EditText usernameEditText = findViewById(R.id.username);
        Button signUpButton = findViewById(R.id.signupButton);
        Button loginRedirect = findViewById(R.id.loginRedirect);

        // Validation en temps réel
        TextWatcher validationWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateInputs();
            }
            @Override public void afterTextChanged(Editable s) {}
        };

        emailEditText.addTextChangedListener(validationWatcher);
        passwordEditText.addTextChangedListener(validationWatcher);
        confirmPasswordEditText.addTextChangedListener(validationWatcher);
        usernameEditText.addTextChangedListener(validationWatcher);

        signUpButton.setOnClickListener(v -> attemptRegistration());
        loginRedirect.setOnClickListener(v -> navigateToLogin());
    }

    private void attemptRegistration() {
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        EditText confirmPasswordEditText = findViewById(R.id.confirmPassword);
        EditText usernameEditText = findViewById(R.id.username);

        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();
        String username = usernameEditText.getText().toString().trim();

        if (!validateNetworkConnection()) return;
        if (!validateForm(email, password, confirmPassword, username)) return;

        showProgressDialog("Création du compte...");
        createFirebaseAccount(email, password, username);
    }

    private boolean validateNetworkConnection() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            showToast("Pas de connexion Internet");
            return false;
        }
        return true;
    }

    private boolean validateForm(String email, String password, String confirmPassword, String username) {
        if (TextUtils.isEmpty(email)) {
            showToast("Email requis");
            return false;
        }
        if (TextUtils.isEmpty(password)) {
            showToast("Mot de passe requis");
            return false;
        }
        if (password.length() < 6) {
            showToast("Le mot de passe doit contenir au moins 6 caractères");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            showToast("Les mots de passe ne correspondent pas");
            return false;
        }
        if (TextUtils.isEmpty(username)) {
            showToast("Nom d'utilisateur requis");
            return false;
        }
        return true;
    }

    private void createFirebaseAccount(String email, String password, String username) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            saveUserData(user, username);
                        } else {
                            showToast("Erreur lors de la création du compte");
                            hideProgressDialog();
                        }
                    } else {
                        hideProgressDialog();
                        Exception exception = task.getException();
                        if (exception instanceof FirebaseAuthException) {
                            handleFirebaseError((FirebaseAuthException) exception);
                        } else {
                            showToast("Erreur lors de l'inscription");
                            Log.e(TAG, "Erreur d'inscription", exception);
                        }
                    }
                });
    }
    //from chatGPT
    private void saveUserData(FirebaseUser user, String username) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", username);
        userData.put("email", user.getEmail());
        userData.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users").document(user.getUid())
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    hideProgressDialog();
                    showToast("Bienvenue " + username + "!");
                    safeRedirectToMain();
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    showToast("Échec de l'enregistrement des données");
                    Log.e(TAG, "Erreur Firestore", e);

                    // Nettoyage en cas d'échec
                    user.delete().addOnCompleteListener(deleteTask -> {
                        if (!deleteTask.isSuccessful()) {
                            Log.e(TAG, "Échec de la suppression du compte après échec Firestore");
                        }
                    });
                });
    }

    private void handleFirebaseError(FirebaseAuthException exception) {
        String errorCode = exception.getErrorCode();
        String errorMessage;

        switch (errorCode) {
            case "ERROR_INVALID_EMAIL":
                errorMessage = "Format d'email invalide";
                break;
            case "ERROR_WEAK_PASSWORD":
                errorMessage = "Le mot de passe doit contenir au moins 6 caractères";
                break;
            case "ERROR_EMAIL_ALREADY_IN_USE":
                errorMessage = "Cet email est déjà utilisé";
                break;
            default:
                errorMessage = "Erreur lors de l'inscription";
                Log.e(TAG, "Erreur Firebase: " + errorCode, exception);
        }

        showToast(errorMessage);
    }

    private void safeRedirectToMain() {
        runOnUiThread(() -> {
            try {
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                supportFinishAfterTransition();
            } catch (Exception e) {
                Log.e(TAG, "Erreur de redirection", e);
                finishAffinity();
            }
        });
    }

    private void navigateToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        supportFinishAfterTransition();
    }

    private void showProgressDialog(String message) {
        runOnUiThread(() -> {
            if (progressDialog == null || !progressDialog.isShowing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(message);
                builder.setCancelable(false);
                progressDialog = builder.create();
                progressDialog.show();
            }
        });
    }

    private void hideProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void validateInputs() {
        EditText emailEditText = findViewById(R.id.email);
        EditText passwordEditText = findViewById(R.id.password);
        EditText confirmPasswordEditText = findViewById(R.id.confirmPassword);
        EditText usernameEditText = findViewById(R.id.username);
        Button signUpButton = findViewById(R.id.signupButton);

        boolean isValid = !TextUtils.isEmpty(emailEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(passwordEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(confirmPasswordEditText.getText().toString().trim()) &&
                !TextUtils.isEmpty(usernameEditText.getText().toString().trim());

        signUpButton.setEnabled(isValid);
    }
}