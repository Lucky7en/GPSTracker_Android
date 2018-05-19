package com.example.vitalyusov.tracker;

import android.support.v7.app.AppCompatActivity;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.view.View;
import android.util.Log;
import android.widget.Toast;
import android.content.pm.PackageManager;

import android.Manifest;
import android.content.Intent;
import android.widget.TextView;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TrackingActivity extends AppCompatActivity{

    private FirebaseFirestore mFirestore;
    private Query mQuery;
    private TextView welcomeLabel;
    private TextView emailLabel;
    private TextView trackingLabel;
    private String coordinates;
    private String email = "testemail@gmail.com";
    public  static final int RequestPermissionCode  = 1 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);
        welcomeLabel = findViewById(R.id.welcomeLabel);
        trackingLabel = findViewById(R.id.trackingLabel);
        emailLabel = findViewById(R.id.emailLabel);
        EnableRuntimePermission();
        startService(new Intent(this,LocationService .class));
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        //email = user.getEmail();
        if (user != null) {
            // User is signed in
            emailLabel.setText(email);
        } else {
        // No user is signed in
    }
    }

    public void EnableRuntimePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(TrackingActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION))
        {

            Toast.makeText(TrackingActivity.this,"ACCESS_FINE_LOCATION permission allows us to Access GPS in app", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(TrackingActivity.this,new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION}, RequestPermissionCode);

        }
    }
    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {

        switch (RC) {

            case RequestPermissionCode:

                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(TrackingActivity.this,"Permission Granted, Now your application can access GPS.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(TrackingActivity.this,"Permission Canceled, Now your application cannot access GPS.", Toast.LENGTH_LONG).show();

                }
                break;
        }
    }
}
