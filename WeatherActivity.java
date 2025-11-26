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

public class WeatherActivity extends AppCompatActivity {

    private static final String TAG = "WeatherActivity";

    private TextView tvCondition;
    private TextView tvTemperature;
    private TextView tvHumidity;

    private DatabaseReference weatherRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

        tvCondition   = findViewById(R.id.tvWeatherConditionDetail);
        tvTemperature = findViewById(R.id.tvWeatherTemperatureDetail);
        tvHumidity    = findViewById(R.id.tvWeatherHumidityDetail);

        FirebaseDatabase db = FirebaseDatabase.getInstance(
                "https://iottest-6f78a-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        weatherRef = db.getReference("weather");

        weatherRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                Log.d(TAG, "onDataChange: " + snapshot);

                if (!snapshot.exists()) {
                    tvCondition.setText("Condition: --");
                    tvTemperature.setText("Temperature: -- °C");
                    tvHumidity.setText("Humidity: -- %");
                    return;
                }

                String cond = null;
                if (snapshot.child("condition").getValue() != null) {
                    cond = snapshot.child("condition").getValue().toString();
                } else if (snapshot.child("cond").getValue() != null) {
                    cond = snapshot.child("cond").getValue().toString();
                }
                if (cond == null || cond.isEmpty()) cond = "--";

                Double temp = null;
                if (snapshot.child("temp").getValue() != null) {
                    temp = toDouble(snapshot.child("temp").getValue());
                } else if (snapshot.child("temperature").getValue() != null) {
                    temp = toDouble(snapshot.child("temperature").getValue());
                }

                Double hum = null;
                if (snapshot.child("hum").getValue() != null) {
                    hum = toDouble(snapshot.child("hum").getValue());
                } else if (snapshot.child("humidity").getValue() != null) {
                    hum = toDouble(snapshot.child("humidity").getValue());
                }

                tvCondition.setText("Condition: " + cond);

                tvTemperature.setText(
                        temp == null ? "Temperature: -- °C"
                                : String.format("Temperature: %.1f °C", temp)
                );

                tvHumidity.setText(
                        hum == null ? "Humidity: -- %"
                                : String.format("Humidity: %.1f %%", hum)
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(WeatherActivity.this,
                        "Failed to load weather: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Double toDouble(Object v) {
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
