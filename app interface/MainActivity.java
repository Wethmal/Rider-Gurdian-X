package com.example.ridergurdianx;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private LinearLayout cardHelmetStatus;
    private LinearLayout cardSpeed;
    private LinearLayout cardCurrentLocation;
    private LinearLayout cardEmergency;
    private LinearLayout cardProfile;
    private LinearLayout cardSettings;
    private LinearLayout cardLiveCam;
    private LinearLayout cardWeather;
    private LinearLayout cardUltrasonic;
    private LinearLayout cardMpu6050;
    private LinearLayout cardAlcohol;

    private TextView tvCurrentSpeed;
    private TextView navDashboard, navMap, navMore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);   // must contain all IDs below

        initViews();
        setupClickListeners();
        mockSpeedData();
    }

    private void initViews() {
        cardHelmetStatus    = findViewById(R.id.cardHelmetStatus);
        cardSpeed           = findViewById(R.id.cardSpeed);
        cardCurrentLocation = findViewById(R.id.cardCurrentLocation);
        cardEmergency       = findViewById(R.id.cardEmergency);
        cardProfile         = findViewById(R.id.cardProfile);
        cardSettings        = findViewById(R.id.cardSettings);
        cardLiveCam         = findViewById(R.id.cardLiveCam);
        cardWeather         = findViewById(R.id.cardWeather);
        cardUltrasonic      = findViewById(R.id.cardUltrasonic);
        cardMpu6050         = findViewById(R.id.cardMpu6050);
        cardAlcohol         = findViewById(R.id.cardAlcohol);

        tvCurrentSpeed = findViewById(R.id.tvCurrentSpeed);

        navDashboard = findViewById(R.id.navDashboard);
        navMap       = findViewById(R.id.navMap);
        navMore      = findViewById(R.id.navMore);
    }

    private void safeSet(LinearLayout view, String viewName, Runnable action) {
        if (view != null) {
            view.setOnClickListener(v -> action.run());
        } else {
            Log.e(TAG, "View is null: " + viewName);
        }
    }

    private void safeSetNav(TextView view, String viewName, Runnable action) {
        if (view != null) {
            view.setOnClickListener(v -> action.run());
        } else {
            Log.e(TAG, "Nav view is null: " + viewName);
        }
    }

    private void setupClickListeners() {

        // Helmet status
        safeSet(cardHelmetStatus, "cardHelmetStatus", () ->
                Toast.makeText(this,
                        "Open Helmet Display Screen (ESP/Helmet status).",
                        Toast.LENGTH_SHORT).show()
        );

        // Speed card
        safeSet(cardSpeed, "cardSpeed", () ->
                Toast.makeText(this,
                        "Open detailed speed & ride stats.",
                        Toast.LENGTH_SHORT).show()
        );

        // Current Location
        safeSet(cardCurrentLocation, "cardCurrentLocation", () ->
                startActivity(new Intent(this, LocationMapActivity.class))
        );

        // Emergency contacts / SOS
        safeSet(cardEmergency, "cardEmergency", () ->
                startActivity(new Intent(this, SosContactsActivity.class))
        );

        // Profile
        safeSet(cardProfile, "cardProfile", () ->
                Toast.makeText(this,
                        "Open Login / Signup / Rider Profile.",
                        Toast.LENGTH_SHORT).show()
        );

        // Settings
        safeSet(cardSettings, "cardSettings", () ->
                Toast.makeText(this,
                        "Open settings for alerts, sensors & theme.",
                        Toast.LENGTH_SHORT).show()
        );

        // Live Cam (ESP32-CAM stream)
        safeSet(cardLiveCam, "cardLiveCam", () ->
                startActivity(new Intent(this, LiveCamActivity.class))
        );

        // Weather card
        safeSet(cardWeather, "cardWeather", () ->
                startActivity(new Intent(this, WeatherActivity.class))
        );

        // Ultrasonic → safe distance screen
        safeSet(cardUltrasonic, "cardUltrasonic", () ->
                startActivity(new Intent(this, UltrasonicActivity.class))
        );

        // MPU6050 accident / tilt detection
        safeSet(cardMpu6050, "cardMpu6050", () ->
                startActivity(new Intent(this, BikeAttitudeActivity.class))
        );

        // Alcohol sensor
        safeSet(cardAlcohol, "cardAlcohol", () ->
                startActivity(new Intent(this, AlcoholActivity.class))
        );

        // Bottom navigation
        safeSetNav(navDashboard, "navDashboard", () ->
                Toast.makeText(this,
                        "You are already on Dashboard.",
                        Toast.LENGTH_SHORT).show()
        );

        safeSetNav(navMap, "navMap", () ->
                Toast.makeText(this,
                        "Bottom Nav: Map clicked.",
                        Toast.LENGTH_SHORT).show()
        );

        safeSetNav(navMore, "navMore", () ->
                Toast.makeText(this,
                        "Bottom Nav: More clicked.",
                        Toast.LENGTH_SHORT).show()
        );
    }

    private void mockSpeedData() {
        int demoSpeed = 32;  // demo value
        if (tvCurrentSpeed != null) {
            tvCurrentSpeed.setText(demoSpeed + " km/h");
        } else {
            Log.e(TAG, "tvCurrentSpeed is null");
        }
    }
}
