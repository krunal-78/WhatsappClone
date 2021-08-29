package com.example.whatsappclone.activity

import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import com.example.whatsappclone.R
import de.hdodenhof.circleimageview.CircleImageView
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import com.example.whatsappclone.models.User
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import java.net.URI


class SetupProfileActivity : AppCompatActivity() {
    private lateinit var profilePicture : CircleImageView
    private lateinit var userNameInput : EditText
    private lateinit var setProfileButton : Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var firebaseStorage : FirebaseStorage
    private lateinit var progressDialog : ProgressDialog
    private var selectedImage : Uri? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup_profile)
        // get firebase auth instance;
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        supportActionBar?.hide()
        //get all ids
        profilePicture = findViewById(R.id.profilePicture)
        userNameInput = findViewById(R.id.userNameInput)
        setProfileButton = findViewById(R.id.setProfileButton)
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Signing In...")
        progressDialog.setCancelable(false)

    }

    fun onProfilePictureClick(view: View){
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*"
        startActivityForResult(intent,45)
        // override on activity result after this new activity will start;
    }
    fun onSetUpProfileClick(view : View){
        val userName = userNameInput.text.toString()
        if(userName.isNotEmpty()){
            progressDialog.show()
            if(selectedImage!=null){
                val storageReference =
                    firebaseAuth.currentUser?.let {
                        firebaseStorage.reference.child("Profiles").child(
                            it.uid)
                    }
                storageReference!!.putFile(selectedImage!!).addOnCompleteListener(OnCompleteListener {
                    if(it.isSuccessful){
                        Log.d("signInSuccess","uploaded profile in storage!")
                        // get that url;
                        storageReference.downloadUrl.addOnSuccessListener {
                            val imageUrl = it.toString()
                            val userId = firebaseAuth.currentUser!!.uid
                            // userName is there;
                            val phoneNumber = firebaseAuth.currentUser!!.phoneNumber

                            val user = User(userId,userName,phoneNumber!!,imageUrl)
                            // now add user in the data base;
                            firebaseDatabase.reference.child("Users").child(userId).setValue(user)
                                .addOnSuccessListener {
                                    val intent = Intent(this,MainActivity::class.java)
                                    startActivity(intent)
                                    progressDialog.dismiss()
                                    finish()
                                    Log.d("signInSuccess","added user successfully in database!")
                                }.addOnFailureListener(OnFailureListener {
                                    Log.d("signInSuccess","can't add user successfully in database!",it)
                                })
                        }.addOnFailureListener(OnFailureListener {
                            Log.d("signInSuccess","can't get image url",it)
                        })
                    }
                }).addOnCanceledListener {
                    Log.e("signInSuccess","can't upload profile in storage!")
                }
            }
            else{
                val userId = firebaseAuth.currentUser!!.uid
                // userName is there;
                val phoneNumber = firebaseAuth.currentUser!!.phoneNumber

                val user = User(userId,userName,phoneNumber!!,"No image")
                // now add user in the data base;
                firebaseDatabase.reference.child("Users").child(userId).setValue(user)
                    .addOnSuccessListener {
                        val intent = Intent(this,MainActivity::class.java)
                        startActivity(intent)
                        progressDialog.dismiss()
                        finish()
                        Log.d("signInSuccess","added user successfully in database!")
                    }.addOnFailureListener(OnFailureListener {
                        Log.d("signInSuccess","can't add user successfully in database!",it)
                    })
            }.addOnFailureListener(OnFailureListener {
                Log.d("signInSuccess","can't get image url",it)
            })
        }
        else{
            Toast.makeText(this,"Name can't be empty!",Toast.LENGTH_SHORT).show()
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(data!=null){
            if(data.data!=null){
                selectedImage =  data.data  // non null
                profilePicture.setImageURI(selectedImage)
            }

        }
    }
}