package com.example.gameon;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateRequestActivity extends AppCompatActivity {

    private EditText etGame, etLocation, etTime, etSkill;
    private TextView tvCount;
    private Button btnPost, btnImport, btnMinus, btnPlus;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private int playersNeeded = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_request);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etGame = findViewById(R.id.etGame);
        etLocation = findViewById(R.id.etLocation);
        etTime = findViewById(R.id.etTime);
        etSkill = findViewById(R.id.etSkill);
        tvCount = findViewById(R.id.tvCount);

        btnPost = findViewById(R.id.btnPost);
        btnImport = findViewById(R.id.btnImport);
        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);

        btnMinus.setOnClickListener(v -> {
            if (playersNeeded > 1) { playersNeeded--; tvCount.setText(String.valueOf(playersNeeded)); }
        });
        btnPlus.setOnClickListener(v -> {
            if (playersNeeded < 20) { playersNeeded++; tvCount.setText(String.valueOf(playersNeeded)); }
        });

        btnImport.setOnClickListener(v -> showBookingDialog());
        btnPost.setOnClickListener(v -> postRequest());
    }

    private void showBookingDialog() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("bookings").whereEqualTo("user_email", mAuth.getCurrentUser().getEmail()).get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) { Toast.makeText(this, "No bookings found.", Toast.LENGTH_SHORT).show(); return; }
                    List<String> venueList = new ArrayList<>();
                    List<QueryDocumentSnapshot> docList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        venueList.add(doc.getString("venue") + " (" + doc.getString("time") + ")");
                        docList.add(doc);
                    }
                    new AlertDialog.Builder(this).setTitle("Select a Booking")
                            .setItems(venueList.toArray(new String[0]), (dialog, which) -> {
                                QueryDocumentSnapshot selected = docList.get(which);
                                etLocation.setText(selected.getString("venue"));
                                etTime.setText(selected.getString("time"));
                                etGame.setText("Futsal");
                            }).show();
                });
    }

    private void postRequest() {
        if (mAuth.getCurrentUser() == null) { Toast.makeText(this, "Login required!", Toast.LENGTH_SHORT).show(); return; }

        Map<String, Object> request = new HashMap<>();
        request.put("game", etGame.getText().toString().trim());
        request.put("location", etLocation.getText().toString().trim());
        request.put("time", etTime.getText().toString().trim());
        request.put("skill", etSkill.getText().toString().trim());
        request.put("players_needed", playersNeeded);
        request.put("host_email", mAuth.getCurrentUser().getEmail());
        request.put("timestamp", System.currentTimeMillis());

        db.collection("team_requests").add(request)
                .addOnSuccessListener(ref -> { Toast.makeText(this, "Game Posted!", Toast.LENGTH_SHORT).show(); finish(); })
                .addOnFailureListener(e -> Toast.makeText(this, "Error.", Toast.LENGTH_SHORT).show());
    }
}