package com.example.whatsappclone.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.util.Log
import android.view.View
import com.example.whatsappclone.R
import android.widget.EditText
import android.widget.ImageView
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsappclone.adapters.MessageAdapter
import com.example.whatsappclone.models.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*
import kotlin.collections.ArrayList
import android.widget.Toast
import com.google.firebase.storage.FirebaseStorage
import kotlin.collections.HashMap


class ChatActivity : AppCompatActivity() {
    private lateinit var messageInput : EditText
    private lateinit var attachment : ImageView
    private lateinit var camera :ImageView
    private lateinit var sendButton : ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageAdapter : MessageAdapter
    private lateinit var itemMessages : ArrayList<com.example.whatsappclone.models.Message>
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var senderRoom:String
    private lateinit var receiverRoom:String
    private lateinit var receiverUserId :String
    private lateinit var firebaseStorage : FirebaseStorage
    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        //get all id's of view
        messageInput = findViewById(R.id.messageInput)
        attachment = findViewById(R.id.attachment)
        camera = findViewById(R.id.camera)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.recyclerView)

        // for sending image
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending Image....")
        progressDialog.setCancelable(false)


        //get data from intent and set it to action bar;
        val userName = intent.getStringExtra(MainActivity.USERNAME_EXTRA)
        // receiver user id ;
        receiverUserId = intent.getStringExtra(MainActivity.USERID_EXTRA).toString()

        //sender user id;
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        // make two different unique ids for sender and receiver by concatenation of the strings;

        senderRoom = senderUserId+receiverUserId
        receiverRoom = receiverUserId+senderUserId
        //set the adapter;
        itemMessages = ArrayList()
        messageAdapter = MessageAdapter(this,itemMessages,senderRoom,receiverRoom)
        recyclerView.adapter = messageAdapter
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        supportActionBar!!.title = userName

        setUpRecyclerView()
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
                        progressDialog.show()
                        storageReference.putFile(selectedImage).addOnCompleteListener {
                            progressDialog.dismiss()
                            if(it.isSuccessful){
                                storageReference.downloadUrl.addOnSuccessListener {
                                    val selectedImagePath = it.toString()
//                                    Toast.makeText(this,selectedImagePath,Toast.LENGTH_SHORT).show()
                                // send image also in message;
                                    sendImageWithMessage(selectedImagePath)

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
    private fun sendImageWithMessage(selectedImagePath : String){
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val messageText = "photo"
        val date = Date()
        // three things message text k sender uid and time;
        val message = Message(messageText,senderUserId,date.time)
        //we have to give same id to send and received msg so that we can easily makr change in both;
        val uniqueId = firebaseDatabase.reference.push().key
//        message.messageText = "photo"
        message.imageUrl = selectedImagePath
        // push makes new node with unique id every time;
        // make a haspmap for updating children
        val lastMsgObject = hashMapOf<String,Any?>()
        lastMsgObject["lastMessage"] = message.messageText
        lastMsgObject["lastMessageTime"] = date.time

        if(messageText.isNotEmpty()){
            firebaseDatabase.reference.child("Chats").child(senderRoom)
                .updateChildren(lastMsgObject)
            firebaseDatabase.reference.child("Chats").child(receiverRoom)
                .updateChildren(lastMsgObject)
            firebaseDatabase.reference.child("Chats")
                .child(senderRoom)
                .child("userMessages")
                .child(uniqueId!!)
                .setValue(message)
                .addOnSuccessListener {
                    Log.d("signInSuccess", "message stored in senderRoom successfully!")
                    firebaseDatabase.reference.child("Chats")
                        .child(receiverRoom)
                        .child("userMessages")
                        .child(uniqueId!!)
                        .setValue(message)
                        .addOnSuccessListener {
                            Log.d("signInSuccess", "message stored in receiverRoom successfully!")
                        }.addOnFailureListener{
                            Log.d(
                                "singInSuccess",
                                "message can't stored in receiverRoom successfully!",
                                it
                            )
                        }
                    // update last message in user's conversation also
                    val lastMsgObject = hashMapOf<String,Any?>()
                    lastMsgObject["lastMessage"] = message.messageText
                    lastMsgObject["lastMessageTime"] = date.time

                    // update children in receiver room ,sender room and users
                    firebaseDatabase.reference.child("Chats").child(senderRoom)
                        .updateChildren(lastMsgObject)
                    firebaseDatabase.reference.child("Chats").child(receiverRoom)
                        .updateChildren(lastMsgObject)
                    // update last msg in user conversation;
                    firebaseDatabase.reference.child("Users").child(receiverUserId)
                        .updateChildren(lastMsgObject)

                }.addOnFailureListener{
                    Log.d("singInSuccess", "message can't stored senderRoom successfully!", it)
                }
        }
        else{
            Toast.makeText(this,"Can't send empty message",Toast.LENGTH_SHORT).show()
        }
    }
    fun onSendButtonClicked(view: View) {
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val messageText = messageInput.text.toString()
        // we need three things msg text , sender id , time;
        messageInput.setText("")
        val date = Date()
        val message = Message(messageText, senderUserId,date.time)
        //we have to give same id to send and received msg so that we can easily makr change in both;
        val uniqueId = firebaseDatabase.reference.push().key
        // push makes new node with unique id every time;
        val lastMsgObject = hashMapOf<String,Any?>()
        lastMsgObject["lastMessage"] = message.messageText
        lastMsgObject["lastMessageTime"] = date.time

        if(messageText.isNotEmpty()) {
            firebaseDatabase.reference.child("Chats").child(senderRoom)
                .updateChildren(lastMsgObject)
            firebaseDatabase.reference.child("Chats").child(receiverRoom)
                .updateChildren(lastMsgObject)
            firebaseDatabase.reference.child("Chats")
                .child(senderRoom)
                .child("userMessages")
                .child(uniqueId!!)
                .setValue(message)
                .addOnSuccessListener {
                    Log.d("signInSuccess", "message stored in senderRoom successfully!")
                    firebaseDatabase.reference.child("Chats")
                        .child(receiverRoom)
                        .child("userMessages")
                        .child(uniqueId)
                        .setValue(message)
                        .addOnSuccessListener {
                            Log.d("signInSuccess", "message stored in receiverRoom successfully!")
                        }.addOnFailureListener {
                            Log.d(
                                "singInSuccess",
                                "message can't stored in receiverRoom successfully!",
                                it
                            )
                        }
                    val lastMsgObject = hashMapOf<String, Any?>()
                    lastMsgObject["lastMessage"] = message.messageText
                    lastMsgObject["lastMessageTime"] = date.time

                    firebaseDatabase.reference.child("Chats").child(senderRoom)
                        .updateChildren(lastMsgObject)
                    firebaseDatabase.reference.child("Chats").child(receiverRoom)
                        .updateChildren(lastMsgObject)
                    // update last message in User's conversation also;
                    firebaseDatabase.reference.child("Users").child(receiverUserId)
                        .updateChildren(lastMsgObject)

                }.addOnFailureListener {
                    Log.d("singInSuccess", "message can't stored senderRoom successfully!", it)
                }
        }
        else{
            Toast.makeText(this,"Can't send empty message!",Toast.LENGTH_SHORT).show()
        }
    }
    private fun setUpRecyclerView(){
        firebaseDatabase.reference.child("Chats")
            .child(senderRoom)
            .child("userMessages")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    itemMessages.clear()
                    snapshot.children.forEach{
                        val message = it.getValue(Message::class.java)
                        if (message != null) {
                            message.messageId = it.key.toString()
                            itemMessages.add(message)
                        }
                    }
                    messageAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("singInSuccess","can't set up message in recycler view because of error!")
                }
            })
    }
}