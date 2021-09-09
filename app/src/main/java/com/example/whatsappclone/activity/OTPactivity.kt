package com.example.whatsappclone.activity

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import android.widget.ProgressBar

import android.widget.Toast
import com.example.whatsappclone.R
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.mukesh.OnOtpCompletionListener
import com.mukesh.OtpView
import java.util.concurrent.TimeUnit

class OTPactivity : AppCompatActivity() {
    private lateinit var phoneNumberLabel : TextView
    private lateinit var firebaseAuth : FirebaseAuth
    private  var verificationId : String = "878778"
    private lateinit var otpView : OtpView
    private lateinit var continueButton : Button
    private lateinit var progressDialog : ProgressDialog
    private lateinit var progressBar : ProgressBar
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otpactivity)
        // get ids of views
        phoneNumberLabel = findViewById(R.id.phoneNumberLabel)
        otpView = findViewById(R.id.otpView)
        continueButton = findViewById(R.id.continueButton)
        progressBar = findViewById(R.id.progressBar)
        //open keyboard automatically for OTP;
        val inputMethodManager : InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED,0)
        otpView.requestFocus()

       //get firebase auth instance;
        firebaseAuth = FirebaseAuth.getInstance()
        // get progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending OTP...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        supportActionBar?.hide()

//        verificationId = "123456"
        // get user phone number through intent;
        val userPhoneNumber = intent.getStringExtra(PhoneNumberAuthentication.PHONE_NUMBER_EXTRA)
        // set text of text view;
        phoneNumberLabel.text = "Verify +91$userPhoneNumber"
        //for phone number authentication make phone auth options;
        val phoneAuthOptions = userPhoneNumber?.let {
            PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber("+91$it")
                .setTimeout(60L,TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                    override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                        TODO("Not yet implemented")
                    }

                    override fun onVerificationFailed(p0: FirebaseException) {
                        TODO("Not yet implemented")
                    }

                    override fun onCodeSent(verifyId: String, p1: PhoneAuthProvider.ForceResendingToken) {
                        super.onCodeSent(verifyId, p1)
                        verificationId = verifyId
                        progressDialog.dismiss()
                    }
                }).build()
        }
        // now verify phone number;
        if (phoneAuthOptions != null) {
            PhoneAuthProvider.verifyPhoneNumber(phoneAuthOptions)
        }
        // for testing white list numbers in firebase;

        otpView.setOtpCompletionListener(object : OnOtpCompletionListener{
            override fun onOtpCompleted(otp: String?) {
                progressBar.visibility = View.VISIBLE
                val phoneAuthCredential =
                    otp?.let { PhoneAuthProvider.getCredential(verificationId, otp) }
                //try sign in with this credential;
                if (phoneAuthCredential != null) {
                    firebaseAuth.signInWithCredential(phoneAuthCredential).addOnCompleteListener(
                        OnCompleteListener {
                            if(it.isSuccessful){
//                                Toast.makeText(this@OTPactivity,"signInSuccess",Toast.LENGTH_SHORT).show()
                                val intent = Intent(this@OTPactivity, SetupProfileActivity::class.java)
                                startActivity(intent)
                                progressBar.visibility = View.GONE
                                finishAffinity()
                                Log.d("signInSuccess","signIn successful!")
                            }
                            else{
                                Toast.makeText(this@OTPactivity,"signInFail",Toast.LENGTH_SHORT).show()
                                Log.d("signInSuccess",
                                    "signIn Failed! $verificationId $userPhoneNumber"
                                )

                            }
                        }).addOnFailureListener(OnFailureListener {
                                Toast.makeText(this@OTPactivity,"signInFail with execption" + it + " "+ verificationId,Toast.LENGTH_SHORT).show()
                                 Log.d("signInSuccess","signIn Failed! with : $verificationId",it )
                    })
                }
            }

        })
    }
}