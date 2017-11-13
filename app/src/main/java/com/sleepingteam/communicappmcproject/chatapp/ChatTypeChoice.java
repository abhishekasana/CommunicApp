package com.sleepingteam.communicappmcproject.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.auth.FirebaseAuth;
import com.sleepingteam.communicappmcproject.R;

public class ChatTypeChoice extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_type_choice);
        Button personalChat = (Button) findViewById(R.id.personalchat);
        Button groupChat = (Button) findViewById(R.id.groupchat);
        Button logout = (Button) findViewById(R.id.logout);
        final EditText email = (EditText) findViewById(R.id.buddyEmail);
        groupChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatTypeChoice.this,MainChatActivity.class);
                intent.putExtra("EMAIL_ID", "");
                startActivity(intent);
            }
        });
        personalChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(TextUtils.isEmpty(email.getText().toString())){
                    email.setError(getString(R.string.error_field_required));
                }else{
                    Intent intent = new Intent(ChatTypeChoice.this,PersonalChat.class);
                    intent.putExtra("EMAIL_ID", email.getText().toString());
                    startActivity(intent);}
            }
        });

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    FirebaseAuth.getInstance().signOut();
                    startActivity(new Intent(ChatTypeChoice.this, LoginActivity.class)); //Go back to home page
                    finish();
            }
        });
    }
}
