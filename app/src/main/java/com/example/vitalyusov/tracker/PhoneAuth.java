package com.example.vitalyusov.tracker;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.design.widget.CoordinatorLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.content.Intent;
import android.widget.Button;
import android.widget.EditText;
import android.view.animation.TranslateAnimation;
import android.widget.ProgressBar;

import com.alimuzaffar.lib.pin.PinEntryEditText;

import java.util.concurrent.TimeUnit;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

public class PhoneAuth extends AppCompatActivity implements
        View.OnClickListener{

    private static final String TAG = "PhoneAuthActivity";

    private static final String KEY_VERIFY_IN_PROGRESS = "key_verify_in_progress";

    private static final int STATE_INITIALIZED = 1;
    private static final int STATE_CODE_SENT = 2;
    private static final int STATE_VERIFY_FAILED = 3;
    private static final int STATE_VERIFY_SUCCESS = 4;
    private static final int STATE_SIGNIN_FAILED = 5;
    private static final int STATE_SIGNIN_SUCCESS = 6;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private boolean mVerificationInProgress = false;
    private String mVerificationId;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;

    private EditText emailField;
    private EditText phoneField;
    private Button sendButton;
    private Button confirmButton;
    private Button submitButton;
    private ProgressBar LoadingSign;
    private CoordinatorLayout mCLayout;
    private String verificationCode;
    private boolean verified;
    private PhoneAuthCredential credentialPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phoneauth);
        // Restore instance state
        if (savedInstanceState != null) {
            onRestoreInstanceState(savedInstanceState);
        }
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Signed out
        } else {
            // Signed in
            final Intent intent = new Intent(this, TrackingActivity.class);
            startActivity(intent);
        }
        emailField = findViewById(R.id.emailField);
        final PinEntryEditText codeField = findViewById(R.id.codeField);
        codeField.setOnPinEnteredListener(new PinEntryEditText.OnPinEnteredListener(){
            @Override
            public void onPinEntered(CharSequence str) {
                verificationCode = str.toString();
            }
        });
        LoadingSign = findViewById(R.id.LoadingSign);
        sendButton = findViewById(R.id.sendButton);
        confirmButton = findViewById(R.id.confirmButton);
        submitButton = findViewById(R.id.submitButton);
        mCLayout = findViewById(R.id.coordinator_layout);
        confirmButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        submitButton.setOnClickListener(this);
        LoadingSign.setVisibility(View.INVISIBLE);
        confirmButton.setVisibility(View.INVISIBLE);
        submitButton.setVisibility(View.INVISIBLE);
        codeField.setVisibility(View.INVISIBLE);
        emailField.setVisibility(View.INVISIBLE);
        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
        // Initialize phone auth callbacks
        // [START phone_auth_callbacks]
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:" + credential);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                // [END_EXCLUDE]

                // [START_EXCLUDE silent]
                // Update the UI and attempt sign in with the phone credential
                updateUI(STATE_VERIFY_SUCCESS, credential);
                // [END_EXCLUDE]
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e);
                // [START_EXCLUDE silent]
                mVerificationInProgress = false;
                // [END_EXCLUDE]

                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    // Invalid request
                    // [START_EXCLUDE]
                    phoneField.setError("Invalid phone number.");
                    // [END_EXCLUDE]
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    // The SMS quota for the project has been exceeded
                }

                // Show a message and update the UI
                // [START_EXCLUDE]
                updateUI(STATE_VERIFY_FAILED);
                // [END_EXCLUDE]
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:" + verificationId);

                // Save verification ID and resending token so we can use them later
                mVerificationId = verificationId;
                mResendToken = token;

                // [START_EXCLUDE]
                // Update UI
                updateUI(STATE_CODE_SENT);
                // [END_EXCLUDE]
            }
        };
        // [END phone_auth_callbacks]
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);

        // [START_EXCLUDE]
        if (mVerificationInProgress && validatePhoneNumber()) {
            startPhoneNumberVerification(phoneField.getText().toString());
        }
        // [END_EXCLUDE]
    }
    // [END on_start_check_user]

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_VERIFY_IN_PROGRESS, mVerificationInProgress);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mVerificationInProgress = savedInstanceState.getBoolean(KEY_VERIFY_IN_PROGRESS);
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        // [START start_phone_auth]
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
        // [END start_phone_auth]

        mVerificationInProgress = true;
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        // [START verify_with_code]
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        // [END verify_with_code]
        signInWithPhoneAuthCredential(credential);
    }

    //NEEDED TO ADD LATER: RESEND VERIFICATION CODE

    private void signInWithPhoneAuthCredential(PhoneAuthCredential credential) {
        credentialPhone = credential;
        final PinEntryEditText codeField = findViewById(R.id.codeField);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");

                            FirebaseUser user = task.getResult().getUser();
                            // [START_EXCLUDE]
                            updateUI(STATE_SIGNIN_SUCCESS, user);
                            // [END_EXCLUDE]
                        } else {
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                                // [START_EXCLUDE silent]
                                codeField.setError("Invalid code.");
                                // [END_EXCLUDE]
                            }
                            // [START_EXCLUDE silent]
                            // Update UI
                            updateUI(STATE_SIGNIN_FAILED);
                            // [END_EXCLUDE]
                        }
                    }
                });
    }
    // [END sign_in_with_phone]

    private void updateUI(int uiState) {
        updateUI(uiState, mAuth.getCurrentUser(), null);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            updateUI(STATE_SIGNIN_SUCCESS, user);
        } else {
            updateUI(STATE_INITIALIZED);
        }
    }

    private void updateUI(int uiState, FirebaseUser user) {
        updateUI(uiState, user, null);
    }

    private void updateUI(int uiState, PhoneAuthCredential cred) {
        updateUI(uiState, null, cred);
    }

    private void updateUI(int uiState, FirebaseUser user, PhoneAuthCredential cred) {
        PinEntryEditText codeField = findViewById(R.id.codeField);
        phoneField = findViewById(R.id.phoneField);
        sendButton = findViewById(R.id.sendButton);
        confirmButton = findViewById(R.id.confirmButton);
        submitButton = findViewById(R.id.submitButton);
        switch (uiState) {
            case STATE_INITIALIZED:
                // Initialized state, show only the phone number field and start button
                break;
            case STATE_CODE_SENT:
                // Code sent state, show the verification field, the
                codeField.setVisibility(View.VISIBLE);
                sendButton.setVisibility(View.INVISIBLE);
                TranslateAnimation slidePhone = new TranslateAnimation(0, -900, 0,0 );
                TranslateAnimation slideCode = new TranslateAnimation(600, 0, 0,0 );
                slidePhone.setDuration(700);
                slideCode.setDuration(700);
                slidePhone.setFillAfter(true);
                slideCode.setFillAfter(true);
                phoneField.startAnimation(slidePhone);
                codeField.startAnimation(slideCode);
                phoneField.setVisibility(View.INVISIBLE);
                confirmButton.setVisibility(View.VISIBLE);
                LoadingSign.setVisibility(View.INVISIBLE);
                break;
            case STATE_VERIFY_FAILED:
                // Verification has failed, show all options
                break;
            case STATE_VERIFY_SUCCESS:
                // Verification has succeeded
                // Set the verification text based on the credential
                if (cred != null) {
                    if (cred.getSmsCode() != null) {
                        codeField.setText(cred.getSmsCode());
                    } else {
                        //codeField.setText(R.string.instant_validation);
                    }
                }

                break;
            case STATE_SIGNIN_FAILED:
                // No-op, handled by sign-in check
                //mDetailText.setText(R.string.status_sign_in_failed);
                break;
            case STATE_SIGNIN_SUCCESS:
                // Np-op, handled by sign-in check
                break;
        }

        if (user == null) {
            // Signed out
            System.out.println("Signed out\n");
        } else {
            // Signed in
            verified = true;
            System.out.println("Signed in\n");
        }
    }

    private boolean validatePhoneNumber() {
        String phoneNumber = phoneField.getText().toString();
        if (TextUtils.isEmpty(phoneNumber)) {
            phoneField.setError("Invalid phone number.");
            return false;
        }

        return true;
    }

        @Override
        public void onClick (View view){
            PinEntryEditText codeField = findViewById(R.id.codeField);
            phoneField = findViewById(R.id.phoneField);
            sendButton = findViewById(R.id.sendButton);
            confirmButton = findViewById(R.id.confirmButton);
            submitButton = findViewById(R.id.submitButton);
            switch (view.getId()) {
                case R.id.sendButton: {
                    //send verification code
                    LoadingSign.setVisibility(View.VISIBLE);
                    if (!validatePhoneNumber()) {
                        return;
                    }
                    startPhoneNumberVerification(phoneField.getText().toString());
                    System.out.println("clicked send\n");
                    break;
                }
                case R.id.confirmButton: {
                    LoadingSign.setVisibility(View.VISIBLE);
                    if (TextUtils.isEmpty(verificationCode)) {
                        codeField.setError("Cannot be empty.");
                        return;
                    }

                    verifyPhoneNumberWithCode(mVerificationId, verificationCode);
                    if(verified){
                        emailField.setVisibility(View.VISIBLE);
                        confirmButton.setVisibility(View.INVISIBLE);
                        TranslateAnimation slideCodeExit = new TranslateAnimation(0, -1500, 0,0 );
                        TranslateAnimation slideEmail = new TranslateAnimation(600, 0, 0,0 );
                        slideEmail.setDuration(700);
                        slideCodeExit.setDuration(700);
                        slideEmail.setFillAfter(true);
                        slideCodeExit.setFillAfter(true);
                        codeField.startAnimation(slideCodeExit);
                        emailField.startAnimation(slideEmail);
                        codeField.setVisibility(View.INVISIBLE);
                        submitButton.setVisibility(View.VISIBLE);
                        LoadingSign.setVisibility(View.INVISIBLE);
                    }
                    System.out.println("clicked confirm\n");
                    System.out.println(verified);
                    break;
                }
                case R.id.submitButton: {
                    //LoadingSign.setVisibility(View.VISIBLE);
                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    final Intent intent = new Intent(this, TrackingActivity.class);
                    startActivity(intent);
                    //UPDATING USER EMAIL DOES NOT WORK SKIPPED UNTIL FIXED
                    /*user.reauthenticate(credentialPhone)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    Log.d(TAG, "User re-authenticated.");
                                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                                    user.updateEmail(emailField.toString())
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Log.d(TAG, "User email address updated.");
                                                        startActivity(intent);
                                                    }
                                                }
                                            });
                                }
                            });*/
                    System.out.println("clicked submit\n");
                    break;
                }
            }
        }
}
