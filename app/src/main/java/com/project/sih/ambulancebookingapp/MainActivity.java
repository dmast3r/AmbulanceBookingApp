package com.project.sih.ambulancebookingapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import android.widget.Toolbar;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String IS_NEW_USER_KEY = "com.project.sih.ambulancebookingapp.IsNewUser";
    private boolean isNewUser;
    private SharedPreferences sharedPreferences;

    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReferenceRoot;
    private String userId;

    private static String[] permissions = {android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};
    private static boolean locationPermissionGranted = false;
    private static FusedLocationProviderClient fusedLocationProviderClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sharedPreferences = getSharedPreferences(getResources().getString(R.string.FILE_NAME_KEY), MODE_PRIVATE);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReferenceRoot = firebaseDatabase.getReference();



        if (firebaseUser == null) {
            List<AuthUI.IdpConfig> providers = Arrays.asList(new AuthUI.IdpConfig.EmailBuilder()/*.setDefaultCountryIso("IN")*/.build());
            startActivityForResult(AuthUI.getInstance()
                    .createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build(), RC_SIGN_IN);
        }

        else {
            userId = firebaseAuth.getUid();
            getDeviceLocation();
            if(sharedPreferences.getString(getResources().getString(R.string.CATEGORY_KEY), "").equals("user")) {
                Intent intent = new Intent(MainActivity.this, UserHome.class);
                startActivity(intent);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == RC_SIGN_IN) {
            if(resultCode == RESULT_OK) {
                firebaseUser = firebaseAuth.getCurrentUser();
                userId = firebaseUser.getUid();
                String userPhone = firebaseUser.getEmail();
                Intent intent = null;

                String logined_before_key = getResources().getString(R.string.LOGINED_BEFORE_KEY);
                if(!sharedPreferences.getBoolean(logined_before_key, false)) {
                    // store some data locally
                    SharedPreferences.Editor sharedEditor = sharedPreferences.edit();
                    sharedEditor.putBoolean(logined_before_key, true);
                    sharedEditor.putString(getResources().getString(R.string.NUMBER_KEY), userPhone);
                    sharedEditor.putString(getResources().getString(R.string.USER_ID_KEY), userId);
                    sharedEditor.apply();

                    databaseReferenceRoot.child("Users").child(userId).child("Phone Number").setValue(userPhone);
                    getDeviceLocation();
                    intent = new Intent(MainActivity.this, SetUpProfileActivity.class);
                }

                else {
                    String category = sharedPreferences.getString(getResources().getString(R.string.CATEGORY_KEY), "");
                    if(category.equals("user"));
                        // Todo later
                    else;
                        // Todo later
                }
                startActivity(intent);
            }
            else {
                Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_LONG).show();
            }
        }
    }


    private void getLocationPermission() {
        Dexter.withActivity(this).withPermissions(permissions).withListener(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if(report.areAllPermissionsGranted()) {
                    locationPermissionGranted = true;
                }
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.cancelPermissionRequest();
            }
        }).onSameThread().check();
    }

    private void getDeviceLocation() {
        getLocationPermission();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        // use try-catch to handle Security exception, which will be thrown if user denies access.
        try {
            if(locationPermissionGranted) {
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()) {
                            Location currentLocation = (Location)task.getResult();
                            LatLng destination = null;
                            if(currentLocation != null)
                                destination = new LatLng(currentLocation.getLatitude(),
                                    currentLocation.getLongitude());
                            Toast.makeText(MainActivity.this, "Inside addcur.." + destination, Toast.LENGTH_LONG).show();
                            if(destination != null) {
                                databaseReferenceRoot.child("Users").child(userId).child("lat").setValue(destination.latitude);
                                databaseReferenceRoot.child("Users").child(userId).child("lat").setValue(destination.longitude);
                            }
                        }
                        else {
                            Toast.makeText(MainActivity.this, "task failed", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        } catch (SecurityException securityException) {
            Toast.makeText(MainActivity.this, "Security Excpetion, can't init current location", Toast.LENGTH_LONG).show();
        }
    }
}
