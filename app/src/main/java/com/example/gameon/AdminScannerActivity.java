package com.example.gameon;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class AdminScannerActivity extends AppCompatActivity {

    private Button btnScan;
    private TextView tvResult;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_scanner);

        db = FirebaseFirestore.getInstance();
        btnScan = findViewById(R.id.btnScan);
        tvResult = findViewById(R.id.tvResult);

        btnScan.setOnClickListener(v -> {
            IntentIntegrator integrator = new IntentIntegrator(AdminScannerActivity.this);
            integrator.setPrompt("Scan a GameOn Ticket");
            integrator.setBeepEnabled(true);
            integrator.setOrientationLocked(true);
            integrator.initiateScan();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null && result.getContents() != null) {
            validateTicket(result.getContents());
        }
    }

    private void validateTicket(String qrData) {
        if (!qrData.startsWith("GAMEON_")) {
            showError("INVALID TICKET");
            return;
        }

        tvResult.setText("Verifying...");

        // Split: "GAMEON_Sportizza_12/12/2026_8:00 PM"
        String[] parts = qrData.split("_");
        if(parts.length < 4) return;

        String venue = parts[1];
        String date = parts[2];
        String time = parts[3];

        db.collection("bookings")
                .whereEqualTo("venue", venue)
                .whereEqualTo("date", date)
                .whereEqualTo("time", time)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        showError("TICKET NOT FOUND");
                    } else {
                        processApproval(queryDocumentSnapshots);
                    }
                });
    }

    private void processApproval(QuerySnapshot documents) {
        String docId = documents.getDocuments().get(0).getId();
        String currentStatus = documents.getDocuments().get(0).getString("status");

        if ("USED".equals(currentStatus)) {
            showError("ALREADY USED!");
        } else {
            // APPROVE AND UPDATE
            db.collection("bookings").document(docId).update("status", "USED");

            tvResult.setText("✅ ACCESS GRANTED\nBooking Verified.");
            tvResult.setTextColor(Color.parseColor("#2E7D32"));
            getWindow().getDecorView().performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
        }
    }

    private void showError(String msg) {
        tvResult.setText("❌ " + msg);
        tvResult.setTextColor(Color.RED);
    }
}