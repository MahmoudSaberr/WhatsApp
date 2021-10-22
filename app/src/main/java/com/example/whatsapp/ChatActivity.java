package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private String messageReceiverId, messageReceiverName, messageReceiverImage, messageSenderId;

    private TextView username_tv, lastSeen_tv;
    private CircleImageView userImage_civ;
    private Toolbar chatToolbar;

    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;

    private ImageButton sendMessage_btn, sendFiles_btn;
    private EditText messageInput_et;

    private RecyclerView userMessageList;
    private final List<Messages> messagesList =new ArrayList<>();
    private LinearLayoutManager linearLayoutManager;
    private MessageAdapter messageAdapter;

    private String saveCurrentTime, saveCurrentDate;
    private String checker ="", myUrl=""; // checker, to check if file => image or pdf or word
    private StorageTask uploadTask;
    private Uri fileUri;

    private ProgressDialog loadingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();

        messageReceiverId = getIntent().getExtras().get("chat_user_id").toString();
        messageReceiverName = getIntent().getExtras().get("chat_user_name").toString();
        messageReceiverImage = getIntent().getExtras().get("chat_user_image").toString();

        InitializeFields();

        //retrieve name
        username_tv.setText(messageReceiverName);
        //retrieve image => to display image need a picasso library
        Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage_civ);

        DisplayLastSeen();

        sendMessage_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //to send a message to firebase database
                sendMessage();

            }
        });

        //this button will use to send any type of files
        sendFiles_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //add dialog box which will contain three options that is for the images or PDF or word from his mobile phone

                CharSequence options[] = new CharSequence[] {
                        "Images",
                        "PDF Files",
                        "Ms word",
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                builder.setTitle("Select The File");
                builder.setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        // i = index of item in options[]
                        if (i==0) {
                            checker = "image";

                            //allow user to select image from his phone
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent.createChooser(intent,"Select Image"),438);

                        }
                        if (i == 1) {
                            checker = "pdf";

                            //allow user to select pdf from his phone
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/pdf");
                            startActivityForResult(intent.createChooser(intent,"Select PDF File"),438);
                        }
                        if (i == 2) {
                            checker = "docx";

                            //allow user to select pdf from his phone
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_GET_CONTENT);
                            intent.setType("application/msword");
                            startActivityForResult(intent.createChooser(intent,"Select MS Word File"),438);
                        }
                    }
                });
                builder.show();

            }
        });

    }

    private void InitializeFields() {

        //first, you must inflate the custom layout to toolbar, then fields on this

        chatToolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(chatToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        //access custom chat bar
        LayoutInflater layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View actionBarView = layoutInflater.inflate(R.layout.custom_chat_bar, null);
        actionBar.setCustomView(actionBarView);

        username_tv = findViewById(R.id.custom_chat_bar_name_tv);
        lastSeen_tv = findViewById(R.id.custom_chat_bar_last_seen_tv);
        userImage_civ = findViewById(R.id.custom_chat_bar_picture_iv);

        sendMessage_btn = findViewById(R.id.chat_send_message_ImgBtn);
        sendFiles_btn = findViewById(R.id.chat_send_files_ImgBtn);
        messageInput_et = findViewById(R.id.chat_message_input_et);

        messageAdapter = new MessageAdapter(ChatActivity.this,messagesList);
        userMessageList = findViewById(R.id.chat_message_list_of_users_rv);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessageList.setLayoutManager(linearLayoutManager);
        userMessageList.setHasFixedSize(true);
        userMessageList.setAdapter(messageAdapter);

        Calendar calendar = Calendar.getInstance();

        //for date
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        //for time
        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a"); //12 format (am , pm)
        saveCurrentTime = currentTime.format(calendar.getTime());

        loadingBar = new ProgressDialog(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        loadingBar.setTitle("Set File");
        loadingBar.setMessage("Please wait, we are sending that file...");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();

        if (requestCode == 438 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            //we have to get that image
            fileUri = data.getData(); // now, by using his we are storing that file which is you will select on the phone gallery and will storing it inside url type variable

            if (!checker.equals("image")) { // it means that equal to pdf or docx

                //we will store that file in firebase Storage and database
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Document Files");

                String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverId;
                String messageReceiverRef = "Messages/" + messageReceiverId + "/" + messageSenderId;

               /*
            there will be thousands or you can say million of messages between different users
            so we have to use a random key for each message (unique) so no message replace with
            previous one so..
             */
                DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                        .child(messageSenderId).child(messageReceiverId).push(); // this will basically create a key

                //get that key
                final String messagePushID = userMessageKeyRef.getKey(); //we will store message by using this key

                final StorageReference filePath = storageReference.child(messagePushID + "." + checker);

                //put that file by using filePath storageReference we can
                filePath.putFile(fileUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task ) {

                        if (task.isSuccessful()) {

                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    //now, we have to get the link of that file to store it inside the firebase database

                                    Uri downloadFileUri = uri;
                                    String download_url = downloadFileUri.toString();

                                    //let's do message body
                                    Map messageFileBody = new HashMap();
                                    messageFileBody.put("message",download_url); //which will contain the link to pdf or docx file
                                    messageFileBody.put("name", fileUri.getLastPathSegment()); // text or file
                                    messageFileBody.put("type", checker); // if user click on pdf checker = pdf if click on ms word checker will be docx
                                    messageFileBody.put("from",messageSenderId); //by it we can display information of sender to receiver
                                    messageFileBody.put("to",messageReceiverId);
                                    messageFileBody.put("messageID",messagePushID);// we will need it when want to delete message
                                    messageFileBody.put("time",saveCurrentTime);
                                    messageFileBody.put("date",saveCurrentDate);

                                    //another map to details
                                    Map messageFileDetails = new HashMap();
                                    messageFileDetails.put(messageSenderRef + "/" + messagePushID, messageFileBody);
                            /*
                               it means we will have a message the parent node and after that have sender id ,receiver id
                               and then you will have this message id and then the message body so let's do it to receiver too
                            */
                                    messageFileDetails.put(messageReceiverRef + "/" + messagePushID, messageFileBody);

                                    // now we need update the children for it
                                    RootRef.updateChildren(messageFileDetails);
                                    loadingBar.dismiss();
                                }
                            });

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        loadingBar.dismiss();
                        Toast.makeText(ChatActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() { // how much is uploaded
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double p = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        loadingBar.setMessage((int) p +" % Uploading");
                    }
                });
            }
            else if (checker.equals("image")) {
                //we will store that image in firebase Storage and database
                StorageReference storageReference = FirebaseStorage.getInstance().getReference().child("Image Files");

                final String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverId;
                final String messageReceiverRef = "Messages/" + messageReceiverId + "/" + messageSenderId;

                /*
            there will be thousands or you can say million of messages between different users
            so we have to use a random key for each message (unique) so no message replace with
            previous one so..
             */
                DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                        .child(messageSenderId).child(messageReceiverId).push(); // this will basically create a key

                //get that key
                final String messagePushID = userMessageKeyRef.getKey(); //we will store message by using this key

                StorageReference filePath = storageReference.child(messagePushID + "." + "jpg");
                uploadTask = filePath.putFile(fileUri);
                uploadTask.continueWithTask(new Continuation() {
                    @Override
                    public Object then(@NonNull Task task) throws Exception {

                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }

                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {
                        if (task.isSuccessful()) {

                            Uri downloadUrl = task.getResult();
                            myUrl = downloadUrl.toString();

                            //store
                            //let's do message body
                            Map messageImageBody = new HashMap();
                            messageImageBody.put("message",myUrl);
                            messageImageBody.put("name", fileUri.getLastPathSegment()); // text or file
                            messageImageBody.put("type", checker);
                            messageImageBody.put("from",messageSenderId); //by it we can display information of sender to receiver
                            messageImageBody.put("to",messageReceiverId);
                            messageImageBody.put("messageID",messagePushID);// we will need it when want to delete message
                            messageImageBody.put("time",saveCurrentTime);
                            messageImageBody.put("date",saveCurrentDate);

                            //another map to details
                            Map messageBodyDetails = new HashMap();
                            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageImageBody);
                            /*
                               it means we will have a message the parent node and after that have sender id ,receiver id
                               and then you will have this message id and then the message body so let's do it to receiver too
                            */
                            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageImageBody);

                            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                                @Override
                                public void onComplete(@NonNull Task task) {

                                    if (task.isSuccessful()) {
                                        loadingBar.dismiss();
                                        Toast.makeText(ChatActivity.this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
                                    }
                                    else {
                                        loadingBar.dismiss();
                                        Toast.makeText(ChatActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                    }

                                    //then i want to clear edit text
                                    messageInput_et.setText("");
                                }
                            });
                            loadingBar.dismiss();
                        }
                    }
                });
            }
            else {
                loadingBar.dismiss();
                Toast.makeText(this, "Nothing Selected, Error.", Toast.LENGTH_SHORT).show();
            }


        }

    }

    private void DisplayLastSeen (){

        //retrieve userState data
        RootRef.child("Users").child(messageReceiverId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        //retrieve state & date & time
                        if (snapshot.child("userState").hasChild("state")){

                            String state = snapshot.child("userState").child("state").getValue().toString();
                            String date = snapshot.child("userState").child("date").getValue().toString();
                            String time = snapshot.child("userState").child("time").getValue().toString();

                            if (state.equals("online")) {
                                lastSeen_tv.setText("online");
                            }
                            else if (state.equals("offline")) {
                                lastSeen_tv.setText("Last Seen: " + date + "        " + time);
                            }


                        }
                        else {
                            //the old user (before put state in main activity) = if userState not available in his information
                            lastSeen_tv.setText("offline");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    //whenever this chat activity start i want to display all messages of the user
    @Override
    protected void onStart() {
        super.onStart();

        //this line because if you pause your app messages list don't repeat
        messagesList.clear();

        //i will retrieve messages by RootRef
        RootRef.child("Messages").child(messageSenderId).child(messageReceiverId)
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                        Messages messages = snapshot.getValue(Messages.class);
                        messagesList.add(messages);
                        messageAdapter.notifyDataSetChanged();

                         /*
                        so it will display all the count that how much messages we have and it will basically give a value to this
                        and it automatically scroll to that new position
                         */
                        userMessageList.smoothScrollToPosition(userMessageList.getAdapter().getItemCount());

                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

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


    //to send a message to firebase database
    private void sendMessage(){

        //first, we have to get the message from edit text an to do that verify either it empty or not
        String message = messageInput_et.getText().toString();

        if (TextUtils.isEmpty(message)) {

            Toast.makeText(this, "Write Message First..!!", Toast.LENGTH_SHORT).show();
        }
        else { // not empty

            //we have to send message to firebase database
            String messageSenderRef = "Messages/" + messageSenderId + "/" + messageReceiverId;
            String messageReceiverRef = "Messages/" + messageReceiverId + "/" + messageSenderId;

            /*
            there will be thousands or you can say million of messages between different users
            so we have to use a random key for each message (unique) so no message replace with
            previous one so..
             */
            DatabaseReference userMessageKeyRef = RootRef.child("Messages")
                    .child(messageSenderId).child(messageReceiverId).push(); // this will basically create a key

            //get that key
            String messagePushID = userMessageKeyRef.getKey(); //we will store message by using this key

            //let's do message body
            Map messageTextBody = new HashMap();
            messageTextBody.put("message",message);
            messageTextBody.put("type","text"); // text or file
            messageTextBody.put("from",messageSenderId); //by it we can display information of sender to receiver
            messageTextBody.put("to",messageReceiverId);
            messageTextBody.put("messageID",messagePushID);// we will need it when want to delete message
            messageTextBody.put("time",saveCurrentTime);
            messageTextBody.put("date",saveCurrentDate);

            //another map to details
            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderRef + "/" + messagePushID, messageTextBody);
            /*
            it means we will have a message the parent node and after that have sender id ,receiver id
            and then you will have this message id and then the message body so let's do it to receiver too
            */
            messageBodyDetails.put(messageReceiverRef + "/" + messagePushID, messageTextBody);

            RootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {

                    if (task.isSuccessful()) {
                        Toast.makeText(ChatActivity.this, "Message Sent Successfully", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(ChatActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    //then i want to clear edit text
                    messageInput_et.setText("");
                }
            });
        }

    }


}