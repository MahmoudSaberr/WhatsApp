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
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {
    private View privateChatView;
    private RecyclerView myChatList_rv;

    private DatabaseReference ChatsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID ="";
    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        privateChatView = inflater.inflate(R.layout.fragment_chats, container, false);

        mAuth = FirebaseAuth.getInstance();

        if(mAuth.getCurrentUser() != null){
            currentUserID = mAuth.getCurrentUser().getUid();
        }
        ChatsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        myChatList_rv = privateChatView.findViewById(R.id.fChat_list_recyclerview);
        myChatList_rv.setLayoutManager(new LinearLayoutManager(getContext()));
        myChatList_rv.setHasFixedSize(true);

        return privateChatView;
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
                        .setQuery(ChatsRef, Contacts.class)
                        .build();

        /*
        by using the firebase recyclerView we can retrieve all chat list from our Contacts node in database
        each online user has contact list in "Contacts" database
         */
        FirebaseRecyclerAdapter<Contacts, ChatsViewHolder> adapter =
                new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull ChatsViewHolder holder, int position, @NonNull Contacts model) {
                        /*
                        we will get id line by line (id of each friend in my contacts)
                        and once i get id i go to "Users" and get his information (name & image)
                        */
                final String usersID = getRef(position).getKey();
                final String[] retrieveImg = {"default_image"}; // make it element array to assign the final

                UsersRef.child(usersID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {

                        if (snapshot.exists()) {

                            // we need a validation to image if exist because it optional
                            if (snapshot.hasChild("image")) {
                                retrieveImg[0] = snapshot.child("image").getValue().toString();

                                //to retrieve the image we using picasso library
                                Picasso.get().load(retrieveImg[0]).placeholder(R.drawable.profile_image).into(holder.profile_iv);

                            }


                            //to retrieve
                            final String retrieveName = snapshot.child("name").getValue().toString();
                            final String retrieveStatus = snapshot.child("status").getValue().toString();

                            //to display
                            holder.username_tv.setText(retrieveName);

                            //retrieve state & date & time
                            if (snapshot.child("userState").hasChild("state")) {

                                String state = snapshot.child("userState").child("state").getValue().toString();
                                String date = snapshot.child("userState").child("date").getValue().toString();
                                String time = snapshot.child("userState").child("time").getValue().toString();

                                if (state.equals("online")) {
                                    holder.userStatus_tv.setText("online");
                                } else if (state.equals("offline")) {
                                    holder.userStatus_tv.setText("Last Seen: " + date + "      " + time);
                                }


                            } else {
                                //the old user (before put state in main activity) = if userState not available in his information
                                holder.userStatus_tv.setText("offline");
                            }


                            // whenever user click on any item => get the id of chat's user and go to chat activity to this user
                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {

                                    Intent chatIntent = new Intent(getContext(), ChatActivity.class);
                                    //get his id and username
                                    chatIntent.putExtra("chat_user_id", usersID);
                                    chatIntent.putExtra("chat_user_name", retrieveName);
                                    chatIntent.putExtra("chat_user_image", retrieveImg[0]);
                                    startActivity(chatIntent);
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }

            @NonNull
            @Override
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.users_display_layout, parent, false);
                ChatsViewHolder viewHolder = new ChatsViewHolder(view);
                return viewHolder;
            }
        };

        //set basically our Recycler View List
        myChatList_rv.setAdapter(adapter);

        //we have to start listening recycler firebase adapter
        adapter.startListening();
    }

    public  static class ChatsViewHolder extends RecyclerView.ViewHolder
    {
        TextView username_tv ,userStatus_tv;
        CircleImageView profile_iv;
        ImageView onlineIcon;

        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            username_tv = itemView.findViewById(R.id.users_display_username_tv);
            userStatus_tv = itemView.findViewById(R.id.users_display_status_tv);
            profile_iv = itemView.findViewById(R.id.users_display_profile_iv);
            onlineIcon = itemView.findViewById(R.id.users_display_online_iv);
            onlineIcon.setVisibility(View.INVISIBLE);
        }
    }

}



