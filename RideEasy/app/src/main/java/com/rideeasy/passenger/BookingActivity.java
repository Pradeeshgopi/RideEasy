package com.rideeasy.passenger;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.android.material.button.MaterialButton;
import org.json.JSONObject;
import java.util.UUID;

public class BookingActivity extends AppCompatActivity {

    String busNumber, conductorId, numberPlate, route;
    String[] stops;
    int[]    fares;

    // Selections
    int fromIndex = 0;
    int toIndex   = 1;
    String passengerType = "Adult";
    int    calculatedFare = 0;

    TextView fareDisplay, summaryText;
    Spinner  fromSpinner, toSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        busNumber   = getIntent().getStringExtra("busNumber");
        conductorId = getIntent().getStringExtra("conductorId");
        numberPlate = getIntent().getStringExtra("numberPlate");
        route       = getIntent().getStringExtra("route");

        // Select stops & fares by bus
        if ("99A".equals(busNumber)) {
            stops = AppConfig.STOPS_99A;
            fares = AppConfig.FARES_99A;
        } else {
            stops = AppConfig.STOPS_119;
            fares = AppConfig.FARES_119;
        }

        buildUI();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI
    // ─────────────────────────────────────────────────────────────────────────
    private void buildUI() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFF07090F);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(48), dp(16), dp(32));
        scroll.addView(root);

        // Back + title
        TextView backBtn = new TextView(this);
        backBtn.setText("← Back");
        backBtn.setTextColor(0xFF4F8EF7);
        backBtn.setTextSize(15);
        backBtn.setPadding(0, 0, 0, dp(4));
        backBtn.setOnClickListener(v -> finish());
        root.addView(backBtn);

        // Title
        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.VERTICAL);
        titleRow.setPadding(0, 0, 0, dp(20));
        titleRow.addView(titleText("Book a Ticket", 0xFFFFFFFF, 22));
        titleRow.addView(titleText("Bus " + busNumber + " • " + numberPlate, 0xFF4F8EF7, 13));
        if (route != null) titleRow.addView(titleText(route, 0xFF94A3B8, 12));
        root.addView(titleRow);

        // ── Journey card ──────────────────────────────────────────────────
        root.addView(sectionCard("📍  SELECT JOURNEY", () -> {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);

            card.addView(label("From Stop"));
            fromSpinner = makeSpinner(stops, 0);
            card.addView(fromSpinner);

            spacer(card, 12);
            card.addView(label("To Stop"));
            toSpinner = makeSpinner(stops, 1);
            card.addView(toSpinner);

            // Stop change listeners
            fromSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    fromIndex = pos;
                    recalcFare();
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
            toSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                    toIndex = pos;
                    recalcFare();
                }
                @Override public void onNothingSelected(AdapterView<?> p) {}
            });
            return card;
        }));

        spacer(root, 12);

        // ── Passenger type card ───────────────────────────────────────────
        root.addView(sectionCard("👤  PASSENGER TYPE", () -> {
            RadioGroup rg = new RadioGroup(this);
            rg.setOrientation(RadioGroup.VERTICAL);

            String[][] types = {
                    {"Adult",             "Full fare"},
                    {"Child",             "Half fare"},
                    {"Senior Citizen",    "Half fare"},
                    {"Differently Abled", "Free"}
            };
            for (String[] t : types) {
                RadioButton rb = new RadioButton(this);
                rb.setText(t[0] + " — " + t[1]);
                rb.setTextColor(0xFFE2E8F0);
                rb.setTextSize(14);
                if ("Adult".equals(t[0])) rb.setChecked(true);
                rg.addView(rb);
            }
            rg.setOnCheckedChangeListener((g, id) -> {
                RadioButton rb = g.findViewById(id);
                if (rb != null) {
                    String full = rb.getText().toString();
                    passengerType = full.contains("Adult")   ? "Adult"
                            : full.contains("Child")         ? "Child"
                            : full.contains("Senior")        ? "Senior Citizen"
                            : "Differently Abled";
                    recalcFare();
                }
            });
            return rg;
        }));

        spacer(root, 12);

        // ── Fare preview card ─────────────────────────────────────────────
        CardView fareCard = new CardView(this);
        fareCard.setCardBackgroundColor(0xFF0A1C3A);
        fareCard.setRadius(dp(16));
        fareCard.setCardElevation(0);
        LinearLayout fareContent = new LinearLayout(this);
        fareContent.setOrientation(LinearLayout.VERTICAL);
        fareContent.setGravity(android.view.Gravity.CENTER);
        fareContent.setPadding(dp(20), dp(20), dp(20), dp(20));

        summaryText = new TextView(this);
        summaryText.setText("Stop 0 → Stop 1");
        summaryText.setTextColor(0xFF94A3B8);
        summaryText.setTextSize(13);
        summaryText.setGravity(android.view.Gravity.CENTER);
        fareContent.addView(summaryText);

        fareDisplay = new TextView(this);
        fareDisplay.setText("Rs. 5");
        fareDisplay.setTextColor(0xFF4F8EF7);
        fareDisplay.setTextSize(36);
        fareDisplay.setTypeface(null, android.graphics.Typeface.BOLD);
        fareDisplay.setGravity(android.view.Gravity.CENTER);
        fareContent.addView(fareDisplay);

        fareCard.addView(fareContent);
        root.addView(fareCard);

        spacer(root, 16);

        // ── Confirm button ─────────────────────────────────────────────────
        MaterialButton bookBtn = new MaterialButton(this);
        bookBtn.setText("Confirm Booking");
        bookBtn.setTextColor(0xFFFFFFFF);
        bookBtn.setTextSize(16);
        bookBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams bbp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        bookBtn.setLayoutParams(bbp);
        bookBtn.setBackgroundColor(0xFF4F8EF7);
        bookBtn.setCornerRadius(dp(14));
        bookBtn.setOnClickListener(v -> confirmBooking(bookBtn));
        root.addView(bookBtn);

        setContentView(scroll);

        // Initial fare calc
        recalcFare();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // FARE CALCULATION
    // ─────────────────────────────────────────────────────────────────────────
    private void recalcFare() {
        if (stops == null || fares == null) return;

        // Must pick different stops, to must be after from
        if (toIndex <= fromIndex) {
            toIndex = fromIndex + 1;
            if (toSpinner != null && toIndex < stops.length) {
                toSpinner.setSelection(toIndex);
            }
        }

        int baseFrom = fromIndex < fares.length ? fares[fromIndex] : 0;
        int baseTo   = toIndex   < fares.length ? fares[toIndex]   : 0;
        int baseFare = Math.max(5, baseTo - baseFrom);

        switch (passengerType) {
            case "Child":
            case "Senior Citizen":
                calculatedFare = Math.max(1, baseFare / 2);
                break;
            case "Differently Abled":
                calculatedFare = 0;
                break;
            default: // Adult
                calculatedFare = baseFare;
        }

        String fromStop = fromIndex < stops.length ? stops[fromIndex] : "";
        String toStop   = toIndex   < stops.length ? stops[toIndex]   : "";

        if (fareDisplay != null) {
            fareDisplay.setText(calculatedFare == 0 ? "Free" : "Rs. " + calculatedFare);
        }
        if (summaryText != null) {
            summaryText.setText(fromStop + " → " + toStop
                    + "  •  " + passengerType);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIRM BOOKING → POST to backend
    // ─────────────────────────────────────────────────────────────────────────
    private void confirmBooking(MaterialButton btn) {
        if (toIndex <= fromIndex) {
            Toast.makeText(this, "Please select a valid destination (after your boarding stop)",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String fromStop = stops[fromIndex];
        String toStop   = stops[toIndex];

        btn.setEnabled(false);
        btn.setText("Booking...");

        try {
            JSONObject body = new JSONObject();
            body.put("bookingId",     UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            body.put("busNumber",     busNumber);
            body.put("conductorId",   conductorId);
            body.put("fromStop",      fromStop);
            body.put("toStop",        toStop);
            body.put("passengerCount", 1);
            body.put("passengerType", passengerType);
            body.put("fare",          calculatedFare);

            ApiHelper.post(AppConfig.BOOK_URL, body, new ApiHelper.ApiCallback() {
                @Override
                public void onSuccess(JSONObject response) {
                    runOnUiThread(() -> {
                        try {
                            if (response.getBoolean("success")) {
                                Toast.makeText(BookingActivity.this,
                                        "✅ Ticket booked!\n" + fromStop + " → " + toStop
                                                + " (Rs." + calculatedFare + ")",
                                        Toast.LENGTH_LONG).show();
                                finish();
                            } else {
                                btn.setEnabled(true);
                                btn.setText("Confirm Booking");
                                Toast.makeText(BookingActivity.this,
                                        "Booking failed: " + response.optString("message"),
                                        Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            resetBtn(btn);
                        }
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        resetBtn(btn);
                        Toast.makeText(BookingActivity.this,
                                "❌ Cannot reach server. Check AppConfig IP.",
                                Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (Exception e) {
            resetBtn(btn);
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void resetBtn(MaterialButton btn) {
        btn.setEnabled(true);
        btn.setText("Confirm Booking");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UI HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    interface CardBuilder { View build(); }

    private CardView sectionCard(String title, CardBuilder builder) {
        CardView cv = new CardView(this);
        cv.setCardBackgroundColor(0xFF0F1420);
        cv.setRadius(dp(16));
        cv.setCardElevation(0);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(0xFF64748B);
        tv.setTextSize(11);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(14));
        tv.setLayoutParams(lp);
        ll.addView(tv);

        View built = builder.build();
        ll.addView(built);
        cv.addView(ll);
        return cv;
    }

    private Spinner makeSpinner(String[] items, int defaultSel) {
        Spinner sp = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, items);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sp.setAdapter(adapter);
        if (defaultSel < items.length) sp.setSelection(defaultSel);
        sp.setBackgroundColor(0xFF19213A);
        sp.setPadding(dp(12), dp(12), dp(12), dp(12));
        return sp;
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFF94A3B8);
        tv.setTextSize(13);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(6));
        tv.setLayoutParams(lp);
        return tv;
    }

    private TextView titleText(String t, int color, int sp) {
        TextView tv = new TextView(this);
        tv.setText(t);
        tv.setTextColor(color);
        tv.setTextSize(sp);
        if (sp >= 18) tv.setTypeface(null, android.graphics.Typeface.BOLD);
        return tv;
    }

    private void spacer(LinearLayout parent, int dpSize) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(dpSize)));
        parent.addView(v);
    }

    private int dp(int val) {
        return (int)(val * getResources().getDisplayMetrics().density);
    }
}
