package com.example.gameon;

import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract; // 1. NEW IMPORT
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
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
import java.util.HashMap;
import java.util.Map;

public class TeamFeedActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirestoreRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_team_feed);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        setupLiveFeed();
    }

    private void setupLiveFeed() {
        Query query = db.collection("team_requests").orderBy("timestamp", Query.Direction.DESCENDING);
        FirestoreRecyclerOptions<RequestModel> options = new FirestoreRecyclerOptions.Builder<RequestModel>()
                .setQuery(query, RequestModel.class).build();

        adapter = new FirestoreRecyclerAdapter<RequestModel, RequestHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull RequestHolder holder, int position, @NonNull RequestModel model) {
                String docId = getSnapshots().getSnapshot(position).getId();
                holder.tvGame.setText(model.getGame());
                holder.tvLocation.setText(model.getLocation());
                holder.tvTime.setText(model.getTime());

                if(model.getSkill() != null && !model.getSkill().isEmpty()) {
                    holder.tvSkill.setText(model.getSkill().toUpperCase());
                    holder.tvSkill.setVisibility(View.VISIBLE);
                } else holder.tvSkill.setVisibility(View.GONE);

                int count = model.getPlayers_needed();
                if (count > 0) {
                    holder.tvPlayerCount.setText(count + " Spots");
                    holder.btnJoin.setEnabled(true);
                    holder.btnJoin.setText("JOIN");
                } else {
                    holder.tvPlayerCount.setText("FULL");
                    holder.btnJoin.setEnabled(false);
                    holder.btnJoin.setText("FULL");
                }

                holder.btnJoin.setOnClickListener(v -> joinTeam(docId, model));
            }

            @NonNull
            @Override
            public RequestHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_feed, parent, false);
                return new RequestHolder(v);
            }
        };
        recyclerView.setAdapter(adapter);
    }

    private void joinTeam(String docId, RequestModel model) {
        if (mAuth.getCurrentUser() == null) { Toast.makeText(this, "Login first!", Toast.LENGTH_SHORT).show(); return; }

        DocumentReference gameRef = db.collection("team_requests").document(docId);

        db.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(gameRef);
                    Long currentSlots = snapshot.getLong("players_needed");
                    if (currentSlots == null || currentSlots <= 0) throw new RuntimeException("Team Full");

                    // 1. Update Game
                    transaction.update(gameRef, "players_needed", currentSlots - 1);

                    // 2. Add to My Joins
                    Map<String, Object> joinData = new HashMap<>();
                    joinData.put("game", model.getGame());
                    joinData.put("location", model.getLocation());
                    joinData.put("time", model.getTime());
                    joinData.put("user_email", mAuth.getCurrentUser().getEmail());
                    joinData.put("timestamp", System.currentTimeMillis());
                    joinData.put("request_id", docId);

                    transaction.set(db.collection("my_joins").document(), joinData);
                    return currentSlots - 1;
                })
                .addOnSuccessListener(r -> {
                    // --- SUCCESS! ---
                    Toast.makeText(this, "Joined Successfully!", Toast.LENGTH_SHORT).show();

                    // 3. TRIGGER CALENDAR UPDATE (NEW)
                    addToCalendar(model.getGame(), model.getLocation(), model.getTime());
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // --- NEW HELPER METHOD FOR CALENDAR ---
    private void addToCalendar(String game, String location, String timeString) {
        // Since 'timeString' might be informal (e.g. "Tonight 8pm"), we use a generic insert intent.
        // This opens the Calendar App pre-filled with details, allowing the user to confirm the exact time.
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, "GameOn Team Match: " + game)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.Events.DESCRIPTION, "You joined this team via GameOn. Time: " + timeString)
                .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Could not open Calendar app", Toast.LENGTH_SHORT).show();
        }
    }

    class RequestHolder extends RecyclerView.ViewHolder {
        TextView tvGame, tvLocation, tvTime, tvSkill, tvPlayerCount;
        Button btnJoin;
        public RequestHolder(@NonNull View itemView) {
            super(itemView);
            tvGame = itemView.findViewById(R.id.tvGame);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSkill = itemView.findViewById(R.id.tvSkill);
            tvPlayerCount = itemView.findViewById(R.id.tvPlayerCount);
            btnJoin = itemView.findViewById(R.id.btnJoin);
        }
    }

    public static class RequestModel {
        String game, location, time, skill;
        int players_needed;
        public RequestModel() {}
        public String getGame() { return game; }
        public String getLocation() { return location; }
        public String getTime() { return time; }
        public String getSkill() { return skill; }
        public int getPlayers_needed() { return players_needed; }
    }

    @Override protected void onStart() { super.onStart(); if(adapter!=null) adapter.startListening(); }
    @Override protected void onStop() { super.onStop(); if(adapter!=null) adapter.stopListening(); }
}