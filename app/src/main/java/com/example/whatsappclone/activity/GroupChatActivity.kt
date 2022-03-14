package com.example.whatsappclone.activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsappclone.R
import com.example.whatsappclone.adapters.GroupMessageAdapter
import com.example.whatsappclone.adapters.MessageAdapter
import com.example.whatsappclone.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView
import java.security.acl.Group
import java.util.*
import kotlin.collections.ArrayList

class GroupChatActivity : AppCompatActivity() {
    private lateinit var messageInput : EditText
    private lateinit var attachment : ImageView
    private lateinit var camera : ImageView
    private lateinit var sendButton : ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var groupMessageAdapter : GroupMessageAdapter
    private lateinit var itemMessages : ArrayList<com.example.whatsappclone.models.Message>
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var receiverUserId :String
    private lateinit var firebaseStorage : FirebaseStorage
    private lateinit var progressDialog: ProgressDialog
    private lateinit var layoutManager : LinearLayoutManager
    private lateinit var progressBar : ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_group_chat)
        //get all id's of view
        messageInput = findViewById(R.id.messageInput)
        attachment = findViewById(R.id.attachment)
        camera = findViewById(R.id.camera)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.recyclerView)
        progressBar = findViewById(R.id.progressBar)
        supportActionBar!!.title = "Group Chat"
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        // for sending image
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending Image....")
        progressDialog.setCancelable(false)

        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        //get data from intent and set it to action bar;
        val userName = intent.getStringExtra(MainActivity.USERNAME_EXTRA)
        // users profile image;
        val userProfile = intent.getStringExtra(MainActivity.USERIMAGE_EXTRA)

        // receiver user id ;
        receiverUserId = intent.getStringExtra(MainActivity.USERID_EXTRA).toString()

        //sender user id;
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid

        itemMessages = ArrayList()

        groupMessageAdapter = GroupMessageAdapter(this,itemMessages)
        // setting layout manager ;
        layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // imp for scrolling , this will show last message first in the screen;
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = groupMessageAdapter

        setUpRecyclerView()
    }
    // on back click close activity;
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return super.onSupportNavigateUp()
    }

    fun onSendButtonClicked(view: android.view.View) {
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val messageText = messageInput.text.toString()
        // we need three things msg text , sender id , time;
        messageInput.setText("")
        val date = Date()
        val message = Message(messageText, senderUserId,date.time)
        //we have to give same id to send and received msg so that we can easily makr change in both;
        val uniqueId = firebaseDatabase.reference.push().key
        if(message.messageText!!.isNotEmpty()) {
            firebaseDatabase.reference.child("Public").child(uniqueId!!).setValue(message)
                .addOnSuccessListener {
                    Log.d("signInSuccess", "message stored successfully in public!")

                }.addOnFailureListener {
                    Log.d("signInSuccess", "message could not stored successfully in public!", it)
                }
        }
        else Toast.makeText(this,"Can't send empty message!",Toast.LENGTH_SHORT).show()
    }

    private fun setUpRecyclerView(){
        firebaseDatabase.reference.child("Public")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    itemMessages.clear()
                    snapshot.children.forEach{
                        val message = it.getValue(Message::class.java)
                        if (message != null) {
                            message.messageId = it.key.toString()
                            itemMessages.add(message)
                        }
                    }
                    groupMessageAdapter.notifyDataSetChanged()
                    // to show last message in the screen already scrolled;
                    recyclerView.smoothScrollToPosition(recyclerView.adapter!!.itemCount)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.d("singInSuccess","can't set up message in recycler view because of error!")
                }
            })
    }
    fun onAttachmentClicked(view : View){
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = "image/*" // for all type "*/*"
        startActivityForResult(intent,25)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode==25){
            if(data!=null){
                if(data.data!=null){
                    val selectedImage = data.data
                    val date = Date()
                    val currTime = date.time
                    val storageReference = firebaseStorage.reference.child("chats")
                        .child(currTime.toString())
                    // putfile on that time;
                    if (selectedImage != null) {
                        progressBar.visibility = View.VISIBLE
                        storageReference.putFile(selectedImage).addOnCompleteListener {
                            progressDialog.dismiss()
                            if(it.isSuccessful){
                                storageReference.downloadUrl.addOnSuccessListener {
                                    val selectedImagePath = it.toString()
//                                    Toast.makeText(this,selectedImagePath,Toast.LENGTH_SHORT).show()
                                    // send image also in message;
                                    progressBar.visibility = View.GONE
                                    handleCaptionWithDialog(selectedImagePath);

                                }.addOnFailureListener{
                                    Log.d("signInSuccess","can't download url!")
                                }
                            }
                            else{
                                Log.d("signInSuccess","Task is not successfull!")
                            }
                        }.addOnFailureListener{
                            Log.d("signInSuccess","can't put file in the storage")
                        }
                    }
                }
            }
        }
    }

    private fun handleCaptionWithDialog(selectedImagePath: String){
        // create view for custom dialog;
        val view = LayoutInflater.from(this).inflate(R.layout.show_image_dialog,null)
        // make buttons that are in the layout;
        val sendImage = view.findViewById<ImageView>(R.id.sendImage)
        val cancelButton = view.findViewById<ImageView>(R.id.cancelButton)
        val selectedImage = view.findViewById<ImageView>(R.id.selectedImage)
        val inputCaption = view.findViewById<EditText>(R.id.inputCaption)
        Glide.with(this).load(selectedImagePath).placeholder(R.drawable.image_placeholder).into(selectedImage)



        // create custom image dialog;
        val imageShowDialog = AlertDialog.Builder(this)
            .setView(view).setCancelable(false).create()
        imageShowDialog.window!!.setBackgroundDrawableResource(android.R.color.transparent)
        imageShowDialog.show()
        sendImage.setOnClickListener {
//            Toast.makeText(this,"SendButton",Toast.LENGTH_SHORT).show()
            // send image if clicked send;
            sendImageWithMessage(selectedImagePath,inputCaption.text.toString())
            imageShowDialog.dismiss()
        }
        cancelButton.setOnClickListener {
//            Toast.makeText(this,"CancelButton",Toast.LENGTH_SHORT).show()
            imageShowDialog.dismiss()
        }
    }
    private fun sendImageWithMessage(selectedImagePath : String,inputCaption:String){
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        progressDialog.show()
        val date = Date()
        // three things message text k sender uid and time;
        val message = Message(inputCaption,senderUserId,date.time)
        //we have to give same id to send and received msg so that we can easily makr change in both;
//        val uniqueId = firebaseDatabase.reference.push().key
//        message.messageText = "photo"
        message.imageUrl = selectedImagePath

        firebaseDatabase.reference.child("Public")
            .push().setValue(message)
            .addOnSuccessListener {
                progressDialog.dismiss()
                Log.d("signInSuccess", "message stored in public successfully!")
            }
            .addOnFailureListener{
                Log.d("signInSuccess", "message could not in public successfully!")
            }
    }

    override fun onResume() {
        super.onResume()
        val currentUid = FirebaseAuth.getInstance().uid
        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("Online")
    }

    override fun onUserLeaveHint() { // on home buttom click user will get offline
        super.onUserLeaveHint()
        val currentUid = FirebaseAuth.getInstance().uid
        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("")
    }

    override fun onPause() {
        super.onPause()
        val currentUid = FirebaseAuth.getInstance().uid
        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("")
    }
}