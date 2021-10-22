package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserID ,senderUserID, current_state;

    private CircleImageView profile_iv;
    private TextView username_tv, userStatus_tv;
    private Button sendMessage_btn,cancelMessage_btn;

    private DatabaseReference UserRef, ChatRequestRef, ContactsRef, NotificationRef;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        UserRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ChatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        mAuth = FirebaseAuth.getInstance();
        NotificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        //senderUserID = the user who is online (me)
        senderUserID = mAuth.getCurrentUser().getUid();

        //we have to receive visit user id to retrieve all the information of this user
        receiverUserID = getIntent().getExtras().get("visit_user_id").toString();

        //new = the user has to send a request (don't know each other)
        current_state ="new";

        profile_iv = findViewById(R.id.profile_visit_profile_iv);
        username_tv = findViewById(R.id.profile_username_tv);
        userStatus_tv = findViewById(R.id.profile_status_tv);
        sendMessage_btn = findViewById(R.id.profile_send_message_btn);
        cancelMessage_btn = findViewById(R.id.profile_cancel_message_btn);

        //retrieve information from firebase database and display it on fields
        RetrieveUserInfo();


    }

    private void RetrieveUserInfo() {
        UserRef.child(receiverUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //the profile image is optional so we have to add a validation for that
                if (snapshot.exists() && (snapshot.hasChild("image"))){

                    //retrieve
                    String userImage = snapshot.child("image").getValue().toString();
                    String username = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();

                    //display
                    Picasso.get().load(userImage).placeholder(R.drawable.profile_image).into(profile_iv);
                    username_tv.setText(username);
                    userStatus_tv.setText(userStatus);

                    ManageChatRequest();

                }
                else { // this is for ===> if the user has not set any profile picture

                    //retrieve
                    String username = snapshot.child("name").getValue().toString();
                    String userStatus = snapshot.child("status").getValue().toString();

                    //display
                    username_tv.setText(username);
                    userStatus_tv.setText(userStatus);

                    ManageChatRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }




    private void ManageChatRequest() {

        //this is for set current state, text and color if i click back and click again to profile
        ChatRequestRef.child(senderUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.hasChild(receiverUserID)) {
                            //retrieve request type
                            String requestType = snapshot.child(receiverUserID).child("request_type")
                                    .getValue().toString();

                            if (requestType.equals("sent")) {
                                current_state = "request_sent";
                                sendMessage_btn.setText("Cancel Chat Request");
                                sendMessage_btn.setBackgroundColor(Color.parseColor("#b30000"));
                            }
                            else if (requestType.equals("received")) {
                                current_state = "request_received";
                                sendMessage_btn.setText("Accept Chat Request"); // the receiver will see this
//                                sendMessage_btn.setBackgroundColor(Color.parseColor("#006257"));

                                cancelMessage_btn.setVisibility(View.VISIBLE); // remember, this only for receiver
                                cancelMessage_btn.setEnabled(true);
                                cancelMessage_btn.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        CancelChatRequest();
                                    }
                                });
                            }
                        }
                        else {
                            ContactsRef.child(senderUserID)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.hasChild(receiverUserID)) {
                                                current_state = "friends";
                                                sendMessage_btn.setText("Remove Contact");
                                                sendMessage_btn.setBackgroundColor(Color.parseColor("#b30000"));

                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

        //i don't need to see the button (send message) if i can see my own profile
        if (!senderUserID.equals(receiverUserID)) {
            sendMessage_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    when i click on the send message btn ==> this will basically send a chat request to
                    the user who i in his profile and that user will accept my request or cancel if
                    the user accept my request then my contact will be added in his contact list
                    */
                    sendMessage_btn.setEnabled(false);

                    if (current_state.equals("new")) { //the two person are new in each other
                        SendChatRequest();
                    }

                    if (current_state.equals("request_sent")) { // it means the user has right now to cancel
                        CancelChatRequest();
                    }

                    if (current_state.equals("request_received")) { // this for the receiver (for accept chat request)
                        AcceptChatRequest();
                    }

                    if (current_state.equals("friends")) { // it means if both the users are added in each other contact
                        RemoveSpecificContact();
                    }

                }
            });
        }
        else { // it means the sender who want to send message = receiver (in his profile)
            sendMessage_btn.setVisibility(View.INVISIBLE);
        }

    }

    private void RemoveSpecificContact() {
        //to sender
        ContactsRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                        {   //to receiver
                            ContactsRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            // we have to change the btn text and color
                                            if (task.isSuccessful()) {
                                                sendMessage_btn.setEnabled(true);
                                                current_state = "new";
                                                sendMessage_btn.setText("Send Message");
                                                sendMessage_btn.setBackgroundColor(Color.parseColor("#006257"));

                                                cancelMessage_btn.setVisibility(View.INVISIBLE);
                                                cancelMessage_btn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }


    private void SendChatRequest() { // need a database reference for the new parent
        ChatRequestRef.child(senderUserID).child(receiverUserID)
                .child("request_type").setValue("sent")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            ChatRequestRef.child(receiverUserID).child(senderUserID)
                                    .child("request_type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                           if (task.isSuccessful()) {
//
//                                               //create a notification
//                                               HashMap<String,String> chatNotificationMap = new HashMap<>();
//                                               chatNotificationMap.put("from",senderUserID);
//                                               chatNotificationMap.put("type","request");
//
//                                               //store it on database
//                                               NotificationRef.child(receiverUserID).push()
//                                                       .setValue(chatNotificationMap)
//                                                       .addOnCompleteListener(new OnCompleteListener<Void>() {
//                                                           @Override
//                                                           public void onComplete(@NonNull Task<Void> task) {
//
//                                                               if (task.isSuccessful()) {

                                                                   sendMessage_btn.setEnabled(true);
                                                                   current_state = "request_sent";
                                                                   sendMessage_btn.setText("Cancel Chat Request");
                                                                   sendMessage_btn.setBackgroundColor(Color.parseColor("#b30000"));
//                                                               }
//                                                           }
//                                                       });

                                           }
                                        }
                                    });
                        }
                    }
                });
    }

    private void AcceptChatRequest() {
        /*
        we will create a contact node in firebase database and inside that
        will receive all contacts of specific user, once the receiver click on
        the accept btn then that contact will be saved in contact list (ContactsRef)
        & remove chat request from ChatRequestRef and then will display on our contacts fragment
         */

        //this for sender
        ContactsRef.child(senderUserID).child(receiverUserID)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            //this for receiver (remember, we must display it to both of them)
                            ContactsRef.child(receiverUserID).child(senderUserID)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                /*
                                                we have to remove the chat request from ChatRequestRef to both
                                                because now both have added each other in contact list (ContactsRef)
                                                 */

                                                //to sender
                                                ChatRequestRef.child(senderUserID).child(receiverUserID)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()) {
                                                                    //to receiver
                                                                    ChatRequestRef.child(receiverUserID).child(senderUserID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        sendMessage_btn.setEnabled(true);
                                                                                        current_state = "friends";
                                                                                        //now both of them have choice to remove contact
                                                                                        sendMessage_btn.setText("Remove Contact");
                                                                                        sendMessage_btn.setBackgroundColor(Color.parseColor("#b30000"));

                                                                                        cancelMessage_btn.setVisibility(View.INVISIBLE);
                                                                                        cancelMessage_btn.setEnabled(false);
                                                                                    }
                                                                                }
                                                                            });
                                                                }
                                                            }
                                                        });

                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    private void CancelChatRequest() {
        ChatRequestRef.child(senderUserID).child(receiverUserID)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful())
                        {
                            ChatRequestRef.child(receiverUserID).child(senderUserID)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            // we have to change the btn text and color
                                            if (task.isSuccessful()) {
                                                sendMessage_btn.setEnabled(true);
                                                current_state = "new";
                                                sendMessage_btn.setText("Send Message");
                                                sendMessage_btn.setBackgroundColor(Color.parseColor("#006257"));

                                                cancelMessage_btn.setVisibility(View.INVISIBLE);
                                                cancelMessage_btn.setEnabled(false);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

}