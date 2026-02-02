package com.example.gameon;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class MyBookingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirestoreRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        if (mAuth.getCurrentUser() == null) return;

        Query query = db.collection("bookings")
                .whereEqualTo("user_email", mAuth.getCurrentUser().getEmail())
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<Booking> options = new FirestoreRecyclerOptions.Builder<Booking>()
                .setQuery(query, Booking.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<Booking, BookingHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull BookingHolder holder, int position, @NonNull Booking model) {
                holder.tvVenue.setText(model.getVenue());
                holder.tvDateTime.setText(model.getDate() + " | " + model.getTime());

                // Show Status if "USED"
                if("USED".equals(model.getStatus())){
                    holder.tvVenue.setTextColor(Color.GRAY);
                    holder.tvDateTime.setText("TICKET USED / EXPIRED");
                }

                // --- GENERATE QR STRING ---
                // Must match the format expected by the scanner!
                String qrContent = "GAMEON_" + model.getVenue() + "_" + model.getDate() + "_" + model.getTime();

                Bitmap qrBitmap = generateQRCode(qrContent);
                if (qrBitmap != null) holder.imgQr.setImageBitmap(qrBitmap);

                holder.btnMap.setOnClickListener(v -> {
                    Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(model.getVenue()));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);
                });
            }

            @NonNull
            @Override
            public BookingHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
                return new BookingHolder(v);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    private Bitmap generateQRCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, 200, 200);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            return bmp;
        } catch (WriterException e) { return null; }
    }

    public static class Booking {
        String venue, date, time, status;
        public Booking() {}
        public String getVenue() { return venue; }
        public String getDate() { return date; }
        public String getTime() { return time; }
        public String getStatus() { return status; }
    }

    class BookingHolder extends RecyclerView.ViewHolder {
        TextView tvVenue, tvDateTime;
        Button btnMap;
        ImageView imgQr;
        public BookingHolder(@NonNull View itemView) {
            super(itemView);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            btnMap = itemView.findViewById(R.id.btnMap);
            imgQr = itemView.findViewById(R.id.imgQr);
        }
    }

    @Override protected void onStart() { super.onStart(); adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); adapter.stopListening(); }
}