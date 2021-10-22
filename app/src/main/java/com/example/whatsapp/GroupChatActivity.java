package com.example.whatsapp;

import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;

public class GroupChatActivity extends AppCompatActivity {

    FirebaseAuth mAuth;
    // UserRef : بنجيب بيه اليوزرز وناخد الاسم الموجود لكل يوزر
    // GroupNameRef :ننده بيها الجروب و نعرض بيه الرسايل الموجوده لو فيه و بنبعت ليه كيي تبع كل مستخدم بيكتب رسالة
    // GroupMessageKeyRef : بنخزن فيها بيانات الرسالة لي هنبعتها للجروب
    DatabaseReference UserRef , GroupNameRef , GroupMessageKeyRef;

    ImageButton sendMessageBtn;
    ScrollView scrollView;
    Toolbar toolbar;
    EditText userMessage;
    TextView displayMessage , groupChatTime;
    String currentGroupName,currentUserID,currentUsername,currentDate,currentTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        //we have to get the group name from GroupsFragment and store it in String to pass it to toolbar
        currentGroupName = getIntent().getExtras().get("groupName").toString();

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        GroupNameRef = FirebaseDatabase.getInstance().getReference("Groups").child(currentGroupName);


        InitializeField();

        // currentUserID help to retrieve username from the "Users" then unique userID
        GetUserInfo();

        /*
         If the user click on send message button then we should get message input from edit text
         and first we have to validate either the user has entered something or not then we will
         save message in Database then we will share it to display message
         */
        sendMessageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SaveMessageInfoInDatabase();

                //we need to once user send the message --> empty that edit text
                userMessage.setText("");

                // we need whenever a new message is added then it should scroll automatically so..
                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });

    }

    //whenever we click on group ---> it must display all the previous message first so...
    @Override
    protected void onStart() {
        super.onStart();

        GroupNameRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()){ // if the group is exist
                    DisplayMessages(snapshot);
                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if (snapshot.exists()){ // if the group is exist
                    DisplayMessages(snapshot);
                }
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void InitializeField() {
        sendMessageBtn = findViewById(R.id.group_chat_send_message_ImgBtn);
        scrollView  = findViewById(R.id.group_chat_scroll_view);
        toolbar = findViewById(R.id.group_chat_bar_layout);
        userMessage = findViewById(R.id.group_chat_message_input);
        displayMessage = findViewById(R.id.group_chat_text_display_tv);
//        groupChatTime = findViewById(R.id.group_chat_time);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(currentGroupName);
    }

    private void GetUserInfo() {
        UserRef.child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // we will check if this user exist then we can proceed
                if (snapshot.exists()){
                    currentUsername = snapshot.child("name").getValue().toString();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    private void SaveMessageInfoInDatabase() {
        //get text from input field
        String message = userMessage.getText().toString();
        String messageKey = GroupNameRef.push().getKey();

        // we have to validate
        if (TextUtils.isEmpty(message)){
            Toast.makeText(this, "Please write message first...", Toast.LENGTH_SHORT).show();
        }
        else {
            //get the current date & time when the user sent message

            //date
            Calendar callForDate = Calendar.getInstance();
            SimpleDateFormat currentDateFormat = new SimpleDateFormat("MMM dd, yyyy");
            currentDate = currentDateFormat.format(callForDate.getTime());
            //time
            Calendar callForTime = Calendar.getInstance();
            SimpleDateFormat currentTimeFormat = new SimpleDateFormat("hh:mm a");
            currentTime = currentTimeFormat.format(callForTime.getTime());

            //now we need to save it inside the firebase database alongside with the other message information
            HashMap<String,Object> groupMessageKey = new HashMap<>();
            GroupNameRef.updateChildren(groupMessageKey);

            //we need to get the reference to that message key and we will store that in GroupMessageKeyRef
            GroupMessageKeyRef = GroupNameRef.child(messageKey);

            //now we are have to store message data by using GroupMessageKeyRef
            HashMap<String,Object> messageInfoMap = new HashMap<>();
            messageInfoMap.put("name",currentUsername);
            messageInfoMap.put("message",message);
            messageInfoMap.put("date",currentDate);
            messageInfoMap.put("time",currentTime);
            //now let's save message data
            GroupMessageKeyRef.updateChildren(messageInfoMap);

        }

    }


    private void DisplayMessages(DataSnapshot snapshot) {
        //here we will retrieve and display all messages for each specific group

        //it will move line by line and will get each message for each specific group
        Iterator iterator = snapshot.getChildren().iterator();
        while (iterator.hasNext()){
            String ChatDate = (String) ((DataSnapshot)iterator.next()).getValue();
            String ChatMessage = (String) ((DataSnapshot)iterator.next()).getValue();
            String ChatName= (String) ((DataSnapshot)iterator.next()).getValue();
            String ChatTime = (String) ((DataSnapshot)iterator.next()).getValue();
            displayMessage.append(ChatName + ":\n" + ChatMessage + "\n" +ChatTime + "     " + ChatDate + "\n\n\n");

            // we need whenever a new message is added then it should scroll automatically so..
            scrollView.fullScroll(ScrollView.FOCUS_DOWN);

        }

    }
}