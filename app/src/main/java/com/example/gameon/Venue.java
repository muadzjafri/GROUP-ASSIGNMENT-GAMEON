package com.example.gameon;

public class Venue {
    private String name;
    private String imageUrl; // We use a URL string, not a Storage file
    private double lat;
    private double lng;

    // Empty constructor needed for Firestore
    public Venue() {}

    public Venue(String name, String imageUrl, double lat, double lng) {
        this.name = name;
        this.imageUrl = imageUrl;
        this.lat = lat;
        this.lng = lng;
    }

    // Getters
    public String getName() { return name; }
    public String getImageUrl() { return imageUrl; }
    public double getLat() { return lat; }
    public double getLng() { return lng; }
}