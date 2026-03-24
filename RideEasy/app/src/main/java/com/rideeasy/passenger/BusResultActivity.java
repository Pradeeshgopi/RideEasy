package com.rideeasy.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;

public class BusResultActivity extends AppCompatActivity {

    String busNumber, conductorId, numberPlate, route;
    int passengers;

    // Firebase — stored for cleanup
    DatabaseReference busRef;
    ValueEventListener busListener;

    // UI
    TextView busNumText, routeText, plateText, passengerText, seatsText, crowdText, percentText;
    TextView liveLabel;
    ProgressBar crowdBar;
    LinearLayout stopContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        busNumber   = getIntent().getStringExtra("busNumber");
        conductorId = getIntent().getStringExtra("conductorId");
        numberPlate = getIntent().getStringExtra("numberPlate");
        route       = getIntent().getStringExtra("route");
        passengers  = getIntent().getIntExtra("passengers", 0);

        buildLayout();
        attachLiveListener();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LAYOUT
    // ─────────────────────────────────────────────────────────────────────────
    private void buildLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF07090F);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(48), dp(16), dp(32));
        scroll.addView(root);

        // Back
        TextView backBtn = new TextView(this);
        backBtn.setText("← Back");
        backBtn.setTextColor(0xFF4F8EF7);
        backBtn.setTextSize(15);
        backBtn.setPadding(0, 0, 0, dp(16));
        backBtn.setOnClickListener(v -> finish());
        root.addView(backBtn);

        // ── Header card ────────────────────────────────────────────────────
        CardView headerCard = makeCard();
        LinearLayout headerContent = cardContent();

        busNumText = makeText("Bus " + busNumber, 0xFFFFFFFF, 24, true);
        routeText  = makeText(route, 0xFF94A3B8, 13, false);
        plateText  = makeText(numberPlate, 0xFF4F8EF7, 13, true);
        liveLabel  = makeText("🔴 Connecting live...", 0xFF94A3B8, 12, false);

        headerContent.addView(busNumText);
        headerContent.addView(routeText);
        headerContent.addView(plateText);
        headerContent.addView(liveLabel);
        headerCard.addView(headerContent);
        root.addView(headerCard);
        addMargin(root, 12);

        // ── Occupancy card ─────────────────────────────────────────────────
        CardView occCard = makeCard();
        LinearLayout occContent = cardContent();

        sectionLabel("👥  LIVE OCCUPANCY", occContent);

        LinearLayout occupancyRow = new LinearLayout(this);
        occupancyRow.setOrientation(LinearLayout.HORIZONTAL);
        occupancyRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams mrp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mrp.setMargins(0, dp(12), 0, 0);
        occupancyRow.setLayoutParams(mrp);

        passengerText = makeText("0", 0xFFFFFFFF, 48, true);
        passengerText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout passSub = new LinearLayout(this);
        passSub.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams psParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        psParams.setMarginStart(dp(12));
        passSub.setLayoutParams(psParams);

        passSub.addView(makeText("passengers", 0xFF94A3B8, 13, false));
        seatsText = makeText("44 seats free", 0xFF10B981, 13, true);
        passSub.addView(seatsText);

        crowdText = makeText("FREE", 0xFF10B981, 13, true);
        crowdText.setBackground(chip(0xFF10B981));
        crowdText.setPadding(dp(14), dp(8), dp(14), dp(8));

        occupancyRow.addView(passengerText);
        occupancyRow.addView(passSub);
        occupancyRow.addView(crowdText);
        occContent.addView(occupancyRow);

        crowdBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        crowdBar.setMax(100);
        crowdBar.setProgress(0);
        crowdBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xFF10B981));
        crowdBar.setProgressBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF1E2940));
        LinearLayout.LayoutParams pbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(10));
        pbp.setMargins(0, dp(16), 0, dp(8));
        crowdBar.setLayoutParams(pbp);
        occContent.addView(crowdBar);

        percentText = makeText("0% occupied", 0xFF10B981, 13, true);
        occContent.addView(percentText);

        occCard.addView(occContent);
        root.addView(occCard);
        addMargin(root, 12);

        // ── Alight prediction card ─────────────────────────────────────────
        CardView stopCard = makeCard();
        LinearLayout stopContent = cardContent();
        sectionLabel("🛑  ALIGHT PREDICTION (by ticket data)", stopContent);
        stopContainer = new LinearLayout(this);
        stopContainer.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams scp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        scp.setMargins(0, dp(10), 0, 0);
        stopContainer.setLayoutParams(scp);
        stopContent.addView(stopContainer);
        stopCard.addView(stopContent);
        root.addView(stopCard);
        addMargin(root, 12);

        // ── Book button ────────────────────────────────────────────────────
        com.google.android.material.button.MaterialButton bookBtn =
                new com.google.android.material.button.MaterialButton(this);
        bookBtn.setText("🎫  Book a Ticket on This Bus");
        bookBtn.setTextColor(0xFFFFFFFF);
        bookBtn.setTextSize(15);
        bookBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        bookBtn.setLayoutParams(bbp);
        bookBtn.setBackgroundColor(0xFF4F8EF7);
        bookBtn.setCornerRadius(dp(14));
        bookBtn.setOnClickListener(v -> {
            Intent i = new Intent(this, BookingActivity.class);
            i.putExtra("busNumber",   busNumber);
            i.putExtra("conductorId", conductorId);
            i.putExtra("numberPlate", numberPlate);
            i.putExtra("route",       route);
            startActivity(i);
        });
        root.addView(bookBtn);

        setContentView(scroll);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LIVE FIREBASE LISTENER — stored and removed in onStop
    // ─────────────────────────────────────────────────────────────────────────
    private void attachLiveListener() {
        String busKey = "bus_" + busNumber + "_" + conductorId;
        busRef = FirebaseDatabase.getInstance().getReference("buses").child(busKey);

        busListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Long pax = snapshot.child("live/totalPassengers").getValue(Long.class);
                Long rev = snapshot.child("live/totalRevenue").getValue(Long.class);
                String crowd = snapshot.child("live/crowdStatus").getValue(String.class);

                int p = pax != null ? pax.intValue() : passengers;
                updateOccupancyUI(p, crowd != null ? crowd : "FREE");

                // Load stop counts from same snapshot
                loadStopCounts(snapshot);

                if (liveLabel != null) liveLabel.setText("🟢 Live data connected");
            }

            @Override
            public void onCancelled(DatabaseError error) {
                if (liveLabel != null) liveLabel.setText("⚠ Live data unavailable");
            }
        };
        busRef.addValueEventListener(busListener);
    }

    private void updateOccupancyUI(int pax, String crowd) {
        int seats    = AppConfig.TOTAL_SEATS;
        int free     = Math.max(0, seats - pax);
        int standing = Math.max(0, pax - seats);
        int percent  = Math.min(100, (int)((pax / (float) seats) * 100));

        passengerText.setText(String.valueOf(pax));
        if (standing > 0) {
            seatsText.setText("0 seats • " + standing + " standing");
            seatsText.setTextColor(0xFFEF4444);
        } else {
            seatsText.setText(free + " seats free");
            seatsText.setTextColor(0xFF10B981);
        }

        crowdBar.setProgress(percent);
        percentText.setText(percent + "% occupied");

        int color;
        if ("CROWDED".equals(crowd))       color = 0xFFEF4444;
        else if ("MODERATE".equals(crowd)) color = 0xFFF59E0B;
        else                               color = 0xFF10B981;

        crowdText.setText(crowd);
        crowdText.setTextColor(color);
        crowdText.setBackground(chip(color));
        percentText.setTextColor(color);
        crowdBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STOP COUNTS — loaded from the same Firebase snapshot
    // ─────────────────────────────────────────────────────────────────────────
    private void loadStopCounts(DataSnapshot snapshot) {
        stopContainer.removeAllViews();
        DataSnapshot stopCountsSnap = snapshot.child("stopCounts");
        Map<String, Long> counts = new HashMap<>();
        for (DataSnapshot child : stopCountsSnap.getChildren()) {
            Long val = child.getValue(Long.class);
            if (val != null) counts.put(child.getKey().replace("_", " "), val);
        }

        if (counts.isEmpty()) {
            TextView empty = makeText("No alighting data yet", 0xFF475569, 13, false);
            empty.setPadding(0, dp(8), 0, 0);
            stopContainer.addView(empty);
            return;
        }

        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rp.setMargins(0, dp(6), 0, dp(6));
            row.setLayoutParams(rp);

            TextView stop = makeText("🛑  " + entry.getKey(), 0xFFE2E8F0, 13, false);
            stop.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView count = makeText(entry.getValue() + " alighting", 0xFF4F8EF7, 13, true);
            row.addView(stop);
            row.addView(count);
            stopContainer.addView(row);

            View divider = new View(this);
            divider.setBackgroundColor(0xFF1E2940);
            divider.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1));
            stopContainer.addView(divider);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // FIX: Remove Firebase listener when activity is not visible
        if (busRef != null && busListener != null) {
            busRef.removeEventListener(busListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (busRef != null && busListener != null) {
            busRef.addValueEventListener(busListener);
        } else {
            attachLiveListener();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private CardView makeCard() {
        CardView cv = new CardView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cv.setLayoutParams(lp);
        cv.setCardBackgroundColor(0xFF0F1420);
        cv.setRadius(dp(20));
        cv.setCardElevation(0);
        return cv;
    }

    private LinearLayout cardContent() {
        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(20), dp(20), dp(20), dp(20));
        return ll;
    }

    private TextView makeText(String text, int color, int spSize, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(spSize);
        if (bold) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private void sectionLabel(String text, LinearLayout parent) {
        TextView tv = makeText(text, 0xFF64748B, 11, true);
        tv.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(4));
        tv.setLayoutParams(lp);
        parent.addView(tv);
    }

    private android.graphics.drawable.GradientDrawable chip(int color) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        gd.setColor(0x22000000 | (color & 0xFFFFFF));
        gd.setStroke(dp(1), color);
        gd.setCornerRadius(dp(20));
        return gd;
    }

    private void addMargin(LinearLayout parent, int dp) {
        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(dp)));
        parent.addView(spacer);
    }

    private int dp(int val) {
        return (int)(val * getResources().getDisplayMetrics().density);
    }
}