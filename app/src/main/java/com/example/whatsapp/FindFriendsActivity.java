package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

/*
we will work on the find friends functionality so that user can search for new people amd then
the user can send a request to them or other people can send request to an online user so that
they can accept their request that is for the contact and then they will be able to chat each other
 */

public class FindFriendsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RecyclerView findFriendsRecyclerViewList;

    private DatabaseReference UsersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_friends);

        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        findFriendsRecyclerViewList = findViewById(R.id.find_friends_recyclerview);
        findFriendsRecyclerViewList.setLayoutManager(new LinearLayoutManager(this));
        findFriendsRecyclerViewList.setHasFixedSize(true);

        toolbar = findViewById(R.id.find_friends_toolbar);
        setSupportActionBar(toolbar);
        //we are going to add a title to that and also a back button on this toolbar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true); // after it go to manifest to this activity and put parentActivityName = "MainActivity"
        getSupportActionBar().setTitle("Find Friends");


    }

     /*
        in order ro use firebase recycler adapter we need to add a dependency for the firebase UI
        Link to Firebase-UI:https://github.com/firebase/FirebaseUI-Android */

    @Override
    protected void onStart() {
        super.onStart();

        //first we need to create a contact model//

        FirebaseRecyclerOptions<Contacts> options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                .setQuery(UsersRef, Contacts.class)
                .build();

        // by using the firebase recycler view adapter we can retrieve all the users from  our firebase
        FirebaseRecyclerAdapter<Contacts,FindFriendsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, FindFriendsViewHolder>(options) {
                    @Override
                    protected void onBindViewHolder(@NonNull FindFriendsViewHolder holder, int position, @NonNull Contacts model) {
                        holder.onlineIcon.setVisibility(View.INVISIBLE);

                        // here, we are retrieving the name & image & status from firebase database using our model
                        holder.username_tv.setText(model.getName());
                        holder.userStatus_tv.setText(model.getStatus());
                        //to retrieve the image we using picasso library
                        Picasso.get().load(model.getImage()).placeholder(R.drawable.profile_image).into(holder.profile_iv);

                        /*
                        whenever a user click on any profile here then it should get uid of this user that i clicked
                        on his profile and then by uid we can retrieve all the information of this user on profile Activity
                         */
                        holder.itemView.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //get uid of the user(position) that i click on
                                String visitUserID = getRef(position).getKey();

                                //send user to profile activity ny using visitUserID
                                Intent profileIntent = new Intent(FindFriendsActivity.this,ProfileActivity.class);
                                profileIntent.putExtra("visit_user_id",visitUserID);
                                startActivity(profileIntent);

                            }
                        });

                    }

                    @NonNull
                    @Override
                    public FindFriendsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        //connect our user to users display layout
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.users_display_layout,parent,false);
                        FindFriendsViewHolder viewHolder = new FindFriendsViewHolder(view);
                        return viewHolder;
                    }
                };

        //set basically our Recycler View List
        findFriendsRecyclerViewList.setAdapter(adapter);

        //we have to start listening recycler firebase adapter
        adapter.startListening();



    }

    public  static class FindFriendsViewHolder extends RecyclerView.ViewHolder
    {
        TextView username_tv ,userStatus_tv;
        CircleImageView profile_iv;
        ImageView onlineIcon;

        public FindFriendsViewHolder(@NonNull View itemView) {
            super(itemView);

            username_tv = itemView.findViewById(R.id.users_display_username_tv);
            userStatus_tv = itemView.findViewById(R.id.users_display_status_tv);
            profile_iv = itemView.findViewById(R.id.users_display_profile_iv);
            onlineIcon = itemView.findViewById(R.id.users_display_online_iv);

        }
    }
}