package com.example.whatsapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

//it's mean our friends

public class ContactsFragment extends Fragment {
    private View contactsView;
    private RecyclerView myContactList_rv;

    private DatabaseReference ContactsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public ContactsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        contactsView = inflater.inflate(R.layout.fragment_contacts, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
          /*
        for the online user id (current user = login user ) ==> we have to display only friends
        this is how many people are added in his contact list
         */
        ContactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");


        myContactList_rv = contactsView.findViewById(R.id.contacts_recyclerview);
        myContactList_rv.setLayoutManager(new LinearLayoutManager(getContext()));
        myContactList_rv.setHasFixedSize(true);


        return contactsView;
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
                        .setQuery(ContactsRef, Contacts.class)
                        .build();

        // by using the firebase recycler view adapter we can retrieve all the users from  our firebase
        FirebaseRecyclerAdapter<Contacts, ContactsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, ContactsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull ContactsViewHolder holder, int position, @NonNull Contacts model) {

                        // users id = all friends id of current user
                        final String usersID =getRef(position).getKey();

                        // connect between each friend (users id) from "Contacts" Firebase database and each profile of them from "Users" Firebase database
                        UsersRef.child(usersID).addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {

                              if (snapshot.exists()) {

                                  //
                                  if (snapshot.child("userState").hasChild("state")){

                                      String state = snapshot.child("userState").child("state").getValue().toString();
                                      String date = snapshot.child("userState").child("date").getValue().toString(); // don't need it
                                      String time = snapshot.child("userState").child("time").getValue().toString(); // don't need it

                                      if (state.equals("online")) {
                                          holder.onlineIcon_iv.setVisibility(View.VISIBLE);
                                      }
                                      else if (state.equals("offline")) {
                                          holder.onlineIcon_iv.setVisibility(View.INVISIBLE);
                                      }


                                  }
                                  else {
                                      //the old user (before put state in main activity) = if userState not available in his information
                                      holder.onlineIcon_iv.setVisibility(View.INVISIBLE);
                                  }


                                  // we need a validation to image if exist because it optional
                                  if (snapshot.hasChild("image")) {

                                   /* // here, we are retrieving the name & image & status from firebase database using our model
                                    holder.username_tv.setText(model.getName());
                                    holder.userStatus_tv.setText(model.getStatus());
                                    //to retrieve the image we using picasso library
                                    Picasso.get().load(model.getImage()).placeholder(R.drawable.profile_image).into(holder.profile_iv);*/

                                      String name = snapshot.child("name").getValue().toString();
                                      String status = snapshot.child("status").getValue().toString();
                                      String image = snapshot.child("image").getValue().toString();

                                      holder.username_tv.setText(name);
                                      holder.userStatus_tv.setText(status);
                                      //to retrieve the image we using picasso library
                                      Picasso.get().load(image).placeholder(R.drawable.profile_image).into(holder.profile_iv);
                                  }
                                  else {
                               /*     // here, we are retrieving the name & status from firebase database using our model
                                    holder.username_tv.setText(model.getName());
                                    holder.userStatus_tv.setText(model.getStatus());*/

                                      String name = snapshot.child("name").getValue().toString();
                                      String status = snapshot.child("status").getValue().toString();

                                      holder.username_tv.setText(name);
                                      holder.userStatus_tv.setText(status);
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
                    public ContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        //connect our user to users display layout
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.users_display_layout,parent,false);
                        ContactsViewHolder viewHolder = new ContactsViewHolder (view);
                        return viewHolder;
                    }
                };

        //set basically our Recycler View List
        myContactList_rv.setAdapter(adapter);

        //we have to start listening recycler firebase adapter
        adapter.startListening();


    }

    public  static class ContactsViewHolder extends RecyclerView.ViewHolder
    {
        TextView username_tv ,userStatus_tv;
        CircleImageView profile_iv;
        ImageView onlineIcon_iv;

        public ContactsViewHolder(@NonNull View itemView) {
            super(itemView);

            username_tv = itemView.findViewById(R.id.users_display_username_tv);
            userStatus_tv = itemView.findViewById(R.id.users_display_status_tv);
            profile_iv = itemView.findViewById(R.id.users_display_profile_iv);
            onlineIcon_iv = itemView.findViewById(R.id.users_display_online_iv);
        }
    }

}