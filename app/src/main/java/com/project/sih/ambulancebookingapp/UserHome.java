package com.project.sih.ambulancebookingapp;

import android.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserHome extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReferenceRoot;
    SharedPreferences sharedPreferences;
    String userId;
    List<LatLng> markers;
    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_home);

        toolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        initMap();

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReferenceRoot = firebaseDatabase.getReference();
        sharedPreferences = getSharedPreferences(getResources().getString(R.string.FILE_NAME_KEY), MODE_PRIVATE);
        userId = sharedPreferences.getString(getResources().getString(R.string.USER_ID_KEY), "");

        databaseReferenceRoot.child("Online").child(userId).setValue(true);
        ValueEventListener changedListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataSnapshot statusChild = dataSnapshot.child("Online"); // The online child of the root
                DataSnapshot usersChild = dataSnapshot.child("Users"); // the users child of the root
                List<String> keys = new ArrayList<>();
                markers = new ArrayList<>();

                for(DataSnapshot dataSnapshot1 : statusChild.getChildren()) {
                    keys.add((String)dataSnapshot1.getKey());
                }

                for(DataSnapshot dataSnapshot1 : usersChild.getChildren()) {
                    if(keys.contains(dataSnapshot1.getKey()) && (String)dataSnapshot1.child("category").getValue() != null
                            &&((String)dataSnapshot1.child("category").getValue()).equals("driver")) {
                        Double lat = (Double)dataSnapshot1.child("lat").getValue(), lng = (Double) dataSnapshot1.child("lng").getValue();
                        markers.add(new LatLng(lat, lng));
                    }
                }
                for(LatLng latLng : markers)
                    addMarker(latLng);
                moveCamera();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReferenceRoot.addValueEventListener(changedListener);
    }

    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        setupGoogleMapScreenSettings(mMap);
    }

    private void addMarker(LatLng latLng) {
        mMap.addMarker(new MarkerOptions().position(latLng).title(formAddress(latLng)).icon(BitmapDescriptorFactory.fromResource(
                R.drawable.ambulance_marker
        )));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if(id == R.id.logout_menu) {
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            firebaseAuth.signOut();
            startActivity(new Intent(UserHome.this, MainActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    private void moveCamera() {
        double avgLat = 0, avgLng = 0;
        for(LatLng latLng : markers) {
            avgLat += latLng.latitude;
            avgLng += latLng.longitude;
        }
        avgLat /= markers.size();
        avgLng /= markers.size();

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(avgLat, avgLng), 8));
    }

    private String formAddress(LatLng latLng) {
        Geocoder geocoder = new Geocoder(UserHome.this);
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            Toast.makeText(UserHome.this, ""+e.toString(), Toast.LENGTH_LONG).show();
        }
        String address = addresses.get(0).getAddressLine(0);
        String city = addresses.get(0).getAddressLine(1);
        String country = addresses.get(0).getAddressLine(2);
        return address+", " + city+", "+country;
    }

    private void setupGoogleMapScreenSettings(GoogleMap mMap) {
        mMap.setBuildingsEnabled(true);
        mMap.setIndoorEnabled(true);
        mMap.setTrafficEnabled(true);
        UiSettings mUiSettings = mMap.getUiSettings();
        mUiSettings.setZoomControlsEnabled(true);
        mUiSettings.setCompassEnabled(true);
        mUiSettings.setMyLocationButtonEnabled(true);
        mUiSettings.setScrollGesturesEnabled(true);
        mUiSettings.setZoomGesturesEnabled(true);
        mUiSettings.setTiltGesturesEnabled(true);
        mUiSettings.setRotateGesturesEnabled(true);
    }
}
