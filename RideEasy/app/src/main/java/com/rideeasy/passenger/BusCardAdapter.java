package com.rideeasy.passenger;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class BusCardAdapter extends RecyclerView.Adapter<BusCardAdapter.ViewHolder> {

    private final List<BusModel> buses;
    private final Context context;

    public BusCardAdapter(Context context, List<BusModel> buses) {
        this.context = context;
        this.buses   = buses;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_bus_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        BusModel bus = buses.get(position);

        int seats    = AppConfig.TOTAL_SEATS;
        int free     = Math.max(0, seats - bus.totalPassengers);
        int standing = Math.max(0, bus.totalPassengers - seats);
        int percent  = Math.min(100, (int)((bus.totalPassengers / (float) seats) * 100));

        h.busNum.setText(bus.busNumber);
        h.routeText.setText(bus.route);
        h.plateText.setText(bus.numberPlate);
        h.passengerCount.setText("👥 " + bus.totalPassengers + " passengers");
        h.percentText.setText(percent + "%");
        h.crowdBar.setProgress(percent);

        if (standing > 0) {
            h.seatsText.setText("💺 0 seats • " + standing + " standing");
        } else {
            h.seatsText.setText("💺 " + free + " seats free");
        }

        // Colour by crowd status
        int color;
        String statusLabel;
        int chipBg;
        if (percent < AppConfig.CROWD_FREE_THRESHOLD) {
            color      = 0xFF10B981;
            statusLabel = "FREE";
            chipBg      = R.drawable.chip_free_bg;
        } else if (percent < AppConfig.CROWD_CROWDED_THRESHOLD) {
            color      = 0xFFF59E0B;
            statusLabel = "MODERATE";
            chipBg      = R.drawable.chip_moderate_bg;
        } else {
            color      = 0xFFEF4444;
            statusLabel = "CROWDED";
            chipBg      = R.drawable.chip_crowded_bg;
        }

        h.crowdChip.setText(statusLabel);
        h.crowdChip.setTextColor(color);
        h.crowdChip.setBackground(context.getDrawable(chipBg));
        h.seatsText.setTextColor(standing > 0 ? 0xFFEF4444 : color);
        h.crowdBar.setProgressTintList(ColorStateList.valueOf(color));

        // Details button
        h.detailsBtn.setOnClickListener(v -> {
            Intent i = new Intent(context, BusResultActivity.class);
            i.putExtra("busNumber",    bus.busNumber);
            i.putExtra("conductorId",  bus.conductorId);
            i.putExtra("numberPlate",  bus.numberPlate);
            i.putExtra("route",        bus.route);
            i.putExtra("passengers",   bus.totalPassengers);
            i.putExtra("crowdStatus",  statusLabel);
            context.startActivity(i);
        });

        // Book button
        h.bookBtn.setOnClickListener(v -> {
            Intent i = new Intent(context, BookingActivity.class);
            i.putExtra("busNumber",   bus.busNumber);
            i.putExtra("conductorId", bus.conductorId);
            i.putExtra("numberPlate", bus.numberPlate);
            i.putExtra("route",       bus.route);
            context.startActivity(i);
        });

        // Whole card also opens details
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(context, BusResultActivity.class);
            i.putExtra("busNumber",   bus.busNumber);
            i.putExtra("conductorId", bus.conductorId);
            i.putExtra("numberPlate", bus.numberPlate);
            i.putExtra("route",       bus.route);
            i.putExtra("passengers",  bus.totalPassengers);
            i.putExtra("crowdStatus", statusLabel);
            context.startActivity(i);
        });
    }

    @Override
    public int getItemCount() { return buses.size(); }

    public void updateBus(BusModel updated) {
        for (int i = 0; i < buses.size(); i++) {
            if (buses.get(i).busNumber.equals(updated.busNumber)
                    && buses.get(i).conductorId.equals(updated.conductorId)) {
                buses.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
        buses.add(updated);
        notifyItemInserted(buses.size() - 1);
    }

    // ─────────────────────────────────────────────────────────────────────────
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView busNum, routeText, plateText, passengerCount, seatsText, percentText, crowdChip;
        ProgressBar crowdBar;
        MaterialButton detailsBtn, bookBtn;

        ViewHolder(View itemView) {
            super(itemView);
            busNum         = itemView.findViewById(R.id.busNum);
            routeText      = itemView.findViewById(R.id.routeText);
            plateText      = itemView.findViewById(R.id.plateText);
            passengerCount = itemView.findViewById(R.id.passengerCount);
            seatsText      = itemView.findViewById(R.id.seatsText);
            percentText    = itemView.findViewById(R.id.percentText);
            crowdChip      = itemView.findViewById(R.id.crowdChip);
            crowdBar       = itemView.findViewById(R.id.crowdBar);
            detailsBtn     = itemView.findViewById(R.id.detailsBtn);
            bookBtn        = itemView.findViewById(R.id.bookBtn);
        }
    }
}
