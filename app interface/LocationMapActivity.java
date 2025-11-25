package com.example.ridergurdianx;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;

public class LocationMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final int REQ_LOCATION = 101;
    private static final String TAG = "LocationMapActivity";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;

    private ImageView btnBack;
    private ImageButton btnMyLocation;
    private ImageButton btnDirections;
    private AutoCompleteTextView etDestination;
    private Button btnStartNav;
    private TextView tvRouteSummary, tvRouteDetails;

    private LatLng currentLatLng;
    private LatLng destLatLng;
    private Polyline currentPolyline;

    // TODO: put your real API key here (same key you use for Maps/Places/Directions)
    private static final String DIRECTIONS_API_KEY = "AIzaSyC81hh9_nDMabknbbkbD2juPe53XlaVM5o";

    // Places autocomplete
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private ArrayAdapter<String> autoCompleteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_map);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        initPlacesAutocomplete();
        setupListeners();
        initMap();
    }

    private void initViews() {
        btnBack        = findViewById(R.id.btnBack);
        btnMyLocation  = findViewById(R.id.btnMyLocation);
        btnDirections  = findViewById(R.id.btnDirections);
        etDestination  = findViewById(R.id.etDestination);
        btnStartNav    = findViewById(R.id.btnStartNav);
        tvRouteSummary = findViewById(R.id.tvRouteSummary);
        tvRouteDetails = findViewById(R.id.tvRouteDetails);
    }

    // ------------------ Places Autocomplete ------------------------

    private void initPlacesAutocomplete() {
        // Initialize Places SDK (only once)
        if (!Places.isInitialized()) {
            Places.initialize(
                    getApplicationContext(),
                    DIRECTIONS_API_KEY    // same API key
            );
        }
        placesClient = Places.createClient(this);
        sessionToken = AutocompleteSessionToken.newInstance();

        autoCompleteAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line
        );
        etDestination.setAdapter(autoCompleteAdapter);

        etDestination.setThreshold(1); // start suggesting from 1 character

        etDestination.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString();
                if (query.length() < 2) {
                    return;
                }
                fetchPlaceSuggestions(query);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void fetchPlaceSuggestions(String query) {
        FindAutocompletePredictionsRequest request =
                FindAutocompletePredictionsRequest.builder()
                        .setSessionToken(sessionToken)
                        .setQuery(query)
                        .setCountry("LK")              // limit to Sri Lanka; remove if you want global
                        .setTypeFilter(TypeFilter.ADDRESS)
                        .build();

        placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener((FindAutocompletePredictionsResponse response) -> {
                    autoCompleteAdapter.clear();
                    for (AutocompletePrediction prediction : response.getAutocompletePredictions()) {
                        String suggestion = prediction.getFullText(null).toString();
                        autoCompleteAdapter.add(suggestion);
                    }
                    autoCompleteAdapter.notifyDataSetChanged();
                    if (!etDestination.isPopupShowing()) {
                        etDestination.showDropDown();
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Places autocomplete error", e));
    }

    // ------------------ Listeners ------------------------

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        btnMyLocation.setOnClickListener(v -> moveCameraToCurrentLocation());

        btnDirections.setOnClickListener(v -> {
            String destText = etDestination.getText().toString().trim();
            if (TextUtils.isEmpty(destText)) {
                Toast.makeText(this, "Enter a destination", Toast.LENGTH_SHORT).show();
                return;
            }
            if (currentLatLng == null) {
                Toast.makeText(this, "Current location not ready yet", Toast.LENGTH_SHORT).show();
                return;
            }
            // Geocode the chosen text & get route
            geocodeDestinationAndRoute(destText);
        });

        // For now Start button just recenters the map on the route.
        btnStartNav.setOnClickListener(v -> {
            if (destLatLng != null && mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 15f));
                Toast.makeText(this, "Navigation started (demo)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Set a route first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ------------------ Map init ------------------------

    private void initMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, "Map fragment not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
        } else {
            enableMyLocation();
        }
    }

    private void enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                        mMap.addMarker(new MarkerOptions()
                                .position(currentLatLng)
                                .title("You are here")
                                .icon(BitmapDescriptorFactory.defaultMarker(
                                        BitmapDescriptorFactory.HUE_AZURE)));
                    }
                });
    }

    private void moveCameraToCurrentLocation() {
        if (currentLatLng != null && mMap != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
        } else {
            Toast.makeText(this, "Current location not ready", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------ Destination & Directions ------------------------

    private void geocodeDestinationAndRoute(String destText) {
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocationName(destText, 1);
                if (list == null || list.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "Destination not found", Toast.LENGTH_SHORT).show());
                    return;
                }

                Address address = list.get(0);
                destLatLng = new LatLng(address.getLatitude(), address.getLongitude());

                runOnUiThread(() -> {
                    if (mMap != null) {
                        mMap.addMarker(new MarkerOptions()
                                .position(destLatLng)
                                .title("Destination"));
                    }
                    requestDirections();
                });

            } catch (Exception e) {
                Log.e(TAG, "Geocoding error", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Error finding destination", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void requestDirections() {
        if (currentLatLng == null || destLatLng == null) return;

        String origin = currentLatLng.latitude + "," + currentLatLng.longitude;
        String dest   = destLatLng.latitude + "," + destLatLng.longitude;

        String url = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + origin
                + "&destination=" + dest
                + "&mode=driving"
                + "&key=" + DIRECTIONS_API_KEY;

        new DirectionsTask().execute(url);
    }

    // ------------------ AsyncTask to call Directions API ------------------------

    private class DirectionsTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... strings) {
            String data = "";
            HttpURLConnection connection = null;
            InputStream stream = null;

            try {
                URL url = new URL(strings[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                stream = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(stream));
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                data = sb.toString();
                br.close();

            } catch (Exception e) {
                Log.e(TAG, "Directions API error", e);
            } finally {
                try {
                    if (stream != null) stream.close();
                    if (connection != null) connection.disconnect();
                } catch (Exception ignored) { }
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null || result.isEmpty()) {
                Toast.makeText(LocationMapActivity.this,
                        "Failed to get directions", Toast.LENGTH_SHORT).show();
                return;
            }
            drawRouteFromJson(result);
        }
    }

    private void drawRouteFromJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONArray routes = obj.getJSONArray("routes");
            if (routes.length() == 0) {
                Toast.makeText(this, "No route found", Toast.LENGTH_SHORT).show();
                return;
            }

            JSONObject route = routes.getJSONObject(0);
            JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
            String points = overviewPolyline.getString("points");

            // Distance & duration
            JSONArray legs = route.getJSONArray("legs");
            JSONObject leg = legs.getJSONObject(0);
            String distanceText = leg.getJSONObject("distance").getString("text");
            String durationText = leg.getJSONObject("duration").getString("text");

            // Decode the polyline
            List<LatLng> decodedPath = PolyUtil.decode(points);

            if (currentPolyline != null) currentPolyline.remove();
            currentPolyline = mMap.addPolyline(new PolylineOptions()
                    .addAll(decodedPath)
                    .width(12f)
                    .color(0xFF00BFA5)   // teal
                    .geodesic(true));

            tvRouteSummary.setText("Route ready");
            tvRouteDetails.setText("Distance: " + distanceText + "   ETA: " + durationText);

            // Fit camera to route start
            if (!decodedPath.isEmpty()) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(decodedPath.get(0), 13f));
            }

        } catch (Exception e) {
            Log.e(TAG, "Parse directions error", e);
            Toast.makeText(this, "Failed to parse route", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------ Permission callback ------------------------

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION &&
                grantResults.length > 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            enableMyLocation();
        } else {
            Toast.makeText(this,
                    "Location permission is required to show your position",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
