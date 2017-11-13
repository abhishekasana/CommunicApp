package com.sleepingteam.communicappmcproject.chatapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sleepingteam.communicappmcproject.R;

public class PersonalChat extends AppCompatActivity {

    private String mDisplayName;
    private String mEmail;
    private ListView mChatListView;
    private EditText mInputText;
    private ImageButton mSendButton;

    private DatabaseReference mDatabaseRefrence;
    private DatabaseReference mchildDatabaseRefrence;
    private DatabaseReference mUserChatDatabaseReference;

    private boolean selectionCheck = false;

    private static final String TAG = "PersonalChat";

    private ChatListAdapter mAdapter;

    MyDatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_chat);

        setupDisplayName();
        String email_id = getIntent().getStringExtra("EMAIL_ID");
        mDatabaseRefrence = FirebaseDatabase.getInstance().getReference();
        int index = mEmail.indexOf('@');
        mEmail = mEmail.substring(0,index);

        index = email_id.indexOf('@');
        email_id = email_id.substring(0,index);

        String dbTagOne = mEmail+email_id;
        String dbTagTwo = email_id+mEmail;
        final String temp = dbTagOne;

//        Toast.makeText(getApplicationContext(),dbTagTwo,Toast.LENGTH_LONG).show();
        mchildDatabaseRefrence = mDatabaseRefrence.child("personalchat");
        mUserChatDatabaseReference = mchildDatabaseRefrence.child("blaa");

        mchildDatabaseRefrence.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.hasChild(temp)) {
                    selectionCheck = true;
//                    mUserChatDatabaseReference = mchildDatabaseRefrence.child(dbTagOne);
                    // run some code
                }else{
                    selectionCheck = false;
//                    mUserChatDatabaseReference = mchildDatabaseRefrence.child(dbTagTwo);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if(!selectionCheck){
            mUserChatDatabaseReference = mchildDatabaseRefrence.child(dbTagOne);
        }else{
            mUserChatDatabaseReference = mchildDatabaseRefrence.child(dbTagTwo);
        }

//        Toast.makeText(getApplicationContext(),"hey "+mUserChatDatabaseReference.child("messages")..getKey(),Toast.LENGTH_LONG).show();
//        mUserChatDatabaseReference.child("messages").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                for (DataSnapshot snap: dataSnapshot.getChildren()) {
////                    Log.e(snap.getKey(),snap.getChildrenCount() + "");
////                    Toast.makeText(getApplicationContext(),"Count is "+snap.getChildrenCount(),Toast.LENGTH_LONG).show();
//                    selectionCheck = true;
//                }
//            }
//
//            @Override
//            public void onCancelled(DatabaseError databaseError) {
//                Toast.makeText(getApplicationContext(),"cancelllll ",Toast.LENGTH_LONG).show();
//            }
//
//        });
//        if(!selectionCheck){
//            mUserChatDatabaseReference = mchildDatabaseRefrence.child(dbTagTwo);
//        }



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


    }

    private void setupDisplayName(){
        SharedPreferences prefs = getSharedPreferences(RegisterActivity.CHAT_PREFS, MODE_PRIVATE);

        mDisplayName = prefs.getString(RegisterActivity.DISPLAY_NAME_KEY, null);
        mEmail = prefs.getString("user_email",null);

        if(mDisplayName == null) mDisplayName = "Anonymous";

    }

    private void sendMessage() {

        String input = mInputText.getText().toString();
        if(!input.equals("")){
//            String encryptedMessage = "";
            String key = mDisplayName;
//            try {
//                encryptedMessage = Crypt.encrypt(key, input);
//            } catch (GeneralSecurityException e) {
//                Log.d("crypt", e.toString());
//            }
            InstantMessage chat = new InstantMessage(input, mDisplayName);
            mUserChatDatabaseReference.child("messages").push().setValue(chat);
            mInputText.setText("");
        }

    }


    @Override
    protected void onStart() {
        super.onStart();
        mAdapter = new ChatListAdapter(this, mUserChatDatabaseReference, mDisplayName);
        mChatListView.setAdapter(mAdapter);
    }

    @Override
    public void onStop() {
        super.onStop();
        mAdapter.cleanup();
    }
}

