package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private Toolbar mToolbar;
    private ViewPager myViewPager;
    private TabLayout myTabLayout;
    private TabsAccessAdapter myTabsAccessAdapter;

    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;
    private String currentUserID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        RootRef = FirebaseDatabase.getInstance().getReference();

        mToolbar = (Toolbar) findViewById(R.id.main_page_tollbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("WhatsApp");

        myViewPager = (ViewPager) findViewById(R.id.main_tabs_pager);
        myTabsAccessAdapter = new TabsAccessAdapter(getSupportFragmentManager());
        myViewPager.setAdapter(myTabsAccessAdapter);

        myTabLayout = (TabLayout) findViewById(R.id.main_tabs);
        myTabLayout.setupWithViewPager(myViewPager);
    }

    //onStart method is called whenever we run our app (remember)
    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser == null){
            /*
           - it means that the user is not authenticated
           - So we will send the user to login activity to login first his email address or phone number
            */
            SendUserToLoginActivity();
        }
        else {// if the user is already login

            UpdateUserStatus("online");

           // we will verify the user existence
            VerifyUSerExistence();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

        UpdateUserStatus("offline");
        }
    }

    @Override
    protected void onPause() {

        FirebaseUser currentUser = mAuth.getCurrentUser();

        super.onPause();
        if (currentUser != null) {

            UpdateUserStatus("offline");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {

            UpdateUserStatus("offline");
        }
    }

    private void VerifyUSerExistence() {
        /*
        what will we do ?
        - we will check first for unique user ID, then we will check for the settings (profile picture..etc)
         */
        String currentUserID = mAuth.getCurrentUser().getUid();
        RootRef.child("Users").child(currentUserID).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //the profile picture is optional but username Mandatory
                if ((snapshot.child("name").exists())){
                       /*
                          it means if it exist >> it means that the user is not a new user (he has already
                           created a new account and he set his username and...etc)
                      */
                //    Toast.makeText(MainActivity.this, "Welcome", Toast.LENGTH_SHORT).show();
                }
                else {// that means he is a new user and he has no update his settings
                    SendUserToSettingsActivity();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        getMenuInflater().inflate(R.menu.options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == R.id.main_sign_out_option_mi){

            UpdateUserStatus("offline");

            mAuth.signOut();
            SendUserToLoginActivity();
        }
        if (item.getItemId() == R.id.main_settings_option_mi) {
            SendUserToSettingsActivity();
        }
        if (item.getItemId() == R.id.main_find_friends_option_mi){
            SendUserToFindFriendsActivity();
        }
        if (item.getItemId() == R.id.main_create_group_option_mi){
            RequestNewGroup();
        }

        return true;
    }

    private void RequestNewGroup() {
        /*
        by using an alert dialog we will ask the user to enter the group name and once the user
        entered that we will store it in satisfied by status and we will retrieve it on our group
        fragment        //we need to style this in themes
         */
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this,R.style.AlertDialog);
        builder.setTitle("Enter Group Name :");

        //then we need an edit text field here to get the group name from a user
        final EditText groupNameField = new EditText(MainActivity.this);
        groupNameField.setHint("e.g One Family");
        builder.setView(groupNameField);

        //now we have to add two buttons (create & cancel)
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //first of all we have to get the input from that edit text
                String groupName = groupNameField.getText().toString();

                //now we have to check it if it is empty or not
                if (TextUtils.isEmpty(groupName)){
                    Toast.makeText(MainActivity.this, "Please Write Group Name..", Toast.LENGTH_SHORT).show();

                }
                else { //we will store it inside the firebase database
                    CreateNewGroup(groupName);
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        //we have to show this dialog
        builder.show();
    }

    private void CreateNewGroup(String groupName) {
        //we will write the code to store the group name in the firebase database (the group name will be a key value)
        RootRef.child("Groups").child(groupName).setValue("")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(MainActivity.this, groupName + " group is Created Successfully..", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            Toast.makeText(MainActivity.this, "Error : " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void SendUserToLoginActivity() {
        Intent loginIntent = new Intent(MainActivity.this,LoginActivity.class);
        // we need to add validation so that the user can not go back if he press the back button
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(loginIntent);
    }

    private void SendUserToSettingsActivity() {
        Intent settingsIntent = new Intent(MainActivity.this,SettingsActivity.class);
        startActivity(settingsIntent);
    }

    private void SendUserToFindFriendsActivity() {
        Intent findFriendsIntent = new Intent(MainActivity.this,FindFriendsActivity.class);
        startActivity(findFriendsIntent);
    }

     /*
        online feature and the last seen of a user so first of all we have to get the current user day
        and time  alongside with 12 format with am or pm and to get the state or type that is the online
        state or offline in this firebase database and once we store that then with the help of that we
        will be displaying the state and the last seen of the user
         */
    private void  UpdateUserStatus(String state) {

        //first get the current date
        String saveCurrentTime, saveCurrentDate;

        Calendar calendar = Calendar.getInstance();

        //for date
        SimpleDateFormat currentDate = new SimpleDateFormat("MMM dd, yyyy");
        saveCurrentDate = currentDate.format(calendar.getTime());

        //for time
        SimpleDateFormat currentTime = new SimpleDateFormat("hh:mm a"); //12 format (am , pm)
        saveCurrentTime = currentTime.format(calendar.getTime());

        //create a map to pass it to database
        HashMap<String, Object> onlineStateMap =new HashMap<>();
        onlineStateMap.put("time",saveCurrentTime);
        onlineStateMap.put("date",saveCurrentDate);
        onlineStateMap.put("state",state);

        //now we have to get the current user id
        currentUserID = mAuth.getCurrentUser().getUid();

        //save this data inside database though id + rootRef
        RootRef.child("Users").child(currentUserID).child("userState")
                .updateChildren(onlineStateMap);



    }
}
