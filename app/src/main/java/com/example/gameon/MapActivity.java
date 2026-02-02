package com.example.gameon;

import android.content.Intent;
import android.os.Bundle;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

// PLACES API IMPORTS
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.api.net.SearchByTextRequest;

import java.util.Arrays;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Spinner spinnerFilter;
    private PlacesClient placesClient;

    // Center of search area (Shah Alam)
    private final LatLng SELANGOR_CENTER = new LatLng(3.0733, 101.5185);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        // 1. INITIALIZE PLACES SDK
        // Ensure "google_maps_key" is inside your strings.xml
        String apiKey = getString(R.string.google_maps_key);
        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), apiKey);
        }
        placesClient = Places.createClient(this);

        // 2. Setup Spinner
        spinnerFilter = findViewById(R.id.spinnerFilter);
        String[] categories = {
                "All Venues", "Futsal", "Football", "Badminton",
                "Sepak Takraw", "Basketball", "Tennis", "Gym/Parks"
        };

        // Ensure you have created 'spinner_item_dark.xml' in layout folder
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item_dark, categories);
        adapter.setDropDownViewResource(R.layout.spinner_item_dark);
        spinnerFilter.setAdapter(adapter);

        // 3. Setup Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Move camera to start position
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SELANGOR_CENTER, 12f));

        // 4. Handle Spinner Selection
        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view != null) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                }
                String selectedCategory = parent.getItemAtPosition(position).toString();
                performSearch(selectedCategory);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 5. Handle Marker Clicks (Pass Data to Details)
        mMap.setOnMarkerClickListener(marker -> {
            getWindow().getDecorView().performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);

            // Retrieve the hidden Place object from the marker tag
            Place place = (Place) marker.getTag();

            Intent intent = new Intent(MapActivity.this, VenueDetailsActivity.class);

            if (place != null) {
                // Pass REAL data from Google
                intent.putExtra("EXTRA_NAME", place.getName());
                intent.putExtra("EXTRA_ADDRESS", place.getAddress());

                if (place.getRating() != null) {
                    intent.putExtra("EXTRA_RATING", String.valueOf(place.getRating()));
                } else {
                    intent.putExtra("EXTRA_RATING", "N/A");
                }
            } else {
                // Fallback for safety
                intent.putExtra("EXTRA_NAME", marker.getTitle());
            }

            startActivity(intent);
            return false;
        });
    }

    // --- DYNAMIC SEARCH LOGIC ---
    private void performSearch(String category) {
        if (mMap == null) return;

        mMap.clear();
        String queryText;

        if (!category.equals("All Venues")) {
            queryText = category + " near Shah Alam";
        } else {
            queryText = "Sports venues near Shah Alam";
        }

        // REQUEST EXTRA FIELDS: ADDRESS AND RATING
        List<Place.Field> placeFields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,  // Needed for Details
                Place.Field.RATING    // Needed for Details
        );

        SearchByTextRequest searchRequest = SearchByTextRequest.builder(queryText, placeFields)
                .setMaxResultCount(10)
                .build();

        Toast.makeText(this, "Searching: " + category, Toast.LENGTH_SHORT).show();

        placesClient.searchByText(searchRequest).addOnSuccessListener(response -> {
            for (Place place : response.getPlaces()) {
                if (place.getLatLng() != null) {
                    // Create the marker
                    Marker marker = mMap.addMarker(new MarkerOptions()
                            .position(place.getLatLng())
                            .title(place.getName())
                            .snippet(place.getAddress())); // Show address in small bubble

                    // VITAL STEP: Store the whole Place object inside the marker
                    // This lets us retrieve Address/Rating when clicked
                    if (marker != null) {
                        marker.setTag(place);
                    }
                }
            }
        }).addOnFailureListener(exception -> {
            Toast.makeText(MapActivity.this, "Search Error: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}