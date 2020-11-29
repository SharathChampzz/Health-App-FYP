package com.example.geofencing_1;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.karumi.dexter.listener.single.PermissionListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    private static final String TAG = "MainActivity";
    private static final int REQ_PERMISSION = 1010;
    private GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;

    private final int FINE_LOCATION_ACCESS_REQUEST_CODE = 10001;

    private final float GEOFENCE_RADIUS = 10;
    private String GEOFENCE_ID = "SOME_GEOFENCE_ID";
    int ID = 1;

    private String pastlatitude = "", pastlongitude = "";
    static MainActivity instance;
    public static MainActivity getInstance() {
        return instance;
    }

    static Intent makeNotificationIntent(Context geofenceService, String msg)
    {
        Log.d(TAG,msg);
        return new Intent(geofenceService,MainActivity.class);
    }

    TextView textView;
    LocationRequest locationRequest;
    FusedLocationProviderClient fusedLocationProviderClient;

    Button quiz, medication, patients, logout;
    Spinner spinner;
    Switch sw;
    private static final String[] paths = {"Trusted Sites", "WHO", "Corona Positives Nearby", "Other Popular Website"};

    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference("history-locations/" + StorageClass.phno +
            "/" + date);

    DatabaseReference current = FirebaseDatabase.getInstance().getReference("current-locations/");

    String phnum = StorageClass.phno;
    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.cordinates);
        logout = findViewById(R.id.logout);

        quiz = findViewById(R.id.quiz);
        medication = findViewById(R.id.medication);
        patients = findViewById(R.id.patients);
        spinner = findViewById(R.id.spinner);
        Button check =  findViewById(R.id.check);
        sw = findViewById(R.id.switch1);

        instance = this;
//        geofencingClient = LocationServices.getGeofencingClient(this);
//        geofenceHelper = new GeofenceHelper(this);
//        enableUserLocation();

        Intent i = new Intent(MainActivity.this, DBupdateService.class);
        MainActivity.this.startService(i);

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // The toggle is enabled

                } else {
                    // The toggle is disabled
                    Intent i = new Intent(MainActivity.this, DBupdateService.class);
                    MainActivity.this.stopService(i);
                }
            }
        });
/*
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        // check if all permissions are granted
                        if (report.areAllPermissionsGranted()) {
                            // do you work now
                            updateLocation();
                            Log.d(TAG, "Permission Granted Updating Location");
                        }

                        // check for permanent denial of any permission
                        if (report.isAnyPermissionPermanentlyDenied()) {
                            // permission is denied permenantly, navigate user to app settings
                            Toast.makeText(MainActivity.this, "You must Grant Permission to make it work!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<com.karumi.dexter.listener.PermissionRequest> permissions, PermissionToken token) {

                    }


                })
                .onSameThread()
                .check();  */

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        updateLocation();
                        Log.d(TAG, "Permission Granted Updating Location");
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MainActivity.this, "You must Grant Permission to make it work!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(com.karumi.dexter.listener.PermissionRequest permission, PermissionToken token) {

                    }


                }).check();



        check.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MedicationActivity.class));
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
                android.R.layout.simple_spinner_item,paths);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener((AdapterView.OnItemSelectedListener) this);

        quiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), QuizActivity.class));
            }
        });


        medication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                GeofenceHelper geofenceHelper = new GeofenceHelper(getApplicationContext());
                if(StorageClass.phno.equals("990219")){
                    startActivity(new Intent(getApplicationContext(), AdminActivity.class));
                }
                else{
                    Toast.makeText(getApplicationContext(), "Sorry! This Feature is Only For Admin!",Toast.LENGTH_SHORT).show();
                }
            }
        });

        patients.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), MapsActivity.class));
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences =getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.clear();
                editor.apply();
                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                finish();
            }
        });
/*
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("location");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int id = 1;
//                Toast.makeText(getApplicationContext(), "DataChange Found!", Toast.LENGTH_SHORT).show();
                for(DataSnapshot snap: snapshot.getChildren()){
                    LocationHelperDB object = snap.getValue(LocationHelperDB.class);
                    String latitude = object.getLat();
                    String longitude = object.getLon();
//                    Toast.makeText(getApplicationContext(), latitude + "," + longitude, Toast.LENGTH_SHORT).show();
                    LatLng latlng = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                    addGeofence(latlng, GEOFENCE_RADIUS, id);
                   // Toast.makeText(getApplicationContext(), latitude + "," + longitude + ", ID = " + id, Toast.LENGTH_SHORT).show();
                    id += 1;
                    new CountDownTimer(10000, 10000) {

                        @Override
                        public void onTick(long millisUntilFinished) {
                            // do something after 1s

                        }

                        @Override
                        public void onFinish() {
                            // do something end times 5s
                        }

                    }.start();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });  */
    }

    private void enableUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            mMap.setMyLocationEnabled(true);
        } else {
            //Ask for permission
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                //We need to show user a dialog for displaying why the permission is needed and then ask for the permission...
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_ACCESS_REQUEST_CODE);
            }
        }
    }

    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQ_PERMISSION
        );
    }

    private void addGeofence(LatLng latLng, float radius, int id) {
        GEOFENCE_ID = String.valueOf(id);
        Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_ID, latLng, radius, Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
        GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
        PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "onSuccess: Geofence Added... ID = " + GEOFENCE_ID);
//                        Toast.makeText(getApplicationContext(), "Geofence Added" + GEOFENCE_ID, Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String errorMessage = geofenceHelper.getErrorString(e);
                        Log.d(TAG, "onFailure: " + errorMessage);
                        Toast.makeText(getApplicationContext(), "Geofence Failed" + GEOFENCE_ID, Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == FINE_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
//                mMap.setMyLocationEnabled(true);
            } else {
                //We do not have the permission..

            }
        }

        int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;
        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
            } else {
                //We do not have the permission..
                Toast.makeText(this, "Background location access is neccessary for geofences to trigger...", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateLocation() {
        Log.d(TAG, "Calling Build Location Request...");
        buildLocationRequest();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, getPendingIntent());
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, MyLocationService.class);
        intent.setAction(MyLocationService.ACTION_PROCESS_UPDATE);
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }


    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(1f);
    }

    public void updateTextView(String latitude, String longitude){
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(pastlatitude.equals("") || pastlongitude.equals("")){
                    pastlatitude = latitude;
                    pastlongitude= longitude;
                    updatescreen(latitude, longitude);
                }
                else if(distance(Double.parseDouble(pastlatitude), Double.parseDouble(pastlongitude),
                        Double.parseDouble(latitude), Double.parseDouble(longitude)) > 0.010){  // atleast 10m
                    updatescreen(latitude, longitude);
                }

            }
        });
    }

    private void updatescreen(String latitude, String longitude){
        String s = "Current Location : " + latitude + " , " + longitude + "\nYour ID: " + StorageClass.phno;
        textView.setText(s);
        LocationHelper object = new LocationHelper(latitude, longitude);
        if(!StorageClass.phno.equals("")){
            current.child(StorageClass.phno).setValue(object);
        }
        mDatabase.push().setValue(object);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String sSelected=parent.getItemAtPosition(position).toString();
        if( sSelected.equals("WHO")){
            Toast.makeText(this, "WHO site will be opened..!!", Toast.LENGTH_SHORT).show();
//            Intent browserintent = new Intent(Intent.ACTION_VIEW ,
//                    Uri.parse("https://www.who.int/"));
//            startActivity(browserintent);
            Intent i = new Intent(this, WebViewActivity.class);
            i.putExtra("url", "https://www.who.int/");
            startActivity(i);
        }

        else if(sSelected.equals("Corona Positives Nearby")){
//            Toast.makeText(this, "Opening site!", Toast.LENGTH_SHORT).show();
//            Intent browserintent = new Intent(Intent.ACTION_VIEW ,
//                    Uri.parse("https://covidnearyou.org/"));
//            startActivity(browserintent);
            Intent i = new Intent(this, WebViewActivity.class);
            i.putExtra("url", "https://covidnearyou.org/");
            startActivity(i);
        }
        else if(sSelected.equals("Other Popular Website")){
            Toast.makeText(this, "Work Under Progress! We will add more sites.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1))
                * Math.sin(deg2rad(lat2))
                + Math.cos(deg2rad(lat1))
                * Math.cos(deg2rad(lat2))
                * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * 1.1515;
        return (dist);
    }

    private double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }

    private double rad2deg(double rad) {
        return (rad * 180.0 / Math.PI);
    }
}