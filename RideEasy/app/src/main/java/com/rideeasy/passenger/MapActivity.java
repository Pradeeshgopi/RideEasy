package com.rideeasy.passenger;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    MapView mapView;
    GoogleMap googleMap;
    LinearLayout busPanel;

    // Firebase listener — stored for cleanup
    DatabaseReference busesRef;
    ValueEventListener busesListener;

    // Route coordinates
    // 99A stops
    static final LatLng[] COORDS_99A = {
            new LatLng(12.9277954, 80.1185422), new LatLng(12.9233649, 80.1207251),
            new LatLng(12.922638,  80.131149),  new LatLng(12.924903,  80.133712),
            new LatLng(12.929964,  80.137223),  new LatLng(12.932673,  80.135887),
            new LatLng(12.939908,  80.146262),  new LatLng(12.917222,  80.192222),
            new LatLng(12.901025,  80.227930)
    };

    // 119 stops
    static final LatLng[] COORDS_119 = {
            new LatLng(13.009716, 80.220637), new LatLng(13.011383, 80.221981),
            new LatLng(13.013214, 80.223102), new LatLng(13.015214, 80.224862),
            new LatLng(13.021532, 80.227451), new LatLng(13.060352, 80.242983),
            new LatLng(13.035235, 80.235825), new LatLng(12.991722, 80.229516),
            new LatLng(12.901025, 80.227930)
    };

    List<BusModel> liveBuses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Root layout
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF07090F);

        // Back bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setBackgroundColor(0xFF0F1420);
        topBar.setPadding(dp(16), dp(48), dp(16), dp(12));
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView back = new TextView(this);
        back.setText("← Back");
        back.setTextColor(0xFF4F8EF7);
        back.setTextSize(15);
        back.setOnClickListener(v -> finish());
        topBar.addView(back);

        TextView title = new TextView(this);
        title.setText("  Live Bus Map");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(17);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        topBar.addView(title);

        root.addView(topBar);

        // Map
        mapView = new MapView(this);
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        mapView.setLayoutParams(mapParams);
        root.addView(mapView);

        // Bus panel at bottom (scrollable horizontal)
        busPanel = new LinearLayout(this);
        busPanel.setOrientation(LinearLayout.VERTICAL);
        busPanel.setBackgroundColor(0xFF0F1420);
        busPanel.setPadding(dp(16), dp(12), dp(16), dp(16));
        LinearLayout.LayoutParams panelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
        busPanel.setLayoutParams(panelParams);

        TextView panelTitle = new TextView(this);
        panelTitle.setText("🚌  Live Buses");
        panelTitle.setTextColor(0xFF94A3B8);
        panelTitle.setTextSize(12);
        panelTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        busPanel.addView(panelTitle);

        root.addView(busPanel);
        setContentView(root);

        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Draw route polylines
        drawRoutes();

        // Centre map on Chennai
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(12.9716, 80.2209), 11));

        // Load live buses
        loadLiveBuses();
    }

    private void drawRoutes() {
        if (googleMap == null) return;

        List<LatLng> route99A = new ArrayList<>();
        for (LatLng ll : COORDS_99A) route99A.add(ll);
        googleMap.addPolyline(new PolylineOptions()
                .addAll(route99A).color(0x884F8EF7).width(6));

        List<LatLng> route119 = new ArrayList<>();
        for (LatLng ll : COORDS_119) route119.add(ll);
        googleMap.addPolyline(new PolylineOptions()
                .addAll(route119).color(0x8810B981).width(6));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FIREBASE LIVE BUSES — listener stored, cleared on panel refresh
    // ─────────────────────────────────────────────────────────────────────────
    private void loadLiveBuses() {
        if (busesRef != null && busesListener != null) {
            busesRef.removeEventListener(busesListener);
        }

        busesRef = FirebaseDatabase.getInstance().getReference("buses");
        busesListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                liveBuses.clear();
                if (googleMap != null) googleMap.clear();
                drawRoutes(); // redraw routes after clearing

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
                    Double lat = busSnap.child("location/latitude").getValue(Double.class);
                    Double lng = busSnap.child("location/longitude").getValue(Double.class);
                    model.latitude  = lat != null ? lat : 0;
                    model.longitude = lng != null ? lng : 0;
                    if (model.busNumber == null) continue;
                    liveBuses.add(model);
                }

                runOnUiThread(() -> {
                    updateMap();
                    updatePanel();
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        };
        busesRef.addValueEventListener(busesListener);
    }

    private void updateMap() {
        if (googleMap == null) return;
        for (BusModel bus : liveBuses) {
            if (bus.latitude == 0 && bus.longitude == 0) continue;
            LatLng pos = new LatLng(bus.latitude, bus.longitude);

            int pct = Math.min(100, (int)((bus.totalPassengers / (float) AppConfig.TOTAL_SEATS) * 100));
            float hue = pct < 40 ? BitmapDescriptorFactory.HUE_GREEN
                    : pct < 75 ? BitmapDescriptorFactory.HUE_YELLOW
                    : BitmapDescriptorFactory.HUE_RED;

            googleMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title("Bus " + bus.busNumber + " (" + bus.numberPlate + ")")
                    .snippet("👥 " + bus.totalPassengers + " | " + bus.crowdStatus)
                    .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        }
    }

    private void updatePanel() {
        // Remove all bus items (keep title at index 0)
        while (busPanel.getChildCount() > 1) {
            busPanel.removeViewAt(busPanel.getChildCount() - 1);
        }

        if (liveBuses.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No buses online right now");
            empty.setTextColor(0xFF475569);
            empty.setTextSize(13);
            empty.setPadding(0, dp(8), 0, 0);
            busPanel.addView(empty);
            return;
        }

        for (BusModel bus : liveBuses) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, dp(8), 0, 0);
            row.setLayoutParams(rp);

            int pct = Math.min(100, (int)((bus.totalPassengers / (float) AppConfig.TOTAL_SEATS) * 100));
            int color = pct < 40 ? 0xFF10B981 : pct < 75 ? 0xFFF59E0B : 0xFFEF4444;

            TextView plate = new TextView(this);
            plate.setText("🚌  " + bus.busNumber + " · " + bus.numberPlate);
            plate.setTextColor(0xFFE2E8F0);
            plate.setTextSize(13);
            plate.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView crowd = new TextView(this);
            crowd.setText(bus.crowdStatus + " · " + bus.totalPassengers + " pax");
            crowd.setTextColor(color);
            crowd.setTextSize(12);
            crowd.setTypeface(null, android.graphics.Typeface.BOLD);

            row.addView(plate);
            row.addView(crowd);
            busPanel.addView(row);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIFECYCLE — fix listener leaks + MapView
    // ─────────────────────────────────────────────────────────────────────────
    @Override protected void onResume()  { super.onResume();  mapView.onResume(); }
    @Override protected void onPause()   { super.onPause();   mapView.onPause(); }
    @Override protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
        if (busesRef != null && busesListener != null) {
            busesRef.removeEventListener(busesListener);
        }
    }
    @Override protected void onStop() {
        super.onStop();
        if (busesRef != null && busesListener != null) {
            busesRef.removeEventListener(busesListener);
        }
    }
    @Override protected void onStart() { super.onStart(); loadLiveBuses(); }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        mapView.onSaveInstanceState(out);
    }

    private int dp(int val) {
        return (int)(val * getResources().getDisplayMetrics().density);
    }
}