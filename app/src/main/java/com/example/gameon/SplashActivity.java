package com.example.gameon;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Wait 3 seconds, then go to LoginActivity (The Gatekeeper)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // CHANGE THIS LINE: Go to LoginActivity, not MainActivity
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Prevent going back to splash
            }
        }, 3000);
    }
}