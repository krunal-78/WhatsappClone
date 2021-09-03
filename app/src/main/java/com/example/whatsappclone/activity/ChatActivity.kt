package com.example.whatsappclone.activity

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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        //get all id's of view
        messageInput = findViewById(R.id.messageInput)
        attachment = findViewById(R.id.attachment)
        camera = findViewById(R.id.camera)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.recyclerView)


        //get data from intent and set it to action bar;
        val userName = intent.getStringExtra(MainActivity.USERNAME_EXTRA)
        // receiver user id ;
        val receiverUserId = intent.getStringExtra(MainActivity.USERID_EXTRA)
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

        supportActionBar!!.title = userName

        setUpRecyclerView()
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


        firebaseDatabase.reference.child("Chats").child(senderRoom).updateChildren(lastMsgObject)
        firebaseDatabase.reference.child("Chats").child(receiverRoom).updateChildren(lastMsgObject)
        firebaseDatabase.reference.child("Chats")
            .child(senderRoom)
            .child("userMessages")
            .child(uniqueId!!)
            .setValue(message)
            .addOnSuccessListener {
                Log.d("signInSuccess","message stored in senderRoom successfully!")
                firebaseDatabase.reference.child("Chats")
                    .child(receiverRoom)
                    .child("userMessages")
                    .child(uniqueId)
                    .setValue(message)
                    .addOnSuccessListener {
                        Log.d("signInSuccess","message stored in receiverRoom successfully!")
                    }.addOnFailureListener{
                        Log.d("singInSuccess","message can't stored in receiverRoom successfully!",it)
                    }
                        val lastMsgObject = hashMapOf<String,Any?>()
                        lastMsgObject["lastMessage"] = message.messageText
                        lastMsgObject["lastMessageTime"] = date.time

                        firebaseDatabase.reference.child("Chats").child(senderRoom).updateChildren(lastMsgObject)
                        firebaseDatabase.reference.child("Chats").child(receiverRoom).updateChildren(lastMsgObject)

            }.addOnFailureListener{
                Log.d("singInSuccess","message can't stored receiverRoom successfully!",it)
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