package com.example.whatsappclone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class PhoneNumberAuthentication : AppCompatActivity() {
    private lateinit var phoneNumberInput : EditText
    private lateinit var getOTPButton : Button

    companion object{
        const val PHONE_NUMBER_EXTRA = "com.example.whatsappclone.activities.phone_number"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_number_authentication)
        phoneNumberInput = findViewById(R.id.phoneNumberInput)
        getOTPButton = findViewById(R.id.getOTPButton)
        supportActionBar?.hide()
        phoneNumberInput.requestFocus()  // opens keyboard automatically ;

    }

    fun onContinueClick(view: View) {
        val phoneNumber = phoneNumberInput.text.toString()
        if(phoneNumber.isEmpty()){
            Toast.makeText(this,"Phone number is empty!", Toast.LENGTH_SHORT).show()
        }
        // go to otp activity;
        val intent = Intent(this, OTPactivity::class.java)
        // take phone number through this activity to otp activity
        intent.putExtra(PHONE_NUMBER_EXTRA,phoneNumber)
        startActivity(intent)
    }
}