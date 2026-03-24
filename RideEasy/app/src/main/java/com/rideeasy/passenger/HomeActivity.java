package com.rideeasy.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    RecyclerView busRecyclerView;
    LinearLayout loadingLayout;
    CardView recBanner;
    TextView greetingText, recTitle, recSubtitle, refreshText;

    BusCardAdapter adapter;
    List<BusModel> busList;

    // Firebase listener stored so we can remove it in onStop
    DatabaseReference busesRef;
    ValueEventListener firebaseListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        busRecyclerView = findViewById(R.id.busRecyclerView);
        loadingLayout   = findViewById(R.id.loadingLayout);
        recBanner       = findViewById(R.id.recBanner);
        greetingText    = findViewById(R.id.greetingText);
        recTitle        = findViewById(R.id.recTitle);
        recSubtitle     = findViewById(R.id.recSubtitle);
        refreshText     = findViewById(R.id.refreshText);

        // Greeting based on time
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if      (hour < 12) greetingText.setText("Good Morning 🌤️");
        else if (hour < 18) greetingText.setText("Good Afternoon ☀️");
        else                greetingText.setText("Good Evening 🌙");

        busList = new ArrayList<>();
        adapter = new BusCardAdapter(this, busList);
        busRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        busRecyclerView.setAdapter(adapter);

        // Search box open search
        findViewById(R.id.searchBox).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        // Quick route chips
        findViewById(R.id.chip99A).setOnClickListener(v -> filterTo("99A"));
        findViewById(R.id.chip119).setOnClickListener(v -> filterTo("119"));
        // Refresh
        refreshText.setOnClickListener(v -> loadBuses());
        // Bottom nav
        findViewById(R.id.navSearch).setOnClickListener(v ->
                startActivity(new Intent(this, SearchActivity.class)));
        findViewById(R.id.navMap).setOnClickListener(v ->
                startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.navBook).setOnClickListener(v ->
                startActivity(new Intent(this, BookingActivity.class)));

        loadBuses();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LOAD: try backend first, fall back to Firebase
    // ─────────────────────────────────────────────────────────────────────────
    private void loadBuses() {
        showLoading(true);

        ApiHelper.get(AppConfig.BUSES_URL, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray buses = response.getJSONArray("buses");
                            busList.clear();
                            for (int i = 0; i < buses.length(); i++) {
                                JSONObject b   = buses.getJSONObject(i);
                                BusModel model = new BusModel();
                                model.busNumber       = b.optString("busNumber");
                                model.conductorId     = b.optString("conductorId");
                                model.numberPlate     = b.optString("numberPlate");
                                model.route           = b.optString("route");
                                model.totalPassengers = b.optInt("totalPassengers");
                                model.totalSeats      = AppConfig.TOTAL_SEATS;
                                model.crowdStatus     = b.optString("crowdStatus", "FREE");
                                model.latitude        = b.optDouble("latitude", 0);
                                model.longitude       = b.optDouble("longitude", 0);
                                model.speed           = b.optDouble("speed", 0);
                                busList.add(model);
                            }
                            adapter.notifyDataSetChanged();
                            showLoading(false);
                            updateRecommendation();
                        } else {
                            loadFromFirebase();
                        }
                    } catch (Exception e) {
                        loadFromFirebase();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> loadFromFirebase());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIREBASE FALLBACK — listener properly stored and removed in onStop
    // ─────────────────────────────────────────────────────────────────────────
    private void loadFromFirebase() {
        removeFirebaseListener(); // safety — remove any old listener
        busesRef = FirebaseDatabase.getInstance().getReference("buses");
        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                busList.clear();
                for (DataSnapshot busSnap : snapshot.getChildren()) {
                    String status = busSnap.child("info/status").getValue(String.class);
                    if (!"online".equals(status)) continue;

                    BusModel model = new BusModel();
                    model.busNumber       = busSnap.child("info/busNumber").getValue(String.class);
                    model.conductorId     = busSnap.child("info/conductorId").getValue(String.class);
                    model.numberPlate     = busSnap.child("info/numberPlate").getValue(String.class);
                    model.route           = busSnap.child("info/route").getValue(String.class);
                    model.crowdStatus     = busSnap.child("live/crowdStatus").getValue(String.class);
                    Long pax = busSnap.child("live/totalPassengers").getValue(Long.class);
                    model.totalPassengers = pax != null ? pax.intValue() : 0;
                    model.totalSeats      = AppConfig.TOTAL_SEATS;
                    if (model.busNumber == null) continue;
                    busList.add(model);
                }
                adapter.notifyDataSetChanged();
                showLoading(false);
                updateRecommendation();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(HomeActivity.this,
                            "Could not load buses. Check network.", Toast.LENGTH_SHORT).show();
                });
            }
        };
        busesRef.addValueEventListener(firebaseListener);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // RECOMMENDATION BANNER
    // ─────────────────────────────────────────────────────────────────────────
    private void updateRecommendation() {
        if (busList.isEmpty()) { recBanner.setVisibility(View.GONE); return; }

        // Find least crowded bus
        BusModel best = busList.get(0);
        for (BusModel b : busList) {
            if (b.totalPassengers < best.totalPassengers) best = b;
        }

        int pct = Math.min(100, (int)((best.totalPassengers / (float) AppConfig.TOTAL_SEATS) * 100));
        String tip;
        if      (pct < 40)  tip = "✅ " + best.busNumber + " is free right now — great time to board!";
        else if (pct < 75)  tip = "🟡 " + best.busNumber + " is moderately filled — acceptable.";
        else                tip = "🔴 All buses are crowded. Consider waiting.";

        recTitle.setText(tip);
        recSubtitle.setText("👥 " + best.totalPassengers + " on board • " + pct + "% full");
        recBanner.setVisibility(View.VISIBLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FILTER by bus number (quick chip)
    // ─────────────────────────────────────────────────────────────────────────
    private void filterTo(String busNum) {
        Intent i = new Intent(this, SearchActivity.class);
        i.putExtra("query", busNum);
        startActivity(i);
    }

    private void showLoading(boolean show) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        busRecyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void removeFirebaseListener() {
        if (busesRef != null && firebaseListener != null) {
            busesRef.removeEventListener(firebaseListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // FIX: Always remove listener when activity is not visible
        removeFirebaseListener();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadBuses();
    }
}