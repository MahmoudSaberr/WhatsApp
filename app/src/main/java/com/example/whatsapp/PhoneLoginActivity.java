package com.example.whatsapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class PhoneLoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    private String mVerificationId ;
    private PhoneAuthProvider.ForceResendingToken mResendToken ;

    private ProgressDialog loadingBar;

    private Button sendVerificationCode_btn , verify_btn;
    private EditText phoneNumber_et , verificationCode_et;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_login);

        mAuth = FirebaseAuth.getInstance();

        InitializeFields();

        sendVerificationCode_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //we have to get the phone number first
                String phoneNumber = phoneNumber_et.getText().toString();
                if (TextUtils.isEmpty(phoneNumber)){
                    Toast.makeText(PhoneLoginActivity.this, "Please Enter Your Phone Number First..!!", Toast.LENGTH_SHORT).show();

                }
                else {
                    loadingBar.setTitle("Phone Verification");
                    loadingBar.setMessage("Please wait, while we are authenticating your phone...");
                    loadingBar.setCanceledOnTouchOutside(false); //so if the user click on the screen then loading bar not canceled
                    loadingBar.show();

                    // we want to send the verification code to the user
                    PhoneAuthOptions options =
                            PhoneAuthOptions.newBuilder(mAuth)
                                    .setPhoneNumber(phoneNumber)       // Phone number to verify
                                    .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
                                    .setActivity(PhoneLoginActivity.this)                 // Activity (for callback binding)
                                    .setCallbacks(mCallbacks)          // OnVerificationStateChangedCallbacks
                                    .build();
                    PhoneAuthProvider.verifyPhoneNumber(options);

                }

            }
        });

        verify_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //we have to make sure that fields are invisible
                verify_btn.setVisibility(View.INVISIBLE);
                verificationCode_et.setVisibility(View.INVISIBLE);

                //we have to get that verification code from his edit text
                String verificationCode = verificationCode_et.getText().toString();
                if (TextUtils.isEmpty(verificationCode)) {
                    Toast.makeText(PhoneLoginActivity.this, "Please write verification code first..!!", Toast.LENGTH_SHORT).show();
                    verify_btn.setVisibility(View.VISIBLE);
                    verificationCode_et.setVisibility(View.VISIBLE);
                }
                else {
                    loadingBar.setTitle("Verification Code");
                    loadingBar.setMessage("Please wait, while we are verifying your code...");
                    loadingBar.setCanceledOnTouchOutside(false); //so if the user click on the screen then loading bar not canceled
                    loadingBar.show();

                    PhoneAuthCredential credential = PhoneAuthProvider.getCredential(mVerificationId, verificationCode);
                    signInWithPhoneAuthCredential(credential); // this method to check write or wrong
                }

            }
        });

        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                /*
                   if onVerificationCompleted  = if the user sent the full number and verification code
                   and then if you verify that code --> we will allow the user to login in our app
                 */
                signInWithPhoneAuthCredential(phoneAuthCredential);
            }

            @Override
            public void onVerificationFailed(@NonNull FirebaseException e) {

                //if the user enter invalid phone number or wrong verification code
                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "Invalid Phone Number, Please enter correct phone number with your country code ...", Toast.LENGTH_SHORT).show();

                /*
                we want to make button & Edit Text invisible
                and make verify btn and his edit text visible
                */
                sendVerificationCode_btn.setVisibility(View.VISIBLE);
                phoneNumber_et.setVisibility(View.VISIBLE);
                verify_btn.setVisibility(View.INVISIBLE);
                verificationCode_et.setVisibility(View.INVISIBLE);

            }

            @Override
            public void onCodeSent(@NonNull String verificationId,
                                   @NonNull PhoneAuthProvider.ForceResendingToken token) {

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                //we have to notify the user that is verification code has been sent to you
                loadingBar.dismiss();
                Toast.makeText(PhoneLoginActivity.this, "Code has been sent, please check and verify...", Toast.LENGTH_SHORT).show();

                /*
                once the Code has been sent ==> we want to make button & Edit Text invisible
                and make verify btn and his edit text visible
                */
                sendVerificationCode_btn.setVisibility(View.INVISIBLE);
                phoneNumber_et.setVisibility(View.INVISIBLE);
                verify_btn.setVisibility(View.VISIBLE);
                verificationCode_et.setVisibility(View.VISIBLE);
            }
         };
    }

    private void InitializeFields() {
            sendVerificationCode_btn = findViewById(R.id.phone_login_send_verification_code_btn);
            verify_btn = findViewById(R.id.phone_login_verify_btn);
            phoneNumber_et = findViewById(R.id.phone_login_number_input_et);
            verificationCode_et = findViewById(R.id.phone_verification_code_et);
            loadingBar = new ProgressDialog(this);
    }

    private void SendUserToMainActivity() {
        Intent mainIntent = new Intent(PhoneLoginActivity.this,MainActivity.class);
        // we need to add validation so that the user can not go back if he press the back button
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success : means that the user has provided the correct code and already ready to go to MainActivity
                            loadingBar.dismiss();

                            Toast.makeText(PhoneLoginActivity.this, "Congratulation, you are logged in successfully...", Toast.LENGTH_SHORT).show();
                            SendUserToMainActivity();

                        } else {
                            // Sign in failed
                            String message = task.getException().getMessage();
                            Toast.makeText(PhoneLoginActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


}