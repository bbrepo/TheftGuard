package com.example.theftguard;

import android.Manifest;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class SetupActivity extends AppCompatActivity {

    //Device Policy Receiver related variables
    ComponentName cn;
    DevicePolicyManager dpm;
    CheckBox AdminEnabledCheckbox;

    //Location service related variables
    public static double latitude,longitude;
    LocationManager locationManager;
    TextView txtLat;

    private static final long LOCATION_REFRESH_TIME = 1000;
    private static final long LOCATION_REFRESH_DISTANCE = 10;
    //private static final int MY_PERMISSION_ACCESS_COURSE_LOCATION = 11;
    private static final int MY_PERMISSION_ACCESS_FINE_LOCATION = 12;

    private final LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(final Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();

            String latitude_s = Double.toString(latitude);
            String longitude_s = Double.toString(longitude);

            txtLat = (TextView) findViewById(R.id.textview_location);
            txtLat.setText(String.format("Latitude:%s, Longitude:%s", latitude_s, longitude_s));

            Log.i("Latitude", "location updated: "+ latitude_s + ", " + longitude_s);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.i("Latitude", "disabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.i("Latitude", "enabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            String msg = String.format("status: %s", status);
            Log.i("Latitude", msg);
        }
    };

    /* ========================== Methods ================================ */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        //Device Policy Receiver
        cn = new ComponentName(this, AdminReceiver.class);
        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        AdminEnabledCheckbox = (CheckBox) findViewById(R.id.checkBox1);

        //init location service
        txtLat = (TextView) findViewById(R.id.textview_location);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //check location permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(
                    this,
                    new String [] { android.Manifest.permission.ACCESS_FINE_LOCATION },
                    MY_PERMISSION_ACCESS_FINE_LOCATION
            );
        }

        //get location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                LOCATION_REFRESH_TIME,
                LOCATION_REFRESH_DISTANCE,
                mLocationListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        AdminEnabledCheckbox.setChecked(dpm.isAdminActive(cn));
        AdminEnabledCheckbox.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        if (isChecked) {
                            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, cn);
                            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                    getString(R.string.device_admin_explanation));
                            startActivity(intent);
                        }
                        else {
                            dpm.removeActiveAdmin(cn);
                        }
                    }
                });

//        //get location updates, not sure if its needed here
//        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
//                LOCATION_REFRESH_TIME,
//                LOCATION_REFRESH_DISTANCE,
//                mLocationListener);

    }

    /* ========= Device Admin Receiver ========= */

    public static class AdminReceiver extends DeviceAdminReceiver {

        @Override
        public void onEnabled(Context context, Intent intent) {
            Toast.makeText(context, context.getString(R.string.admin_receiver_status_enabled),
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onDisabled(Context context, Intent intent) {
            Toast.makeText(context, context.getString(R.string.admin_receiver_status_disabled),
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
            if (!Process.myUserHandle().equals(user)) {
                // This password expiration was on another user, for example a parent profile. Skip it.
                return;
            }
            Toast.makeText(context, context.getString(R.string.admin_receiver_status_pw_failed),
                    Toast.LENGTH_LONG).show();

            //location
            String latitude_s = Double.toString(latitude);
            String longitude_s = Double.toString(longitude);
            Log.i("Latitude", "Password Failed location: "+ latitude_s + ", " + longitude_s);

            //photo
            //init takePictureIntent
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE);
            takePictureIntent.putExtra("android.intent.extra.quickCapture",true);
            takePictureIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            //saving photo
            String currentPhotoPath;
            File storageDir;
            File image;
            Map<String,Object> map=new HashMap<>();

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            try {
                image = File.createTempFile(
                        imageFileName,  /* prefix */
                        ".jpg",         /* suffix */
                        storageDir      /* directory */
                );

                // Save a file: path for use with ACTION_VIEW intents
                currentPhotoPath = image.getAbsolutePath();
                Log.i("Path:",currentPhotoPath);
                map.put("purl",currentPhotoPath);
                Uri photoURI = FileProvider.getUriForFile(context,
                        BuildConfig.APPLICATION_ID + ".provider",
                        image);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            }
            catch (IOException e) {
                e.printStackTrace();
                Log.i("takePicture", "IOException: Failed to save the photo");
            }

            //start intent
            try {
                context.startActivity(takePictureIntent);
                Log.i("takePicture", "TakePictureIntent succeeded");

            }
            catch (ActivityNotFoundException e) {
                // display error state to the user
                Toast.makeText(context, "Failed to take a photo", Toast.LENGTH_LONG).show();
                Log.i("takePicture", "Failed to start takePictureIntent");
            }

            //push location and image

            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            System.out.println(timestamp);


            map.put("latitude",latitude_s);
            map.put("longitude",longitude_s);
            map.put("time",timestamp.toString());


            FirebaseDatabase.getInstance().getReference(FirebaseAuth.getInstance().getCurrentUser().getUid()).child("location").push()
                    .setValue(map)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(context.getApplicationContext(),"Inserted Successfully",Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            Toast.makeText(context.getApplicationContext(),"Could not insert",Toast.LENGTH_LONG).show();
                        }
                    });


        }

        @Override
        public void onPasswordSucceeded(Context context, Intent intent, UserHandle user){
            if (!Process.myUserHandle().equals(user)) {
                // This password expiration was on another user, for example a parent profile. Skip it.
                return;
            }
            Toast.makeText(context, context.getString(R.string.admin_receiver_status_pw_succeeded),
                    Toast.LENGTH_LONG).show();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("DevicePolicyReceiver", "MyDevicePolicyReceiver Received: " + intent.getAction());
            super.onReceive(context, intent);
        }

    }

    /* ========= Photo helper ========= */



}



