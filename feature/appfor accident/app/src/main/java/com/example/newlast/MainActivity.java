package com.example.newlast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String DEVICE_NAME = "HC-05";
    private final String PHONE_NUMBER = "0769930678";
    private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice hc05;
    BluetoothSocket btSocket;
    InputStream inputStream;
    FusedLocationProviderClient fusedLocationClient;

    TextView statusText, connectionStatus, locationText;
    Button btnConnect, btnCall, btnSendLocation;

    private boolean isConnected = false;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request all permissions at startup
        requestAllPermissions();

        statusText = findViewById(R.id.statusText);
        connectionStatus = findViewById(R.id.connectionStatus);
        locationText = findViewById(R.id.locationText);
        btnConnect = findViewById(R.id.btnConnect);
        btnCall = findViewById(R.id.btnCall);
        btnSendLocation = findViewById(R.id.btnSendLocation);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        btnConnect.setOnClickListener(v -> connectHC05());
        btnCall.setOnClickListener(v -> makePhoneCall());
        btnSendLocation.setOnClickListener(v -> sendLocationSMS());

        // Check if location services are enabled
        checkLocationServices();

        // Start location updates
        startLocationUpdates();
    }

    private void checkLocationServices() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle("Location Services Disabled")
                    .setMessage("Please enable Location/GPS in your phone settings for accurate positioning.")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }
    }

    private void requestAllPermissions() {
        String[] permissions;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+ (API 31+)
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            // Below Android 12
            permissions = new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.CALL_PHONE,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        ActivityCompat.requestPermissions(this, permissions, 1);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 || requestCode == 100) {
            boolean allGranted = true;
            StringBuilder deniedPerms = new StringBuilder();

            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    deniedPerms.append(permissions[i].replace("android.permission.", "")).append(", ");
                }
            }

            if (allGranted) {
                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
            } else {
                String denied = deniedPerms.toString();
                if (denied.length() > 2) {
                    denied = denied.substring(0, denied.length() - 2);
                }
                Toast.makeText(this, "Denied: " + denied + ". Please enable in Settings.", Toast.LENGTH_LONG).show();

                // Show dialog to open app settings
                showSettingsDialog();
            }
        }
    }

    private void showSettingsDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("This app needs Bluetooth, Location, Phone, and SMS permissions to work properly. Please enable them in Settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void connectHC05() {
        // Check for Bluetooth permissions based on Android version
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            // Android 12+
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission required. Please grant in settings.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        } else {
            // Below Android 12
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Bluetooth permission required. Please grant in settings.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, 100);
                return;
            }
        }

        // Check if Bluetooth is enabled
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "Please enable Bluetooth", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find HC-05 device
        try {
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                if (device.getName() != null && device.getName().equals(DEVICE_NAME)) {
                    hc05 = device;
                    break;
                }
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
            return;
        }

        if (hc05 == null) {
            Toast.makeText(this, "HC-05 not paired. Please pair in Bluetooth settings", Toast.LENGTH_LONG).show();
            return;
        }

        // Connect in background thread
        new Thread(() -> {
            try {
                btSocket = hc05.createRfcommSocketToServiceRecord(MY_UUID);
                btSocket.connect();
                inputStream = btSocket.getInputStream();
                isConnected = true;

                runOnUiThread(() -> {
                    connectionStatus.setText("● Connected");
                    connectionStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    btnConnect.setEnabled(false);
                    Toast.makeText(this, "HC-05 Connected Successfully", Toast.LENGTH_SHORT).show();
                });

                listenForData();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    connectionStatus.setText("● Connection Failed");
                    connectionStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    Toast.makeText(this, "Failed to connect: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void listenForData() {
        new Thread(() -> {
            byte[] buffer = new byte[256];
            int bytes;

            while (isConnected) {
                try {
                    if (inputStream.available() > 0) {
                        bytes = inputStream.read(buffer);
                        String received = new String(buffer, 0, bytes).trim();

                        runOnUiThread(() -> {
                            if (received.contains("A") || received.contains("ACCIDENT")) {
                                statusText.setText("🚨 ACCIDENT DETECTED!");
                                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                                handleAccident();
                            } else {
                                statusText.setText("✓ Normal Riding");
                                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                            }
                        });
                    }
                    Thread.sleep(100);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    isConnected = false;
                    break;
                }
            }
        }).start();
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationText.setText("📍 Location permission needed");
            return;
        }

        // Request fresh location
        com.google.android.gms.location.LocationRequest locationRequest =
                com.google.android.gms.location.LocationRequest.create()
                        .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setInterval(10000)
                        .setFastestInterval(5000);

        com.google.android.gms.location.LocationCallback locationCallback = new com.google.android.gms.location.LocationCallback() {
            @Override
            public void onLocationResult(com.google.android.gms.location.LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        updateLocationDisplay();
                    }
                }
            }
        };

        // Get last known location first
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
                updateLocationDisplay();
            } else {
                locationText.setText("📍 Acquiring GPS signal...");
            }
        });

        // Request continuous location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private void updateLocationDisplay() {
        runOnUiThread(() -> {
            if (currentLatitude != 0.0 && currentLongitude != 0.0) {
                locationText.setText(String.format("📍 Lat: %.6f, Lng: %.6f", currentLatitude, currentLongitude));
                locationText.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            } else {
                locationText.setText("📍 Acquiring GPS signal...");
                locationText.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
            }
        });
    }

    private void handleAccident() {
        Toast.makeText(this, "ACCIDENT DETECTED! Sending alert...", Toast.LENGTH_LONG).show();
        sendLocationSMS();

        // Wait 3 seconds then make call
        new Thread(() -> {
            try {
                Thread.sleep(3000);
                runOnUiThread(this::makePhoneCall);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendLocationSMS() {
        // Check SMS permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "SMS permission not granted", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 200);
            return;
        }

        if (currentLatitude == 0.0 || currentLongitude == 0.0) {
            Toast.makeText(this, "Location not ready. Please wait for GPS signal...", Toast.LENGTH_LONG).show();

            // Try to get location immediately
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        updateLocationDisplay();
                        Toast.makeText(this, "Location acquired! Try sending again.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return;
        }

        // Create short, simple message
        String googleMapsUrl = "https://maps.google.com/?q=" + currentLatitude + "," + currentLongitude;
        String message = "EMERGENCY! Accident detected. Location: " + googleMapsUrl;

        try {
            SmsManager smsManager = SmsManager.getDefault();

            // Check if message needs to be split
            ArrayList<String> parts = smsManager.divideMessage(message);

            if (parts.size() > 1) {
                // Send as multiple parts
                smsManager.sendMultipartTextMessage(PHONE_NUMBER, null, parts, null, null);
                Toast.makeText(this, "✅ SMS sent in " + parts.size() + " parts to " + PHONE_NUMBER, Toast.LENGTH_LONG).show();
            } else {
                // Send as single message
                smsManager.sendTextMessage(PHONE_NUMBER, null, message, null, null);
                Toast.makeText(this, "✅ SMS sent to " + PHONE_NUMBER, Toast.LENGTH_LONG).show();
            }

            // Log for debugging
            android.util.Log.d("SMS_SEND", "Message: " + message);
            android.util.Log.d("SMS_SEND", "To: " + PHONE_NUMBER);

        } catch (SecurityException e) {
            Toast.makeText(this, "❌ SMS permission denied", Toast.LENGTH_LONG).show();
            android.util.Log.e("SMS_ERROR", "Security exception", e);
        } catch (Exception e) {
            Toast.makeText(this, "❌ SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            android.util.Log.e("SMS_ERROR", "Send failed", e);
        }
    }

    private void makePhoneCall() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Call permission required", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + PHONE_NUMBER));
        startActivity(callIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket != null) {
                isConnected = false;
                btSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}