package com.rideeasy.conductor;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;
import android.location.Location;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    // ─── Views ───────────────────────────────────────────────────────────────
    TextView busNumberText, routeText, conductorIdText, shiftTimeText, numberPlateText;
    TextView crowdStatusText, crowdPercentText, passengerCountText, seatsText, standingText;
    TextView revenueText, revenueSubText, totalTicketsText, totalBoardedText, totalAlightedText;
    TextView gpsButtonText, gpsCoordText;
    LinearLayout activityFeed;
    MaterialCardView issueTicketBtn, alightBtn, gpsCard;
    com.google.android.material.button.MaterialButton endShiftBtn;
    ProgressBar crowdProgressBar;

    // ─── Bus data ────────────────────────────────────────────────────────────
    int totalPassengers = 0;
    int totalBoarded    = 0;
    int totalAlighted   = 0;
    int ticketsIssued   = 0;
    int totalRevenue    = 0;
    int lastTicketPrice = 0;

    String busNumber, route, conductorId, numberPlate, busKey;

    // ─── GPS ─────────────────────────────────────────────────────────────────
    FusedLocationProviderClient fusedLocationClient;
    LocationCallback locationCallback;
    double currentLat     = 13.0827;
    double currentLng     = 80.2707;
    double currentSpeed   = 0.0;
    double currentBearing = 0.0;
    boolean gpsActive     = false;
    private static final int LOCATION_PERMISSION_REQUEST = 1;

    // ─── Route data ──────────────────────────────────────────────────────────
    // 99A: Tambaram → Sholinganallur
    static final String[] STOPS_99A = {
            "Tambaram Railway Station", "Convent Stop", "Mahalakshmi Nagar",
            "SIVET College", "Pallavan Nagar", "Perumbakkam",
            "Medavakkam", "Sholinganallur"
    };
    static final int[] PRICES_99A = {5, 8, 10, 13, 16, 20, 25, 30};

    // 119: Guindy → Sholinganallur
    static final String[] STOPS_119 = {
            "Concorde", "Jn. of Race Course Rd", "Vijaya Nagar",
            "IRT Road Jn.", "Kandanchavadi", "Thorappakkam",
            "Karapakkam", "Sholinganallur"
    };
    static final int[] PRICES_119 = {5, 8, 11, 14, 17, 20, 23, 27};

    String[] destinations;
    int[]    basePrices;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load saved prefs
        SharedPreferences prefs = getSharedPreferences(AppConfig.PREFS_NAME, MODE_PRIVATE);
        busNumber   = prefs.getString(AppConfig.KEY_BUS,       "");
        route       = prefs.getString(AppConfig.KEY_ROUTE,     "");
        conductorId = prefs.getString(AppConfig.KEY_CONDUCTOR, "");
        numberPlate = prefs.getString(AppConfig.KEY_PLATE,     "");
        String token = prefs.getString(AppConfig.KEY_TOKEN,     "");

        // Validate — if missing, send back to login
        if (busNumber.isEmpty() || conductorId.isEmpty() || token.isEmpty()) {
            logout();
            return;
        }

        ApiHelper.authToken = token;
        busKey = "bus_" + busNumber + "_" + conductorId;

        // Route data
        if (busNumber.equals("99A")) {
            destinations = STOPS_99A;
            basePrices   = PRICES_99A;
        } else {
            destinations = STOPS_119;
            basePrices   = PRICES_119;
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Bind views
        busNumberText      = findViewById(R.id.busNumberText);
        routeText          = findViewById(R.id.routeText);
        conductorIdText    = findViewById(R.id.conductorIdText);
        shiftTimeText      = findViewById(R.id.shiftTimeText);
        numberPlateText    = findViewById(R.id.numberPlateText);
        crowdStatusText    = findViewById(R.id.crowdStatusText);
        crowdPercentText   = findViewById(R.id.crowdPercentText);
        passengerCountText = findViewById(R.id.passengerCountText);
        seatsText          = findViewById(R.id.seatsText);
        standingText       = findViewById(R.id.standingText);
        revenueText        = findViewById(R.id.revenueText);
        revenueSubText     = findViewById(R.id.revenueSubText);
        totalTicketsText   = findViewById(R.id.totalTicketsText);
        totalBoardedText   = findViewById(R.id.totalBoardedText);
        totalAlightedText  = findViewById(R.id.totalAlightedText);
        gpsButtonText      = findViewById(R.id.gpsButtonText);
        gpsCoordText       = findViewById(R.id.gpsCoordText);
        activityFeed       = findViewById(R.id.activityFeed);
        issueTicketBtn     = findViewById(R.id.issueTicketBtn);
        alightBtn          = findViewById(R.id.alightBtn);
        gpsCard            = findViewById(R.id.gpsCard);
        endShiftBtn        = findViewById(R.id.endShiftBtn);
        crowdProgressBar   = findViewById(R.id.crowdProgressBar);

        // Set static info
        busNumberText.setText("Bus " + busNumber);
        routeText.setText(route);
        conductorIdText.setText(conductorId);
        numberPlateText.setText(numberPlate);
        shiftTimeText.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));

        // Buttons
        issueTicketBtn.setOnClickListener(v -> showTicketDialog());
        alightBtn.setOnClickListener(v -> showAlightDialog());
        gpsCard.setOnClickListener(v -> toggleGps());
        endShiftBtn.setOnClickListener(v -> confirmEndShift());

        updateUI();
        updateFirebase();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ISSUE TICKET DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showTicketDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_issue_ticket, null);

        TextView plate = dialogView.findViewById(R.id.dialogNumberPlate);
        plate.setText(numberPlate);

        android.widget.Spinner spinner = dialogView.findViewById(R.id.destinationSpinner);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, destinations);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.passengerTypeGroup);
        TextView priceText = dialogView.findViewById(R.id.priceText);

        final int[]    finalPrice = {basePrices[0]};
        final int[]    finalBase  = {basePrices[0]};
        final String[] finalType  = {"Adult"};
        final String[] finalDest  = {destinations[0]};

        Runnable updatePrice = () -> {
            int id = radioGroup.getCheckedRadioButtonId();
            if (id == R.id.radioAdult) {
                finalPrice[0] = finalBase[0];
                finalType[0]  = "Adult";
            } else if (id == R.id.radioChild) {
                finalPrice[0] = Math.max(1, finalBase[0] / 2);
                finalType[0]  = "Child";
            } else if (id == R.id.radioSenior) {
                finalPrice[0] = Math.max(1, finalBase[0] / 2);
                finalType[0]  = "Senior";
            } else if (id == R.id.radioDisabled) {
                finalPrice[0] = 0;
                finalType[0]  = "Differently Abled";
            }
            priceText.setText("Rs. " + finalPrice[0]);
        };

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                finalBase[0] = basePrices[pos];
                finalDest[0] = destinations[pos];
                updatePrice.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });
        radioGroup.setOnCheckedChangeListener((g, id) -> updatePrice.run());

        new AlertDialog.Builder(this)
                .setTitle("Issue Ticket")
                .setView(dialogView)
                .setPositiveButton("Issue Ticket", (dialog, which) -> {
                    ticketsIssued++;
                    totalPassengers++;
                    totalBoarded++;
                    totalRevenue    += finalPrice[0];
                    lastTicketPrice  = finalPrice[0];

                    // Save stop count for alight prediction
                    String stopKey = finalDest[0].replace(" ", "_").replace(".", "");
                    DatabaseReference stopRef = FirebaseDatabase.getInstance()
                            .getReference("buses")
                            .child(busKey)
                            .child("stopCounts")
                            .child(stopKey);

                    stopRef.get().addOnSuccessListener(snapshot -> {
                        long current = snapshot.exists() ? snapshot.getValue(Long.class) : 0L;
                        stopRef.setValue(current + 1);
                    });

                    addActivityItem("🎫", finalType[0] + " → " + finalDest[0],
                            finalPrice[0] > 0 ? "Rs." + finalPrice[0] : "Free");
                    updateUI();
                    updateFirebase();
                    Toast.makeText(this,
                            "✅ " + finalType[0] + " to " + finalDest[0] + " — Rs." + finalPrice[0],
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALIGHT DIALOG (NEW — fixes totalAlighted always being 0)
    // ─────────────────────────────────────────────────────────────────────────
    private void showAlightDialog() {
        View dialogView = getLayoutInflater().inflate(
                android.R.layout.select_dialog_item, null);

        EditText input = new EditText(this);
        input.setHint("Number of passengers alighted");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        input.setTextColor(0xFFFFFFFF);
        input.setHintTextColor(0xFF666666);
        input.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
                .setTitle("🚪 Record Alighting")
                .setMessage("How many passengers alighted at this stop?")
                .setView(input)
                .setPositiveButton("Confirm", (dialog, which) -> {
                    String val = input.getText().toString().trim();
                    if (val.isEmpty()) return;
                    int count = Integer.parseInt(val);
                    if (count <= 0) return;

                    // Decrement count — but never below 0
                    totalAlighted   += count;
                    totalPassengers  = Math.max(0, totalPassengers - count);

                    addActivityItem("🚪", count + " passengers alighted", "-" + count);
                    updateUI();
                    updateFirebase();
                    Toast.makeText(this, count + " passengers alighted recorded",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GPS
    // ─────────────────────────────────────────────────────────────────────────
    private void toggleGps() {
        if (!gpsActive) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST);
                return;
            }
            startGps();
        } else {
            stopGps();
        }
    }

    private void startGps() {
        gpsActive = true;
        gpsButtonText.setText("GPS Active");
        gpsButtonText.setTextColor(0xFF10B981);
        gpsCard.setBackgroundTintList(null);
        findViewById(R.id.gpsButtonText).setBackground(
                getDrawable(R.drawable.gps_btn_bg_active));

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) return;
                Location loc = result.getLastLocation();
                if (loc != null) {
                    currentLat     = loc.getLatitude();
                    currentLng     = loc.getLongitude();
                    currentSpeed   = loc.getSpeed() * 3.6; // m/s → km/h
                    currentBearing = loc.getBearing();
                    gpsCoordText.setText(String.format(Locale.getDefault(),
                            "%.4f° N,  %.4f° E  •  %.1f km/h",
                            currentLat, currentLng, currentSpeed));
                    updateFirebase();
                    addActivityItem("📍", "Location updated", "Live");
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // FIX: Pass Looper.getMainLooper() — not null
            fusedLocationClient.requestLocationUpdates(
                    locationRequest, locationCallback, Looper.getMainLooper());
            addActivityItem("📍", "GPS tracking started", "🟢 Live");
            Toast.makeText(this, "GPS tracking started!", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopGps() {
        gpsActive = false;
        gpsButtonText.setText("Start GPS");
        gpsButtonText.setTextColor(0xFF4F8EF7);
        gpsCoordText.setText("Tap to start GPS");
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE UI
    // ─────────────────────────────────────────────────────────────────────────
    private void updateUI() {
        int seats    = AppConfig.TOTAL_SEATS;
        int free     = Math.max(0, seats - totalPassengers);
        int standing = Math.max(0, totalPassengers - seats);
        // Cap percent at 100 for display only; raw count is unlimited
        int percent  = Math.min(100, (int)((totalPassengers / (float) seats) * 100));

        passengerCountText.setText(String.valueOf(totalPassengers));

        if (standing > 0) {
            seatsText.setText("0 seats • " + standing + " standing");
            seatsText.setTextColor(0xFFEF4444);
            if (standingText != null) {
                standingText.setText("⚠ " + standing + " standing");
                standingText.setVisibility(View.VISIBLE);
            }
        } else {
            seatsText.setText(free + " seats free");
            seatsText.setTextColor(0xFF10B981);
            if (standingText != null) standingText.setVisibility(View.GONE);
        }

        totalTicketsText.setText(String.valueOf(ticketsIssued));
        totalBoardedText.setText("↑" + totalBoarded);
        totalAlightedText.setText("↓" + totalAlighted);
        revenueText.setText("Rs. " + totalRevenue);
        revenueSubText.setText(ticketsIssued + " tickets today");

        crowdPercentText.setText(percent + "% occupied");
        crowdProgressBar.setProgress(percent);

        int color;
        String statusLabel;
        if (percent < AppConfig.CROWD_FREE_THRESHOLD) {
            color       = 0xFF10B981;
            statusLabel = "FREE";
        } else if (percent < AppConfig.CROWD_CROWDED_THRESHOLD) {
            color       = 0xFFF59E0B;
            statusLabel = "MODERATE";
        } else {
            color       = 0xFFEF4444;
            statusLabel = "CROWDED";
        }

        crowdStatusText.setText(statusLabel);
        crowdStatusText.setTextColor(color);
        crowdPercentText.setTextColor(color);
        crowdProgressBar.setProgressTintList(
                android.content.res.ColorStateList.valueOf(color));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE FIREBASE + BACKEND
    // ─────────────────────────────────────────────────────────────────────────
    private void updateFirebase() {
        int percent = Math.min(100, (int)((totalPassengers / (float) AppConfig.TOTAL_SEATS) * 100));

        String crowdStatus;
        if      (percent < AppConfig.CROWD_FREE_THRESHOLD)      crowdStatus = "FREE";
        else if (percent < AppConfig.CROWD_CROWDED_THRESHOLD)   crowdStatus = "MODERATE";
        else                                                     crowdStatus = "CROWDED";

        // Firebase (backup)
        Map<String, Object> info = new HashMap<>();
        info.put("busNumber",   busNumber);
        info.put("conductorId", conductorId);
        info.put("numberPlate", numberPlate);
        info.put("route",       route);
        info.put("status",      "online");

        Map<String, Object> live = new HashMap<>();
        live.put("totalPassengers",  totalPassengers);
        live.put("totalSeats",       AppConfig.TOTAL_SEATS);
        live.put("occupancyPercent", percent);
        live.put("totalBoarded",     totalBoarded);
        live.put("totalAlighted",    totalAlighted);
        live.put("ticketsIssued",    ticketsIssued);
        live.put("totalRevenue",     totalRevenue);
        live.put("lastTicketPrice",  lastTicketPrice);
        live.put("crowdStatus",      crowdStatus);
        live.put("lastUpdated",      System.currentTimeMillis());

        Map<String, Object> location = new HashMap<>();
        location.put("latitude",  currentLat);
        location.put("longitude", currentLng);
        location.put("speed",     currentSpeed);
        location.put("bearing",   currentBearing);

        DatabaseReference busRef = FirebaseDatabase.getInstance()
                .getReference("buses").child(busKey);
        busRef.child("info").updateChildren(info);
        busRef.child("live").updateChildren(live);
        busRef.child("location").updateChildren(location);

        // Node.js backend (primary)
        sendToBackend(crowdStatus, percent);
    }

    private void sendToBackend(String crowdStatus, int occupancyPercent) {
        try {
            JSONObject body = new JSONObject();
            body.put("busNumber",        busNumber);
            body.put("conductorId",      conductorId);
            body.put("numberPlate",      numberPlate);
            body.put("route",            route);
            body.put("totalPassengers",  totalPassengers);
            body.put("totalSeats",       AppConfig.TOTAL_SEATS);
            body.put("occupancyPercent", occupancyPercent);
            body.put("crowdStatus",      crowdStatus);
            body.put("totalRevenue",     totalRevenue);
            body.put("ticketsIssued",    ticketsIssued);
            body.put("latitude",         currentLat);
            body.put("longitude",        currentLng);
            body.put("speed",            currentSpeed);

            ApiHelper.post(AppConfig.BUS_UPDATE_URL, body, new ApiHelper.ApiCallback() {
                @Override public void onSuccess(JSONObject response) {}
                @Override public void onError(String error) {}   // silent — Firebase is backup
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIVITY FEED
    // ─────────────────────────────────────────────────────────────────────────
    private void addActivityItem(String icon, String text, String detail) {
        // Remove placeholder text on first real item
        if (activityFeed.getChildCount() == 1
                && activityFeed.getChildAt(0) instanceof TextView) {
            activityFeed.removeAllViews();
        }

        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.HORIZONTAL);
        item.setPadding(0, dpToPx(10), 0, dpToPx(10));
        item.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView iconView = new TextView(this);
        iconView.setText(icon);
        iconView.setTextSize(18);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(36), dpToPx(36));
        iconParams.setMarginEnd(dpToPx(12));
        iconView.setLayoutParams(iconParams);
        iconView.setGravity(android.view.Gravity.CENTER);

        LinearLayout textContainer = new LinearLayout(this);
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView mainText = new TextView(this);
        mainText.setText(text);
        mainText.setTextColor(0xFFE2E8F0);
        mainText.setTextSize(13);

        TextView timeText = new TextView(this);
        timeText.setText(new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date()));
        timeText.setTextColor(0xFF64748B);
        timeText.setTextSize(11);

        textContainer.addView(mainText);
        textContainer.addView(timeText);

        TextView detailView = new TextView(this);
        detailView.setText(detail);
        detailView.setTextColor(0xFF4F8EF7);
        detailView.setTextSize(13);
        detailView.setTypeface(null, android.graphics.Typeface.BOLD);

        item.addView(iconView);
        item.addView(textContainer);
        item.addView(detailView);

        View divider = new View(this);
        divider.setBackgroundColor(0xFF1E293B);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        activityFeed.addView(divider, 0);
        activityFeed.addView(item, 0);

        // Keep max 8 items
        while (activityFeed.getChildCount() > 16) {
            activityFeed.removeViewAt(activityFeed.getChildCount() - 1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // END SHIFT
    // ─────────────────────────────────────────────────────────────────────────
    private void confirmEndShift() {
        new AlertDialog.Builder(this)
                .setTitle("End Shift?")
                .setMessage("Revenue: Rs." + totalRevenue + "\nTickets: " + ticketsIssued
                        + "\nBoarded: " + totalBoarded + " | Alighted: " + totalAlighted)
                .setPositiveButton("End Shift", (dialog, which) -> {
                    stopGps();

                    // Set Firebase status to offline
                    FirebaseDatabase.getInstance()
                            .getReference("buses")
                            .child(busKey)
                            .child("info")
                            .child("status")
                            .setValue("offline");

                    // Notify backend
                    try {
                        JSONObject body = new JSONObject();
                        body.put("conductorId", conductorId);
                        body.put("busNumber",   busNumber);
                        ApiHelper.post(AppConfig.END_SHIFT_URL, body, new ApiHelper.ApiCallback() {
                            @Override public void onSuccess(JSONObject r) { runOnUiThread(() -> logout()); }
                            @Override public void onError(String error)   { runOnUiThread(() -> logout()); }
                        });
                    } catch (Exception e) {
                        logout();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void logout() {
        getSharedPreferences(AppConfig.PREFS_NAME, MODE_PRIVATE).edit().clear().apply();
        ApiHelper.authToken = "";
        Toast.makeText(this, "Shift ended! Great job! 🚍", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PERMISSIONS
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startGps();
        } else {
            Toast.makeText(this, "Location permission needed for GPS tracking",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopGps();
    }

    private int dpToPx(int dp) {
        return (int)(dp * getResources().getDisplayMetrics().density);
    }
}