package com.sleepingteam.communicappmcproject.chatapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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


public class MainChatActivity extends AppCompatActivity {

    private String mDisplayName;
    private ListView mChatListView;
    private EditText mInputText;
    private ImageButton mSendButton;

    private DatabaseReference mDatabaseRefrence;

    private ChatListAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_chat);

        setupDisplayName();
        mDatabaseRefrence = FirebaseDatabase.getInstance().getReference();


        // Link the Views in the layout to the Java code
        mInputText = (EditText) findViewById(R.id.messageInput);
        mSendButton = (ImageButton) findViewById(R.id.sendButton);
        mChatListView = (ListView) findViewById(R.id.chat_list_view);

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
                wifidirectclick(v);
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


    @Override
    protected void onStart() {
        super.onStart();
        mAdapter = new ChatListAdapter(this, mDatabaseRefrence, mDisplayName);
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
}
