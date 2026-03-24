package com.rideeasy.passenger;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {

    EditText searchInput;
    RecyclerView resultsRecyclerView;
    LinearLayout emptyLayout;
    BusCardAdapter adapter;
    List<BusModel> allBuses, filteredBuses;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Build layout programmatically (simple search screen)
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF07090F);
        root.setPadding(dp(16), dp(48), dp(16), dp(16));

        // Back + title
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
        topRow.setPadding(0, 0, 0, dp(16));

        TextView backBtn = new TextView(this);
        backBtn.setText("← Back");
        backBtn.setTextColor(0xFF4F8EF7);
        backBtn.setTextSize(15);
        backBtn.setOnClickListener(v -> finish());
        topRow.addView(backBtn);

        TextView title = new TextView(this);
        title.setText("  Search Buses");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        topRow.addView(title);

        root.addView(topRow);

        // Search box
        searchInput = new EditText(this);
        searchInput.setHint("Bus number or route name...");
        searchInput.setBackgroundColor(0xFF0F1420);
        searchInput.setTextColor(0xFFFFFFFF);
        searchInput.setHintTextColor(0xFF475569);
        searchInput.setTextSize(15);
        searchInput.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(52));
        searchParams.setMargins(0, 0, 0, dp(16));
        searchInput.setLayoutParams(searchParams);
        root.addView(searchInput);

        // Empty
        emptyLayout = new LinearLayout(this);
        emptyLayout.setOrientation(LinearLayout.VERTICAL);
        emptyLayout.setGravity(android.view.Gravity.CENTER);
        emptyLayout.setPadding(0, dp(60), 0, 0);
        emptyLayout.setVisibility(View.GONE);
        TextView emptyText = new TextView(this);
        emptyText.setText("🔍\n\nNo buses found\nfor \"" + "\"");
        emptyText.setTextColor(0xFF475569);
        emptyText.setTextSize(14);
        emptyText.setGravity(android.view.Gravity.CENTER);
        emptyLayout.addView(emptyText);
        root.addView(emptyLayout);

        // Results
        resultsRecyclerView = new RecyclerView(this);
        resultsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        root.addView(resultsRecyclerView);

        setContentView(root);

        allBuses      = new ArrayList<>();
        filteredBuses = new ArrayList<>();
        adapter       = new BusCardAdapter(this, filteredBuses);
        resultsRecyclerView.setAdapter(adapter);

        // If opened from chip shortcut, pre-fill query
        String preQuery = getIntent().getStringExtra("query");
        if (preQuery != null && !preQuery.isEmpty()) {
            searchInput.setText(preQuery);
        }

        // Live search filter — THIS ACTUALLY WORKS (fix for old dead search)
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterBuses(s.toString().trim());
            }
        });

        loadAllBuses();
    }

    private void loadAllBuses() {
        ApiHelper.get(AppConfig.BUSES_URL, new ApiHelper.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                runOnUiThread(() -> {
                    try {
                        if (response.getBoolean("success")) {
                            JSONArray buses = response.getJSONArray("buses");
                            allBuses.clear();
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
                                allBuses.add(model);
                            }
                            String query = searchInput.getText().toString().trim();
                            filterBuses(query);
                        }
                    } catch (Exception e) {
                        Toast.makeText(SearchActivity.this, "Error loading buses", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(SearchActivity.this,
                                "Cannot connect to server", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void filterBuses(String query) {
        filteredBuses.clear();
        String lower = query.toLowerCase();
        for (BusModel b : allBuses) {
            if (query.isEmpty()
                    || b.busNumber.toLowerCase().contains(lower)
                    || b.route.toLowerCase().contains(lower)
                    || (b.numberPlate != null && b.numberPlate.toLowerCase().contains(lower))) {
                filteredBuses.add(b);
            }
        }
        adapter.notifyDataSetChanged();
        emptyLayout.setVisibility(filteredBuses.isEmpty() ? View.VISIBLE : View.GONE);
        resultsRecyclerView.setVisibility(filteredBuses.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private int dp(int val) {
        return (int)(val * getResources().getDisplayMetrics().density);
    }
}