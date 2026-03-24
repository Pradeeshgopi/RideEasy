package com.rideeasy.conductor;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText conductorIdInput, passwordInput;
    MaterialButton loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        conductorIdInput = findViewById(R.id.conductorIdInput);
        passwordInput    = findViewById(R.id.passwordInput);
        loginBtn         = findViewById(R.id.loginBtn);

        // Always show login — no auto-login for security
        ApiHelper.authToken = "";

        loginBtn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String conductorId = conductorIdInput.getText().toString().trim();
        String password    = passwordInput.getText().toString().trim();

        if (conductorId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter your ID and password",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        loginBtn.setEnabled(false);
        loginBtn.setText("Signing in...");

        try {
            JSONObject body = new JSONObject();
            body.put("conductorId", conductorId);
            body.put("password", password);

            ApiHelper.post(AppConfig.LOGIN_URL, body, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        try {
                            if (response.getBoolean("success")) {
                                String token = response.getString("token");
                                ApiHelper.authToken = token;

                                JSONObject conductor = response.getJSONObject("conductor");

                                // Save to ONE SharedPreferences (fix for inconsistency)
                                SharedPreferences.Editor ed = getSharedPreferences(
                                        AppConfig.PREFS_NAME, MODE_PRIVATE).edit();
                                ed.putString(AppConfig.KEY_TOKEN,     token);
                                ed.putString(AppConfig.KEY_CONDUCTOR, conductor.getString("conductorId"));
                                ed.putString(AppConfig.KEY_BUS,       conductor.getString("busNumber"));
                                ed.putString(AppConfig.KEY_PLATE,     conductor.getString("numberPlate"));
                                ed.putString(AppConfig.KEY_ROUTE,     conductor.getString("route"));
                                ed.apply();

                                String cId = conductor.getString("conductorId");
                                Toast.makeText(LoginActivity.this,
                                        "Welcome " + cId + "! 🚍",
                                        Toast.LENGTH_SHORT).show();

                                goToDashboard();
                            } else {
                                resetLoginBtn();
                                String msg = response.optString("message", "Login failed");
                                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            resetLoginBtn();
                            Toast.makeText(LoginActivity.this,
                                    "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        resetLoginBtn();
                        Toast.makeText(LoginActivity.this,
                                "❌ Cannot connect to server!\nCheck your WiFi and AppConfig IP.",
                                Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (Exception e) {
            resetLoginBtn();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetLoginBtn() {
        loginBtn.setEnabled(true);
        loginBtn.setText("Start My Shift 🚍");
    }

    private void goToDashboard() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}