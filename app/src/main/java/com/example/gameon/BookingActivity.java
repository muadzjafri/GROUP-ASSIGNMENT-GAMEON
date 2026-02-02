package com.example.gameon;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class BookingActivity extends AppCompatActivity {

    private DatePicker datePicker;
    private Button btnSlot18, btnSlot20, btnSlot22, btnConfirm;
    private String selectedTime = "";
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String venueName = "Unknown Venue";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        if (getIntent().hasExtra("EXTRA_NAME")) {
            venueName = getIntent().getStringExtra("EXTRA_NAME");
        }
        setTitle("Book: " + venueName);

        datePicker = findViewById(R.id.datePicker);
        btnSlot18 = findViewById(R.id.btnSlot18);
        btnSlot20 = findViewById(R.id.btnSlot20);
        btnSlot22 = findViewById(R.id.btnSlot22);
        btnConfirm = findViewById(R.id.btnConfirm);

        View.OnClickListener slotListener = v -> {
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            btnSlot18.setBackgroundColor(Color.parseColor("#2C2C2C"));
            btnSlot20.setBackgroundColor(Color.parseColor("#2C2C2C"));
            btnSlot22.setBackgroundColor(Color.parseColor("#2C2C2C"));
            v.setBackgroundColor(Color.parseColor("#FF6D00"));

            Button b = (Button) v;
            if (b.getId() == R.id.btnSlot18) selectedTime = "6:00 PM";
            if (b.getId() == R.id.btnSlot20) selectedTime = "8:00 PM";
            if (b.getId() == R.id.btnSlot22) selectedTime = "10:00 PM";
        };

        btnSlot18.setOnClickListener(slotListener);
        btnSlot20.setOnClickListener(slotListener);
        btnSlot22.setOnClickListener(slotListener);

        btnConfirm.setOnClickListener(v -> saveBooking());
    }

    private void saveBooking() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedTime.isEmpty()) {
            Toast.makeText(this, "Select a time slot!", Toast.LENGTH_SHORT).show();
            return;
        }

        int day = datePicker.getDayOfMonth();
        int month = datePicker.getMonth();
        int year = datePicker.getYear();
        String dateStr = day + "/" + (month + 1) + "/" + year;

        Map<String, Object> booking = new HashMap<>();
        booking.put("venue", venueName);
        booking.put("date", dateStr);
        booking.put("time", selectedTime);
        booking.put("user_email", mAuth.getCurrentUser().getEmail());
        booking.put("timestamp", System.currentTimeMillis());

        // --- CRITICAL UPDATE: STATUS ---
        booking.put("status", "ACTIVE");

        db.collection("bookings").add(booking)
                .addOnSuccessListener(documentReference -> {
                    getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    Toast.makeText(BookingActivity.this, "Booking Successful!", Toast.LENGTH_SHORT).show();
                    addMatchToCalendar(venueName, year, month, day, selectedTime);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(BookingActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void addMatchToCalendar(String venue, int year, int month, int day, String time) {
        Calendar beginTime = Calendar.getInstance();
        int hour = 18;
        if (time.contains("8:00")) hour = 20;
        if (time.contains("10:00")) hour = 22;
        beginTime.set(year, month, day, hour, 0);

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, beginTime.getTimeInMillis() + (3600000))
                .putExtra(CalendarContract.Events.TITLE, "GameOn Match: " + venue)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, venue);
        startActivity(intent);
    }
}