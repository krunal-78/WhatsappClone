package com.example.whatsappclone.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import com.example.whatsappclone.R
import com.google.firebase.auth.FirebaseAuth

class PhoneNumberAuthentication : AppCompatActivity() {
    private lateinit var phoneNumberInput : EditText
    private lateinit var getOTPButton : Button
    private lateinit var warning : TextView
    private lateinit var firebaseAuth : FirebaseAuth

    companion object{
        const val PHONE_NUMBER_EXTRA = "com.example.whatsappclone.activities.phone_number"
    }

    override fun onStart() {
        super.onStart()
        firebaseAuth = FirebaseAuth.getInstance()
        if(firebaseAuth.currentUser!=null){
            val intent = Intent(this,MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_number_authentication)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        getOTPButton = findViewById(R.id.getOTPButton)
        warning = findViewById(R.id.warning)
        supportActionBar?.hide()
        phoneNumberInput.requestFocus()  // opens keyboard automatically ;
    }

    fun onGetOTPClick(view: View) {
        val phoneNumber = phoneNumberInput.text.toString()
        if(phoneNumber.isEmpty()){
            Toast.makeText(this,"Phone number is empty!", Toast.LENGTH_SHORT).show()
            warning.text = getString(R.string.phone_number_warning)
        }
        else {
            // go to otp activity;
            val intent = Intent(this, OTPactivity::class.java)
            // take phone number through this activity to otp activity
            intent.putExtra(PHONE_NUMBER_EXTRA, phoneNumber)
            startActivity(intent)
        }
    }
}