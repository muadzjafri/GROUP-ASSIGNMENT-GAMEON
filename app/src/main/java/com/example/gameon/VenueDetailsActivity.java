package com.example.gameon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class VenueDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_venue_details);

        // 1. Link UI Elements
        TextView tvTitle = findViewById(R.id.tvVenueTitle);
        TextView tvAddress = findViewById(R.id.tvVenueAddress); // Make sure this exists in XML!
        TextView tvRating = findViewById(R.id.tvVenueRating);   // Make sure this exists in XML!
        TextView tvDesc = findViewById(R.id.tvVenueDesc);
        Button btnBook = findViewById(R.id.btnBookNow);

        // 2. Get Data passed from MapActivity
        Intent intent = getIntent();
        String venueName = "Venue Details";
        String venueAddress = "Address not available";
        String venueRating = "N/A";

        if (intent != null) {
            if (intent.hasExtra("EXTRA_NAME")) venueName = intent.getStringExtra("EXTRA_NAME");
            if (intent.hasExtra("EXTRA_ADDRESS")) venueAddress = intent.getStringExtra("EXTRA_ADDRESS");
            if (intent.hasExtra("EXTRA_RATING")) venueRating = intent.getStringExtra("EXTRA_RATING");
        }

        // 3. Set Text to UI
        tvTitle.setText(venueName);

        // Show the real address from Google
        tvAddress.setText(venueAddress);

        // Show Rating with a Star
        tvRating.setText("Rating: " + venueRating + " ⭐");

        tvDesc.setText("Welcome to " + venueName + ". Tap below to see available time slots.");

        // 4. THE CONNECTION TO BOOKING
        String finalVenueName = venueName;
        btnBook.setOnClickListener(v -> {
            Intent bookingIntent = new Intent(VenueDetailsActivity.this, BookingActivity.class);
            bookingIntent.putExtra("EXTRA_NAME", finalVenueName);
            startActivity(bookingIntent);
        });
    }
}