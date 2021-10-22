package com.example.whatsapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class GroupsFragment extends Fragment {

    //initialize here Items to be used
    private View groupFragmentView;
    private ListView listView;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> listOfGroups = new ArrayList<>();

    private DatabaseReference GroupRef;


    public GroupsFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        groupFragmentView = inflater.inflate(R.layout.fragment_groups, container, false);

        //here inside we can initialize the reference to firebase database to the "Groups"
        GroupRef = FirebaseDatabase.getInstance().getReference().child("Groups");

        //here this method we will initialize fields
        InitializeFields();

        // here we have to create a method to retrieve and display groups from database
        RetrieveAndDisplayGroups();

        //when i click a group name , i want to move to his chat of group
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String currentGroupName = parent.getItemAtPosition(position).toString();
                Intent groupChatIntent= new Intent(getContext(),GroupChatActivity.class);
                groupChatIntent.putExtra("groupName",currentGroupName);
                startActivity(groupChatIntent);
            }
        });

        return groupFragmentView;
    }

    private void InitializeFields() {
        listView = groupFragmentView.findViewById(R.id.fgroups_list_view);
        arrayAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1,listOfGroups);
        listView.setAdapter(arrayAdapter);

    }

    private void RetrieveAndDisplayGroups() {
        GroupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // we have to create a set to store every group
                Set<String> set = new HashSet<>();

                //create iterator to read every child of parent node "Groups"
                Iterator iterator = snapshot.getChildren().iterator();
                //While iterator find a group will store it in set
                while (iterator.hasNext()){
                    set.add(((DataSnapshot)iterator.next()).getKey());
                    /*
                    get key method will basically get all the group names and later when
                    the user how much groups he created so this key will get that
                     */
                }
                //now we have to clear the list so for that we can recurrently as basically
                listOfGroups.clear();
                //then add all items in array list
                listOfGroups.addAll(set);
                //now to see the change on the screen
                arrayAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


}