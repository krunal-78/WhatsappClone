package com.example.whatsappclone.activity

import android.R.*
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.example.whatsappclone.R
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.ProgressBar
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
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import de.hdodenhof.circleimageview.CircleImageView


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
    private lateinit var toolBar: Toolbar
    private lateinit var userImageToolbar: CircleImageView
    private lateinit var backArrow : ImageView
    private lateinit var userNameToolbar : TextView
    private lateinit var indicator : TextView
    private lateinit var layoutManager : LinearLayoutManager
    private lateinit var userName : String
    private lateinit var userProfile: String
    private lateinit var progressBar : ProgressBar
    companion object{
        const val SELECTED_IMAGE_PATH_EXTRA = "com.example.whatsappclone.selected_image_path"
        const val USER_NAME_EXTRA = "com.example.whatsappclone.user_name"
        const val USER_PROFILE_EXTRA = "com.example.whatsappclone.user_profile"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        //get all id's of view
        messageInput = findViewById(R.id.messageInput)
        attachment = findViewById(R.id.attachment)
        camera = findViewById(R.id.camera)
        sendButton = findViewById(R.id.sendButton)
        recyclerView = findViewById(R.id.recyclerView)
        toolBar = findViewById(R.id.toolBar)
        userImageToolbar = findViewById(R.id.userImageToolbar)
        backArrow = findViewById(R.id.backArrow)
        userNameToolbar = findViewById(R.id.userNameToolbar)
        indicator = findViewById(R.id.indicator)
        progressBar = findViewById(R.id.progressBar)

        // for sending image
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending Image....")
        progressDialog.setCancelable(false)


        //get data from intent and set it to action bar;
        userName = intent.getStringExtra(MainActivity.USERNAME_EXTRA).toString()
        // users profile image;
        userProfile = intent.getStringExtra(MainActivity.USERIMAGE_EXTRA).toString()

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
        // setting layout manager ;
        layoutManager = LinearLayoutManager(this)
        layoutManager.stackFromEnd = true // imp for scrolling , this will show last message first in the screen;
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = messageAdapter
        firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseStorage = FirebaseStorage.getInstance()
        // set user online if it is;
        setOnline();
        handleTyping();
//        supportActionBar!!.title = userName
        // setting data in the tool bar;
        setDataInToolBar(userName,userProfile)
        setUpRecyclerView()
//        recyclerView.smoothScrollToPosition(recyclerView.adapter!!.itemCount)

    }
    // imp part doubt;
    private fun handleTyping() {
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val handler = Handler()
        messageInput.addTextChangedListener(object:TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                firebaseDatabase.reference.child("Presence").child(senderUserId).setValue("typing...")
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed(userStoppedTyping,1000)
            }

            override fun afterTextChanged(s: Editable?) {

            }
            val userStoppedTyping = Runnable {
                kotlin.run {
                    firebaseDatabase.reference.child("Presence").child(senderUserId).setValue("Online")
                }
            }
        })
    }

    private fun setOnline() {
        firebaseDatabase.reference.child("Presence").child(receiverUserId).addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.exists()){
                    val Status = snapshot.getValue(String::class.java)
                    if(Status!!.isNotEmpty()){
                        indicator.text = Status
                        indicator.visibility = View.VISIBLE
                    }
                    else{
                        indicator.text = ""
                        indicator.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("signInSuccess","cancelled because of error")
            }

        })
    }

    private fun setDataInToolBar(userName : String , userProfile : String){
        userNameToolbar.text = userName
        Glide.with(this).load(userProfile)
            .placeholder(R.drawable.image_placeholder).into(userImageToolbar)
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
                            if(it.isSuccessful){
                                storageReference.downloadUrl.addOnSuccessListener {
                                    val selectedImagePath = it.toString()
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

    private fun sendImageWithMessage(selectedImagePath: String,inputCaption:String){
        progressDialog.show()
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val date = Date()
        // three things message text k sender uid and time;
        val message = Message(inputCaption, senderUserId, date.time)
        //we have to give same id to send and received msg so that we can easily makr change in both;
        val uniqueId = firebaseDatabase.reference.push().key
//        message.messageText = "photo"
        message.imageUrl = selectedImagePath
        // push makes new node with unique id every time;
        // make a haspmap for updating children
        val lastMsgObject = hashMapOf<String,Any?>()
        lastMsgObject["lastMessage"] = message.messageText
        lastMsgObject["lastMessageTime"] = date.time

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
                            progressDialog.dismiss()
                            Log.d("signInSuccess", "message stored in receiverRoom successfully!")
                        }.addOnFailureListener{
                            Log.d(
                                "singInSuccess",
                                "message can't stored in receiverRoom successfully!",
                                it
                            )
                        }
//                     update last message in user's conversation also
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
    fun onSendButtonClicked(view: View) {
        val senderUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val messageText = messageInput.text.toString()
        // we need three things msg text , sender id , time;
        messageInput.setText("")
        val date = Date()
        val message = Message(messageText, senderUserId,date.time)
        message.imageUrl = ""
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
                    // to show last message in the screen already scrolled;
                    recyclerView.smoothScrollToPosition(recyclerView.adapter!!.itemCount)
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.d("singInSuccess","can't set up message in recycler view because of error!")
                }
            })
    }
    override fun onResume() {
        super.onResume()
        val currentUid = FirebaseAuth.getInstance().uid
        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("Online")
    }

//    override fun onStop() {
//        super.onStop()
//        val currentUid = FirebaseAuth.getInstance().uid
//        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("")
//    }
    fun onBackArrowClicked(view: android.view.View) {
        finish()
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