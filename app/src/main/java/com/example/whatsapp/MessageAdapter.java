package com.example.whatsapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter <MessageAdapter.MessageViewHolder> {

    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference UserRef;
    private Activity context;

    public MessageAdapter( Activity context, List<Messages> userMessagesList) {
        this.context = context;
        this.userMessagesList = userMessagesList;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView senderMessageText, receiverMessageText;
        CircleImageView receiverPicture;
        ImageView messageSenderImage, messageReceiverImage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText = itemView.findViewById(R.id.custom_sender_message_tv);
            receiverMessageText = itemView.findViewById(R.id.custom_receiver_message_tv);
            receiverPicture = itemView.findViewById(R.id.custom_message_profile_civ);
            messageSenderImage = itemView.findViewById(R.id.custom_message_sender_image_view);
            messageReceiverImage = itemView.findViewById(R.id.custom_message_receiver_image_view);

        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.custom_messages_layout,parent,false);

        mAuth = FirebaseAuth.getInstance();

        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        //the user who will be online he will be the sender of the message
        String messageSenderID = mAuth.getCurrentUser().getUid();

        Messages messages = userMessagesList.get(position);
        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();

        //you can basically retrieve the profile picture of the user by UserRef
        UserRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserID);
        UserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                // first confirm if data exist or not
                if (snapshot.exists()) {

                    //you know image is optional so need a validation
                    if (snapshot.hasChild("image")) {

                        //retrieve
                        String receiverUserPicture = snapshot.child("image").getValue().toString();

                        //to display you need a picasso library
                        Picasso.get().load(receiverUserPicture).placeholder(R.drawable.profile_image)
                                .into(holder.receiverPicture);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


        //first make fields invisible
        holder.receiverPicture.setVisibility(View.GONE);
        holder.receiverMessageText.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.messageSenderImage.setVisibility(View.GONE);
        holder.messageReceiverImage.setVisibility(View.GONE);

        //display messages if text
        if (fromMessageType.equals("text")) {

            if (fromUserID.equals(messageSenderID)) { //you are the sender

                holder.senderMessageText.setVisibility(View.VISIBLE);

                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setTextColor(Color.BLACK); // it's not required because i actually do it in xml
                holder.senderMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() + " - " + messages.getDate());

            }
            else { //fromUserID != messageSenderID    (you are the receiver)
                holder.receiverPicture.setVisibility(View.VISIBLE);
                holder.receiverMessageText.setVisibility(View.VISIBLE);

                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setTextColor(Color.BLACK); // it's not required because i actually do it in xml
                holder.receiverMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() + " - " + messages.getDate());

            }
        }
        else if (fromMessageType.equals("image")) {

            if (fromUserID.equals(messageSenderID)) { //you are the sender

                holder.messageSenderImage.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.messageSenderImage);

            }
            else { //fromUserID != messageSenderID    (you are the receiver)
                holder.receiverPicture.setVisibility(View.VISIBLE);
                holder.messageReceiverImage.setVisibility(View.VISIBLE);

                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverImage);

            }
        }
        else if (fromMessageType.equals("pdf") || fromMessageType.equals("docx")) { // which means Document File

            if (fromUserID.equals(messageSenderID)) { //you are the sender

                holder.messageSenderImage.setVisibility(View.VISIBLE);

                /*
                we will display file image and if receiver click on ir will download the file
                this image i uploaded to firebase storage from web and copy url from file location
                 */
                Picasso.get()
                        .load("https://firebasestorage.googleapis.com/v0/b/whatsapp-63f15.appspot.com/o/Image%20Files%2Ffile.png?alt=media&token=969f8549-c80b-4c14-be18-b045a9ee29e6")
                        .into(holder.messageSenderImage);

            }
            else { //fromUserID != messageSenderID    (you are the receiver)

                holder.receiverPicture.setVisibility(View.VISIBLE);
                holder.messageReceiverImage.setVisibility(View.VISIBLE);

                /*
                we will display file image and if receiver click on ir will download the file
                this image i uploaded to firebase storage from web and copy url from file location
                 */
                Picasso.get()
                        .load("https://firebasestorage.googleapis.com/v0/b/whatsapp-63f15.appspot.com/o/Image%20Files%2Ffile.png?alt=media&token=969f8549-c80b-4c14-be18-b045a9ee29e6")
                        .into(holder.messageReceiverImage);

            }
        }

        if (fromUserID.equals(messageSenderID)) { // this for sender
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if user click on any item on the chat activity then we have basically 4 type of files so we have to check

                    if (userMessagesList.get(position).getType().equals("pdf") || userMessagesList.get(position).getType().equals("docx") ) {

                        // also i want to display a dialog message (delete for me , delete for everyone, download and cancel

                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Download and view this document",
                                "Cancel",
                                "Delete for everyone"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) { // "Delete for me"

                                    DeleteSentMessage(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                                else if (i == 1) { //"Download and view this document"

                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                                    context.startActivity(intent);
                                }
                                else if (i == 3) { //"Delete for everyone"

                                    DeleteMessageForEveryone(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("text")) {

                        // also i want to display a dialog message (delete for me , delete for everyone and cancel

                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Cancel",
                                "Delete for everyone"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) { // "Delete for me"

                                    DeleteSentMessage(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                                else if (i == 2) { //"Delete for everyone"

                                    DeleteMessageForEveryone(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("image")) {

                        // also i want to display a dialog message (delete for me ,"Delete for everyone" View and cancel

                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "View this image",
                                "Cancel",
                                "Delete for everyone"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) { // "Delete for me"

                                    DeleteSentMessage(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                                else
                                if (i == 1) { // "View this image"

                                    Intent imageViewerIntent = new Intent(context,ImageViewerActivity.class);
                                    imageViewerIntent.putExtra("url",userMessagesList.get(position).getMessage());
                                    context.startActivity(imageViewerIntent);
                                }
                                else if (i == 3) { //"Delete for everyone"

                                    DeleteMessageForEveryone(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                }
            });
        }
        else { // for receiver
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // if user click on any item on the chat activity then we have basically 4 type of files so we have to check

                    if (userMessagesList.get(position).getType().equals("pdf") || userMessagesList.get(position).getType().equals("docx") ) {

                        // also i want to display a dialog message (delete for me , download and cancel)

                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Download and view this document",
                                "Cancel"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) { // "Delete for me"

                                    DeleteReceiveMessage(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                                else if (i == 1) { //"Download and view this document"
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(userMessagesList.get(position).getMessage()));
                                    context.startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("text")) {

                        // also i want to display a dialog message (delete for me and cancel

                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "Cancel"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) { // "Delete for me"

                                    DeleteReceiveMessage(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                            }
                        });
                        builder.show();
                    }
                    else if (userMessagesList.get(position).getType().equals("image")) {

                        // also i want to display a dialog message (delete for me , View and cancel

                        CharSequence options[] = new CharSequence[] {
                                "Delete for me",
                                "View this image",
                                "Cancel"
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Delete Message?");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                if (i == 0) { // "Delete for me"

                                    DeleteReceiveMessage(position,holder);

                                    Intent intent = new Intent(context,MainActivity.class);
                                    context.startActivity(intent);
                                }
                                else
                                if (i == 1) { // "View this image"

                                    Intent imageViewerIntent = new Intent(context,ImageViewerActivity.class);
                                    imageViewerIntent.putExtra("url",userMessagesList.get(position).getMessage());
                                    context.startActivity(imageViewerIntent);
                                }
                            }
                        });
                        builder.show();
                    }



                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }

    private void DeleteSentMessage(final int position, final MessageViewHolder holder){

        final DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Messages")
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()) {

                    Toast.makeText(context, "Deleted Successfully..", Toast.LENGTH_SHORT).show();
                }
                else {

                    Toast.makeText(context, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private void DeleteReceiveMessage(final int position, final MessageViewHolder holder){

        final DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Messages")
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()) {

                    Toast.makeText(context, "Deleted Successfully..", Toast.LENGTH_SHORT).show();
                }
                else {

                    Toast.makeText(context, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void DeleteMessageForEveryone(final int position, final MessageViewHolder holder){

        //for sender
        final DatabaseReference RootRef = FirebaseDatabase.getInstance().getReference();
        RootRef.child("Messages")
                .child(userMessagesList.get(position).getFrom())
                .child(userMessagesList.get(position).getTo())
                .child(userMessagesList.get(position).getMessageID())
                .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()) {
                    //for receiver
                    RootRef.child("Messages")
                            .child(userMessagesList.get(position).getTo())
                            .child(userMessagesList.get(position).getFrom())
                            .child(userMessagesList.get(position).getMessageID())
                            .removeValue().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                            if (task.isSuccessful()) {

                                Toast.makeText(context, "Deleted Successfully..", Toast.LENGTH_SHORT).show();
                            }
                            else {

                                Toast.makeText(context, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                else {

                    Toast.makeText(context, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

}
