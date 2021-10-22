package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateSettings_btn;
    private EditText username_et , userStatus_et;
    private CircleImageView profile_iv;

    private String currentUserID;
    private FirebaseAuth mAuth;
    private DatabaseReference RootRef;

    private static final int GALLERY_PIC = 1;
    private StorageReference UserProfileImageRef;
    private ProgressDialog loadingBar;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        RootRef = FirebaseDatabase.getInstance().getReference();
        UserProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        InitializeFields();

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");

        RetrieveUserInfo();

        updateSettings_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpdateSettings();
            }
        });


        /*
         i want when he click on the Image View go to his gallery , select photo
         and store it in firebase then display it on Image View
         */
        profile_iv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //sending the user to his phone
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                //select type of files it will select from phone
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent , GALLERY_PIC);


            }
        });
    }


    private void InitializeFields() {
        updateSettings_btn = findViewById(R.id.settings_update_btn);
        username_et = findViewById(R.id.settings_username_et);
        userStatus_et = findViewById(R.id.settings_status_et);
        profile_iv = findViewById(R.id.settings_profile_iv);
        loadingBar = new ProgressDialog(this);
        toolbar = findViewById(R.id.settings_tool_bar);
    }

    //for crop the picture (After put library & permission & Activity)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // start picker to get image for cropping
        if (requestCode == GALLERY_PIC && resultCode == RESULT_OK && data!=null) {
            Uri ImageUri = data.getData();

            //then use the image in cropping activity
            CropImage.activity()
                    .setGuidelines(CropImageView.Guidelines.ON)
                    .setAspectRatio(1,1)
                    .start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);


            if (resultCode == RESULT_OK)
            {
                loadingBar.setTitle("Set Profile Image");
                loadingBar.setMessage("Please wait, your profile image is updating...");
                loadingBar.setCanceledOnTouchOutside(false);
                loadingBar.show();

                //this resultUri basically contain the cropped image
                Uri resultUri = result.getUri();

                //then store this crop image in firebase storage
                StorageReference filePath = UserProfileImageRef.child(currentUserID + ".jpg");
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                            /*
                            we will basically save this link of the picture image of the current user
                            inside the firebase database and once we store this image link then we can
                            access and display this profile image on the settings activity
                             */
                            task.getResult().getStorage().getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    final String downloadUrl = task.getResult().toString();
                                    Log.d("Download url", downloadUrl);
                                    RootRef.child("Users").child(currentUserID).child("image")
                                            .setValue(downloadUrl)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()){ // it means image stored inside the firebase database
                                                        Toast.makeText(SettingsActivity.this, "Image uploaded to database, Successfully...", Toast.LENGTH_SHORT).show();
                                                    }
                                                    else {
                                                        String message = task.getException().getMessage();
                                                        Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                }
                            });
                            loadingBar.dismiss();
                            Toast.makeText(SettingsActivity.this, "Profile Image Uploaded Successfully...", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            loadingBar.dismiss();
                            String message = task.getException().getMessage();
                            Toast.makeText(SettingsActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            }
        }

    }

    private void UpdateSettings() {
        String setUsername = username_et.getText().toString();
        String setStatus = userStatus_et.getText().toString();

        if (TextUtils.isEmpty(setUsername)){
            Toast.makeText(this, "Please write your username first...", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(setStatus)){
            Toast.makeText(this, "Please write your status...", Toast.LENGTH_SHORT).show();
        }
        else { // it means that both not empty
            HashMap<String,Object> profileMap = new HashMap<>();
                profileMap.put("uid",currentUserID);
                profileMap.put("name",setUsername);
                profileMap.put("status",setStatus);
            RootRef.child("Users").child(currentUserID).updateChildren(profileMap)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                SendUserToMainActivity();
                                Toast.makeText(SettingsActivity.this, "Profile Updated Successfully..", Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(SettingsActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }


    private void RetrieveUserInfo() {
        RootRef.child("Users").child(currentUserID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if ((snapshot.exists()) && (snapshot.hasChild("name")) && (snapshot.hasChild("image"))){
                            //it means if the user has created an account (by ID), then he updated his profile settings (name & image)
                            String retrieveUsername = snapshot.child("name").getValue().toString();
                            String retrieveStatus = snapshot.child("status").getValue().toString();
                            String retrieveProfileImage = snapshot.child("image").getValue().toString();

                            username_et.setText(retrieveUsername);
                            userStatus_et.setText(retrieveStatus);

                            // to display to the user in retrieveProfileImage for that we using a picasso library
                            Picasso.get().load(retrieveProfileImage).into(profile_iv);
                        }
                        else if ((snapshot.exists()) && (snapshot.hasChild("name"))) {
                            //it means the user create new account and he set his profile name and the status but not the profile picture
                            String retrieveUsername = snapshot.child("name").getValue().toString();
                            String retrieveStatus = snapshot.child("status").getValue().toString();

                            username_et.setText(retrieveUsername);
                            userStatus_et.setText(retrieveStatus);
                        }
                        else {
                            //it means that the user has now created a new account recently and he has to update his profile information
                            Toast.makeText(SettingsActivity.this, "Pleae set your profile information...", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }


    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this,MainActivity.class);
        // we need to add validation so that the user can not go back if he press the back button
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }


}