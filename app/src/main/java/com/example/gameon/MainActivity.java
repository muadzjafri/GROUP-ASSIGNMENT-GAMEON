package com.example.gameon;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// --- NEW IMPORT FOR THE BUTTON ---
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvGreeting, tvUsername, tvHeroTitle, tvHeroTime, tvStatBookings, tvStatTeams;
    private Button btnLogin;

    // Listeners
    private ListenerRegistration bookingListener, teamListener, nameListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI Links
        tvGreeting = findViewById(R.id.tvGreeting);
        tvUsername = findViewById(R.id.tvUsername);
        btnLogin = findViewById(R.id.btnLogin);
        tvHeroTitle = findViewById(R.id.tvHeroTitle);
        tvHeroTime = findViewById(R.id.tvHeroTime);
        tvStatBookings = findViewById(R.id.tvStatBookings);
        tvStatTeams = findViewById(R.id.tvStatTeams);

        // --- 1. EXISTING BUTTONS ---
        findViewById(R.id.btnDiscovery).setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        findViewById(R.id.btnMyBookings).setOnClickListener(v -> startActivity(new Intent(this, MyBookingsActivity.class)));
        findViewById(R.id.btnFindTeam).setOnClickListener(v -> startActivity(new Intent(this, CreateRequestActivity.class))); // HOST GAME
        findViewById(R.id.btnViewFeed).setOnClickListener(v -> startActivity(new Intent(this, TeamFeedActivity.class))); // JOIN GAME
        findViewById(R.id.btnMyTeams).setOnClickListener(v -> startActivity(new Intent(this, MyJoinsActivity.class))); // LEAVE GAME

        // --- 2. NEW: SCANNER BUTTON LOGIC ---
        // We use FloatingActionButton because that is what we put in the XML
        FloatingActionButton btnScan = findViewById(R.id.btnScan);
        btnScan.setOnClickListener(v -> {
            // Launch the Scanner Activity
            Intent intent = new Intent(MainActivity.this, AdminScannerActivity.class);
            startActivity(intent);
        });

        // --- 3. PROFILE LOGIC ---
        View.OnClickListener openProfile = v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        tvGreeting.setOnClickListener(openProfile);
        tvUsername.setOnClickListener(openProfile);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupRealtimeListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bookingListener != null) bookingListener.remove();
        if (teamListener != null) teamListener.remove();
        if (nameListener != null) nameListener.remove();
    }

    private void setupRealtimeListeners() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            tvGreeting.setText("Hello,");
            tvUsername.setText("Guest");
            tvStatBookings.setText("-");
            tvStatTeams.setText("-");
            btnLogin.setText("Login");
            btnLogin.setOnClickListener(v -> startActivity(new Intent(this, LoginActivity.class)));
            return;
        }

        tvGreeting.setText("Welcome back,");
        btnLogin.setText("Logout");
        btnLogin.setOnClickListener(v -> { mAuth.signOut(); recreate(); });

        // 1. Name Listener
        nameListener = db.collection("users").document(user.getUid())
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists() && snapshot.getString("name") != null)
                        tvUsername.setText(snapshot.getString("name"));
                    else tvUsername.setText(user.getEmail());
                });

        // 2. Booking Listener
        bookingListener = db.collection("bookings")
                .whereEqualTo("user_email", user.getEmail())
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    int count = (snapshots != null) ? snapshots.size() : 0;
                    tvStatBookings.setText(String.valueOf(count));

                    if (count > 0) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                        tvHeroTitle.setText(doc.getString("venue"));
                        tvHeroTime.setText(doc.getString("date") + " @ " + doc.getString("time"));
                    } else if (tvStatTeams.getText().equals("0")) {
                        tvHeroTitle.setText("No Upcoming Games");
                        tvHeroTime.setText("Book a court or join a team!");
                    }
                });

        // 3. Team Listener
        teamListener = db.collection("my_joins")
                .whereEqualTo("user_email", user.getEmail())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    int count = (snapshots != null) ? snapshots.size() : 0;
                    tvStatTeams.setText(String.valueOf(count));

                    if (tvStatBookings.getText().toString().equals("0")) {
                        if (count > 0) {
                            QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                            tvHeroTitle.setText(doc.getString("game"));
                            tvHeroTime.setText("Team Match @ " + doc.getString("time"));
                        } else {
                            tvHeroTitle.setText("No Upcoming Games");
                            tvHeroTime.setText("Book a court or join a team!");
                        }
                    }
                });
    }
}