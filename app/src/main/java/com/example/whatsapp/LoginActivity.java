package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference UsersRef;

    private Button login_btn , phone_btn;
    private EditText email_et, password_et;
    private TextView forgetPassword_tv,newAccount_tv;

    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");
        InitializeFields();

        newAccount_tv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserToRegisterActivity();
            }
        });

        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AllowUserToLogin();
            }
        });

        phone_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SendUserToPhoneLoginActivity();
            }
        });
    }

    private void AllowUserToLogin() {
        String email = email_et.getText().toString();
        String password = password_et.getText().toString();

        if (TextUtils.isEmpty(email)){
            Toast.makeText(this, "Please Enter Email...!", Toast.LENGTH_SHORT).show();
        }
        else if (TextUtils.isEmpty(password)){
            Toast.makeText(this, "Please Enter Password...!", Toast.LENGTH_SHORT).show();
        }
        else {
            loadingBar.setTitle("Logged In");
            loadingBar.setMessage("Please wait...");
            loadingBar.setCanceledOnTouchOutside(true); //this loading bar will not be disappeared until the new account has been logged
            loadingBar.show();

            mAuth.signInWithEmailAndPassword(email,password)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {

                            if (task.isSuccessful()){


                                SendUserToMainActivity();
                                Toast.makeText(LoginActivity.this, "Logged in Successful...", Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();

                            }
                            else {
                                Toast.makeText(LoginActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                loadingBar.dismiss();
                            }
                        }
                    });
        }
    }

    private void InitializeFields() {
        login_btn = findViewById(R.id.login_btn);
        phone_btn = findViewById(R.id.login_phone_btn);
        email_et = findViewById(R.id.login_email_et);
        password_et = findViewById(R.id.login_password_et);
        forgetPassword_tv =findViewById(R.id.login_forget_password_tv);
        newAccount_tv = findViewById(R.id.login_new_account_tv);

        loadingBar = new ProgressDialog(this);

    }


    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(LoginActivity.this,MainActivity.class);
        // we need to add validation so that the user can not go back if he press the back button
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void SendUserToRegisterActivity() {
        Intent registerIntent = new Intent(LoginActivity.this,RegisterActivity.class);
        startActivity(registerIntent);

    }

    private void SendUserToPhoneLoginActivity() {
        Intent phoneLoginIntent = new Intent(LoginActivity.this,PhoneLoginActivity.class);
        startActivity(phoneLoginIntent);
    }
}