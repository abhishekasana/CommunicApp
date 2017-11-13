package com.sleepingteam.communicappmcproject.chatapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;

import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.sleepingteam.communicappmcproject.R;
import com.sleepingteam.communicappmcproject.wifidirect.wifi.WiFiDirectActivity;

import java.security.GeneralSecurityException;

import static com.sleepingteam.communicappmcproject.R.id.fab;


public class MainChatActivity extends AppCompatActivity implements LocationListener {

    private static String mDisplayName;
    private ListView mChatListView;
    private EditText mInputText;
    private ImageButton mSendButton;

    private DatabaseReference mDatabaseRefrence;
    private ChatListAdapter mAdapter;
    private LocationManager locationManager;
    private Context mContext;

    private double mLat;
    private double mLong;

    private String email_id = "";

    MyDatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        setupDisplayName();
        mDatabaseRefrence = FirebaseDatabase.getInstance().getReference();

        mContext = getApplicationContext();
        locationManager = (LocationManager) mContext
                .getSystemService(Context.LOCATION_SERVICE);
      
        email_id = getIntent().getStringExtra("EMAIL_ID");

        FloatingActionButton fab_share = (FloatingActionButton) findViewById(fab);
        fab_share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Click action
                wifidirectclick(view);
            }
        });

        // Link the Views in the layout to the Java code
        mInputText = (EditText) findViewById(R.id.messageInput);
        mSendButton = (ImageButton) findViewById(R.id.sendButton);
        mChatListView = (ListView) findViewById(R.id.chat_list_view);

        databaseHelper = new MyDatabaseHelper(this,null,null,1);

        mInputText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                sendMessage();
                return true;
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
        ImageButton mShareButton = (ImageButton) findViewById(R.id.wifidirectredirect);
        mShareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Have to add a button in the UI for Location sharing
                sendLocationMessage();
//                wifidirectclick(v);
            }
        });


    }

    private void setupDisplayName(){
        SharedPreferences prefs = getSharedPreferences(RegisterActivity.CHAT_PREFS, MODE_PRIVATE);

        mDisplayName = prefs.getString(RegisterActivity.DISPLAY_NAME_KEY, null);

        if(mDisplayName == null) mDisplayName = "Anonymous";

    }

    private void sendMessage() {

        String input = mInputText.getText().toString();

        if(!input.equals("")){
            String encryptedMessage = "";
            String key = mDisplayName;
            try {
                encryptedMessage = Crypt.encrypt(key, input);
            } catch (GeneralSecurityException e) {
                Log.d("crypt", e.toString());
            }
            InstantMessage chat = new InstantMessage(encryptedMessage, mDisplayName);
            mDatabaseRefrence.child("messages").push().setValue(chat);
            mInputText.setText("");
        }

    }

    public static String getUsername() {
        return mDisplayName;
    }

    private void sendLocationMessage() {
        String googleMapsURL = "";
        boolean isGPSEnable = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (isGPSEnable) {
            if (locationManager != null) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(mContext, "No permissions", Toast.LENGTH_SHORT).show();
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1, 1, this);
                double latitude, longitude;
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (location == null) {
                    Snackbar.make(mChatListView, "Problem in identifying location", Snackbar.LENGTH_LONG).show();
                    latitude = mLat;
                    longitude = mLong;
                } else {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }
                locationManager.removeUpdates(this);
                Toast.makeText(mContext, "Lat: " + latitude + ", Long: " + longitude,
                        Toast.LENGTH_LONG).show();
                googleMapsURL = "https://www.google.com/maps/search/?api=1&query=" + latitude +
                        ", " + longitude;
                String encryptedMessage = "";
                String key = mDisplayName;
                try {
                    encryptedMessage = Crypt.encrypt(key, googleMapsURL);
                } catch (GeneralSecurityException e) {
                    Log.d("crypt", e.toString());
                }
                InstantMessage chat = new InstantMessage(encryptedMessage, mDisplayName);
                mDatabaseRefrence.child("messages").push().setValue(chat);
            }
        } else {
//            Toast.makeText(mContext, "GPS is not enabled", Toast.LENGTH_LONG).show();
            Snackbar.make(mChatListView, "GPS is not enabled", Snackbar.LENGTH_LONG).show();
            if (isNetworkEnabled) {
                if (locationManager != null) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1, 10, this);
                    double latitude, longitude;
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    locationManager.removeUpdates(this);
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Toast.makeText(mContext, "Lat: " + latitude + ", Long: " + longitude,
                            Toast.LENGTH_LONG).show();
                    googleMapsURL = "https://www.google.com/maps/search/?api=1&query=" + latitude +
                            ", " + longitude;
                    String encryptedMessage = "";
                    String key = mDisplayName;
                    try {
                        encryptedMessage = Crypt.encrypt(key, googleMapsURL);
                    } catch (GeneralSecurityException e) {
                        Log.d("crypt", e.toString());
                    }
                    InstantMessage chat = new InstantMessage(encryptedMessage, mDisplayName);
                    mDatabaseRefrence.child("messages").push().setValue(chat);
                }
            } else {
                Snackbar.make(mChatListView, "Network location is not available", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        if(!email_id.equals("")){
//            mAdapter = new ChatListAdapter(this, mDatabaseRefrence, mDisplayName, email_id);
//        }else{
            mAdapter = new ChatListAdapter(this, mDatabaseRefrence, mDisplayName);
//        }
        mChatListView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.cleanup();
    }

    public void wifidirectclick(View view) {
        Intent intent = new Intent(getApplicationContext(), WiFiDirectActivity.class);
        Toast.makeText(getApplicationContext(),"Opening file sharing module",Toast.LENGTH_LONG).show();
        startActivity(intent);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLat = location.getLatitude();
        mLong = location.getLongitude();
}

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }
}
