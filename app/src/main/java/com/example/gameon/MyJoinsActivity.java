package com.example.gameon;

import android.content.Intent;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class MyJoinsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirestoreRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_joins);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        if (mAuth.getCurrentUser() == null) return;

        Query query = db.collection("my_joins")
                .whereEqualTo("user_email", mAuth.getCurrentUser().getEmail())
                .orderBy("timestamp", Query.Direction.DESCENDING);

        FirestoreRecyclerOptions<JoinModel> options = new FirestoreRecyclerOptions.Builder<JoinModel>()
                .setQuery(query, JoinModel.class)
                .build();

        adapter = new FirestoreRecyclerAdapter<JoinModel, JoinHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull JoinHolder holder, int position, @NonNull JoinModel model) {
                holder.tvGame.setText(model.getGame());
                holder.tvDetails.setText(model.getLocation() + " | " + model.getTime());

                // LEAVE BUTTON LOGIC
                holder.btnLeave.setOnClickListener(v -> {
                    new AlertDialog.Builder(MyJoinsActivity.this)
                            .setTitle("Leave Team?")
                            .setMessage("Are you sure?")
                            .setPositiveButton("Yes", (dialog, which) -> {
                                DocumentReference joinRef = getSnapshots().getSnapshot(position).getReference();
                                leaveTeam(joinRef, model.getRequest_id());
                            })
                            .setNegativeButton("No", null)
                            .show();
                });

                // MAP BUTTON LOGIC
                holder.btnMap.setOnClickListener(v -> {
                    String venueName = model.getLocation();
                    Uri mapUri = Uri.parse("geo:0,0?q=" + Uri.encode(venueName));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, mapUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    try {
                        v.getContext().startActivity(mapIntent);
                    } catch (Exception e) {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(venueName)));
                        v.getContext().startActivity(browserIntent);
                    }
                });
            }

            @NonNull
            @Override
            public JoinHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
                return new JoinHolder(v);
            }
        };

        recyclerView.setAdapter(adapter);
    }

    private void leaveTeam(DocumentReference joinRef, String originalRequestId) {
        db.runTransaction(transaction -> {
                    transaction.delete(joinRef);
                    if (originalRequestId != null && !originalRequestId.isEmpty()) {
                        DocumentReference requestRef = db.collection("team_requests").document(originalRequestId);
                        DocumentSnapshot snapshot = transaction.get(requestRef);
                        if (snapshot.exists()) {
                            Long current = snapshot.getLong("players_needed");
                            if (current == null) current = 0L;
                            transaction.update(requestRef, "players_needed", current + 1);
                        }
                    }
                    return null;
                }).addOnSuccessListener(v -> Toast.makeText(this, "Left Team", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    joinRef.delete(); // Fallback
                    Toast.makeText(this, "Left Team", Toast.LENGTH_SHORT).show();
                });
    }

    // --- UPDATED VIEW HOLDER ---
    class JoinHolder extends RecyclerView.ViewHolder {
        TextView tvGame, tvDetails;
        Button btnLeave, btnMap;
        ImageView imgQr; // We need to find this to hide it

        public JoinHolder(@NonNull View itemView) {
            super(itemView);
            tvGame = itemView.findViewById(R.id.tvVenue);
            tvDetails = itemView.findViewById(R.id.tvDateTime);

            // Buttons
            btnLeave = itemView.findViewById(R.id.btnCancel);
            btnLeave.setText("Leave Team"); // Rename button

            btnMap = itemView.findViewById(R.id.btnMap);
            btnMap.setVisibility(View.VISIBLE);

            // --- THE FIX: HIDE THE QR CODE ---
            imgQr = itemView.findViewById(R.id.imgQr);
            if (imgQr != null) {
                imgQr.setVisibility(View.GONE); // <--- THIS REMOVES THE WHITE SPACE
            }
        }
    }

    public static class JoinModel {
        String game, location, time, request_id;
        public JoinModel() {}
        public String getGame() { return game; }
        public String getLocation() { return location; }
        public String getTime() { return time; }
        public String getRequest_id() { return request_id; }
    }

    @Override protected void onStart() { super.onStart(); if(adapter!=null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if(adapter!=null) adapter.stopListening(); }
}