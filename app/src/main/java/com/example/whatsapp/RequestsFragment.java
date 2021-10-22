package com.example.whatsapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/*
    who will send me chat request?
    basically the new people (i don't know them but want talk with me)
 */
public class RequestsFragment extends Fragment {

    private View requestsView;
    private RecyclerView myRequestsList_rv;

    private DatabaseReference RequestsRef, UsersRef, ContactRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public RequestsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        requestsView =inflater.inflate(R.layout.fragment_requests, container, false);


        //we need user id who logged in into his account anf for that id we will display all requests
        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RequestsRef = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        ContactRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

        myRequestsList_rv = requestsView.findViewById(R.id.requests_recyclerview);
        myRequestsList_rv.setLayoutManager(new LinearLayoutManager(getContext()));
        myRequestsList_rv.setHasFixedSize(true);

        return requestsView;
    }
     /*
        in order ro use firebase recycler adapter we need to add a dependency for the firebase UI
        Link to Firebase-UI:https://github.com/firebase/FirebaseUI-Android */

    @Override
    public void onStart() {
        super.onStart();

        //first we need to create a contact model//

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(RequestsRef.child(currentUserID), Contacts.class)
                        .build();

        // by using the firebase recycler view adapter we can retrieve all the users from  our firebase
        FirebaseRecyclerAdapter<Contacts, RequestViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, RequestViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull RequestViewHolder holder, int position, @NonNull Contacts model) {

                        // first, make buttons visible
                        holder.itemView.findViewById(R.id.users_display_request_accept_btn).setVisibility(View.VISIBLE);
                        holder.itemView.findViewById(R.id.users_display_request_cancel_btn).setVisibility(View.VISIBLE);

                        //and online icon invisible
                        holder.itemView.findViewById(R.id.users_display_online_iv).setVisibility(View.INVISIBLE);

                        // to get id of users line by line
                        final String userID = getRef(position).getKey();

                        DatabaseReference GetTypeRef = getRef(position).child("request_type").getRef();

                        /*
                        once get user id we have to go inside each id and check the request type
                        because we are just retrieving the received request type
                        */
                        GetTypeRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                //get Value from snapshot
                                if (snapshot.exists()) {
                                    String reqType = snapshot.getValue().toString();

                                    //check
                                    if (reqType.equals("received")) {

                                        UsersRef.child(userID).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                // we need a validation to image if exist because it optional
                                                if (snapshot.hasChild("image")) {

                                                    String image = snapshot.child("image").getValue().toString();

                                                    //to retrieve the image we using picasso library
                                                    Picasso.get().load(image).placeholder(R.drawable.profile_image).into(holder.profile_iv);
                                                }

                                                String name = snapshot.child("name").getValue().toString();
                                                String status = snapshot.child("status").getValue().toString();

                                                holder.username_tv.setText(name);
                                                holder.userStatus_tv.setText("Wants to connect with you.");

                                                //make buttons work on
                                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {

                                                        //here we can create options for our dialog box
                                                        CharSequence options[] =new CharSequence[] {
                                                                "Accept",
                                                                "Cancel"
                                                        };

                                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                        builder.setTitle(name + " Chat Request");
                                                        builder.setItems(options, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {

                                                                //i = position of CharSequence
                                                                if (i == 0) { // if the user click on accept btn

                                                                    //we will remove the user from request list and add to contact
                                                                    //to sender
                                                                    ContactRef.child(currentUserID).child(userID)
                                                                            .child("Contacts").setValue("Saved")
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    if (task.isSuccessful()) {
                                                                                        //to receiver
                                                                                        ContactRef.child(userID).child(currentUserID)
                                                                                                .child("Contacts").setValue("Saved")
                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                        if (task.isSuccessful()) {
                                                                                                            //now the contact will be saved and display in Contacts Fragment

                                                                                                            //i want to remove that request from the requests fragment//
                                                                                                            //first, we need to remove for sender
                                                                                                            RequestsRef.child(currentUserID).child(userID)
                                                                                                                    .removeValue()
                                                                                                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                        @Override
                                                                                                                        public void onComplete(@NonNull Task<Void> task) {
                                                                                                                            //then for receiver
                                                                                                                            if (task.isSuccessful()){
                                                                                                                                RequestsRef.child(userID).child(currentUserID)
                                                                                                                                        .removeValue()
                                                                                                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                                                            @Override
                                                                                                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                                                                                                Toast.makeText(getContext(), "New Contact Added.", Toast.LENGTH_SHORT).show();
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
                                                                if (i == 1) { // if the user click on cancel btn

                                                                    //i want to remove that request from the requests fragment//
                                                                    //first, we need to remove for sender
                                                                    RequestsRef.child(currentUserID).child(userID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    //then for receiver
                                                                                    if (task.isSuccessful()){
                                                                                        RequestsRef.child(userID).child(currentUserID)
                                                                                                .removeValue()
                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                        Toast.makeText(getContext(), "Contact Deleted.", Toast.LENGTH_SHORT).show();
                                                                                                    }
                                                                                                });
                                                                                    }
                                                                                }
                                                                            });
                                                                }

                                                            }
                                                        });
                                                        builder.show();
                                                    }
                                                });

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                                    }
                                    else if (reqType.equals("sent")) {

                                        Button reqSent = holder.itemView.findViewById(R.id.users_display_request_cancel_btn);
                                        reqSent.setText("Request Sent");
                                        reqSent.setBackgroundColor(Color.parseColor("#A6A6A6"));

                                        //resize button
                                        ViewGroup.LayoutParams params = reqSent.getLayoutParams();
                                        params.width = 400;

                                        holder.itemView.findViewById(R.id.users_display_request_accept_btn).setVisibility(View.GONE);


                                        UsersRef.child(userID).addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                                                // we need a validation to image if exist because it optional
                                                if (snapshot.hasChild("image")) {

                                                    String image = snapshot.child("image").getValue().toString();

                                                    //to retrieve the image we using picasso library
                                                    Picasso.get().load(image).placeholder(R.drawable.profile_image).into(holder.profile_iv);
                                                }

                                                String name = snapshot.child("name").getValue().toString();
                                                String status = snapshot.child("status").getValue().toString();

                                                holder.username_tv.setText(name);
                                                holder.userStatus_tv.setText("You have sent a request to" + name);

                                                //make buttons work on
                                                holder.itemView.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {

                                                        //here we can create options for our dialog box
                                                        CharSequence options[] =new CharSequence[] {
                                                                "Cancel Chat Request"
                                                        };

                                                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                                                        builder.setTitle(name + " Already Sent Request");
                                                        builder.setItems(options, new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialogInterface, int i) {

                                                                //i = position of CharSequence
                                                                if (i == 0) { // if the user click on cancel btn

                                                                    //i want to remove that request from the requests fragment//
                                                                    //first, we need to remove for sender
                                                                    RequestsRef.child(currentUserID).child(userID)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    //then for receiver
                                                                                    if (task.isSuccessful()){
                                                                                        RequestsRef.child(userID).child(currentUserID)
                                                                                                .removeValue()
                                                                                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                                    @Override
                                                                                                    public void onComplete(@NonNull Task<Void> task) {
                                                                                                        Toast.makeText(getContext(), "You have cancelled the chat request.", Toast.LENGTH_SHORT).show();
                                                                                                    }
                                                                                                });
                                                                                    }
                                                                                }
                                                                            });
                                                                }

                                                            }
                                                        });
                                                        builder.show();
                                                    }
                                                });

                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });



                    }

                    @NonNull
                    @Override
                    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        //connect our user to users display layout
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.users_display_layout,parent,false);
                        RequestViewHolder viewHolder = new RequestViewHolder (view);
                        return viewHolder;
                    }
                };

        //set basically our Recycler View List
        myRequestsList_rv.setAdapter(adapter);

        //we have to start listening recycler firebase adapter
        adapter.startListening();

    }

    public  static class RequestViewHolder extends RecyclerView.ViewHolder
    {
        TextView username_tv ,userStatus_tv;
        CircleImageView profile_iv;
        Button accept_btn,cancel_btn;
        ImageView onlineIcon;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);

            username_tv = itemView.findViewById(R.id.users_display_username_tv);
            userStatus_tv = itemView.findViewById(R.id.users_display_status_tv);
            profile_iv = itemView.findViewById(R.id.users_display_profile_iv);
            accept_btn = itemView.findViewById(R.id.users_display_request_accept_btn);
            cancel_btn = itemView.findViewById(R.id.users_display_request_cancel_btn);
            onlineIcon = itemView.findViewById(R.id.users_display_online_iv);

        }
    }

}