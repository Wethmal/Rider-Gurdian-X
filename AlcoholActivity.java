package com.example.ridergurdianx;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class AlcoholActivity extends AppCompatActivity {

    private static final String TAG = "AlcoholActivity";

    private TextView tvAlcoholDetected;
    private TextView tvAlcoholLevel;
    private TextView tvAlcoholAdvice;

    private DatabaseReference alcoholRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alcohol);

        tvAlcoholDetected = findViewById(R.id.tvAlcoholDetected);
        tvAlcoholLevel    = findViewById(R.id.tvAlcoholLevel);
        tvAlcoholAdvice   = findViewById(R.id.tvAlcoholAdvice);

        FirebaseDatabase db = FirebaseDatabase.getInstance(
                "https://iottest-6f78a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        alcoholRef = db.getReference("alcohol");

        alcoholRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Log.d(TAG, "onDataChange: " + snapshot);  // <== watch this in Logcat

                if (!snapshot.exists()) {
                    tvAlcoholDetected.setText("Detected: --");
                    tvAlcoholLevel.setText("Sensor Value: --");
                    tvAlcoholAdvice.setText("No alcohol data available.");
                    return;
                }

                Boolean detected = snapshot.child("detected").getValue(Boolean.class);
                boolean isDetected = detected != null && detected;
                String detectText = isDetected ? "YES" : "NO";

                Object gasObj = snapshot.child("gasValue").getValue();
                String gasText = gasObj != null ? gasObj.toString() : "--";

                tvAlcoholDetected.setText("Detected: " + detectText);
                tvAlcoholLevel.setText("Gas Value: " + gasText);

                if (isDetected) {
                    tvAlcoholAdvice.setText("Status: HIGH – Alcohol detected! Riding NOT allowed.");
                } else {
                    tvAlcoholAdvice.setText("Status: SAFE – No alcohol detected.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AlcoholActivity.this,
                        "Failed to load alcohol data: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
