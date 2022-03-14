package com.example.whatsappclone.activity

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import com.example.whatsappclone.R
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import android.widget.ProgressBar
import android.widget.ImageView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.cooltechworks.views.shimmer.ShimmerRecyclerView
import com.example.whatsappclone.adapters.TopStatusAdapter
import com.example.whatsappclone.adapters.UsersAdapter
import com.example.whatsappclone.interfaces.IUsersAdapter
import com.example.whatsappclone.models.Status
import com.example.whatsappclone.models.User
import com.example.whatsappclone.models.UserStatus
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRegistrar
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(),IUsersAdapter {
    private lateinit var bottomNavigationBar : BottomNavigationView
    private lateinit var recyclerView : ShimmerRecyclerView
    private lateinit var usersAdapter : UsersAdapter
    private lateinit var itemUsers : ArrayList<User>
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var topStatusRecyclerView: ShimmerRecyclerView
    private lateinit var itemUserStatus : ArrayList<UserStatus>
    private lateinit var topStatusAdapter: TopStatusAdapter
    private lateinit var progressDialog : ProgressDialog
    private lateinit var currentUser : User
    private lateinit var progressBar : ProgressBar
    companion object{
        const val USERNAME_EXTRA = "com.example.whatsappclone.activity.userName"
        const val USERID_EXTRA = "com.example.whatsappclone.activity.userId"
        const val USERIMAGE_EXTRA = "com.example.whatsappclone.activity.userImage"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // get id's
        bottomNavigationBar = findViewById(R.id.bottomNavigationBar)
        recyclerView = findViewById(R.id.recyclerView)
        topStatusRecyclerView = findViewById(R.id.topStatusRecyclerView)
        progressBar = findViewById(R.id.progressBar)

        // set progress dialog;
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Uploading Status...")
        progressDialog.setCancelable(false)

        firebaseDatabase = FirebaseDatabase.getInstance()

        itemUsers = ArrayList<User>()
        itemUserStatus = ArrayList<UserStatus>()
        // set the user's adapter
        usersAdapter = UsersAdapter(this,this,itemUsers)
        recyclerView.adapter = usersAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))
//        recyclerView.setDemoChildCount(usersAdapter.itemCount)
        recyclerView.showShimmerAdapter()
        // set the status' adapter;
        topStatusAdapter = TopStatusAdapter(this,itemUserStatus)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        topStatusRecyclerView.layoutManager = layoutManager
        topStatusRecyclerView.adapter = topStatusAdapter
//        topStatusRecyclerView.setDemoChildCount(topStatusAdapter.itemCount)
        if(topStatusAdapter.itemCount!=0) //
        topStatusRecyclerView.showShimmerAdapter()

        // now fetch data in recycler view from database;
        fetchDataInRecyclerView()
        //click listner for bottom navigation bar;
        onBottomNavigationBarClicked()
        //get current user's details;
        getCurrentUserDetails()

        //fetch data from database for top status recycler view;
        fetchDataInTopStatusRecyclerView()

    }
    private fun fetchDataInTopStatusRecyclerView(){
        firebaseDatabase.reference.child("Stories")
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("signInSuccess","got data successfully for status recycler view!")
                    // for making user Status object we need four things;
                    if(snapshot.exists()) {
                        itemUserStatus.clear()
                        snapshot.children.forEach {
                            val userName = it.child("userName").getValue(String::class.java)
                            val userProfilePicture =
                                it.child("userProfilePicture").getValue(String::class.java)
                            val lastUpdatedTime =
                                it.child("lastUpdatedTime").getValue(Long::class.java)
                            val userStatus =
                                UserStatus(userName!!, userProfilePicture!!, lastUpdatedTime!!)
                            // make array list of statuses;
                            val allStatus = ArrayList<Status>()
                            it.child("allStatus").children.forEach {
                                val itemStatus = it.getValue(Status::class.java)
                                allStatus.add(itemStatus!!)
                                removeStatus(itemStatus,allStatus)
                                allStatus.sortByDescending { it.timeStamp }
                            }
                            userStatus.allUserStatus =
                                allStatus // set user status' array list to this array list;
                            // if all status is zero then remove it don't add;
                            if(userStatus.allUserStatus!!.size!=0) {
                                itemUserStatus.add(userStatus)
                            }
//                                removeAllUserStatus(userStatus)
//                                Log.d("signInSuccess","all Status of user are removed!")
                            itemUserStatus.sortByDescending { it.lastUpdatedTime }
                        }
                        topStatusRecyclerView.hideShimmerAdapter()
                        topStatusAdapter.notifyDataSetChanged()
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("signInSuccess","can't get data from database for status recycler view!")
                }

            })
    }
    private fun getCurrentUserDetails(){
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        firebaseDatabase.reference.child("Users")
            .child(currentUserId)
            .addValueEventListener(object : ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    currentUser = snapshot.getValue(User::class.java)!!
                    Log.d("signInSuccess","got current user's details successfully!")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("signInSuccess","failed to get current user's details because of error!")
                }

            })
    }
    private fun onBottomNavigationBarClicked(){
        bottomNavigationBar.setOnNavigationItemSelectedListener(object : BottomNavigationView.OnNavigationItemSelectedListener{
            override fun onNavigationItemSelected(item: MenuItem): Boolean {
                when(item.itemId){
                    R.id.status->{
                        // open image select;
                        val intent = Intent()
                        intent.type = "image/*"
                        intent.setAction(Intent.ACTION_GET_CONTENT)
                        startActivityForResult(intent,75)

                    }

                }
                return true
            }

        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // if an image is selected then upload it to firebase storage;
        if(data!=null){
            if(data.data!=null){
                progressBar.visibility = View.VISIBLE
                val firebaseStorage = FirebaseStorage.getInstance()
                // for unique id node we will pass current time;
                val date = Date()
                val storageReference = firebaseStorage.reference.child("Status").child(date.time.toString())
                // upload it to database;
                storageReference.putFile(data.data!!).addOnCompleteListener{
                    //if file is uploaded successfully then download;
                    if(it.isSuccessful){
                        storageReference.downloadUrl.addOnSuccessListener {
                            progressBar.visibility = View.GONE
                            val imageUrl = it.toString() // image url that is downloaded from database successfully;
                            handleStatusDialog(imageUrl)
                            Log.d("signInSuccess","downloaded status from storage successfully!")

                        }.addOnFailureListener{
                            Log.d("signInSuccess","failed to downloaded status from storage!")
                        }
                    }
                    else{
                        Log.d("signInSuccess","can't upload status successfully!")
                    }
                }.addOnFailureListener{
                    Log.d("signInSuccess","failed to upload status because of exception!",it)
                }
            }
        }
    }
    private fun sendStatusImage(selectedImagePath: String){
        progressDialog.show()
        // make object of userStatus to store it in database;
        val date = Date()
        val userName = currentUser.userName
        val userProfilePicture = currentUser.profileImageUrl
        val lastUpdatedTime = date.time
        val userStatus = UserStatus(userName,userProfilePicture,lastUpdatedTime)

        //now we want to update status all time so make a hashMap;
        val userStatusHashMap = hashMapOf<String,Any?>()
        userStatusHashMap["userName"] = userStatus.userName
        userStatusHashMap["userProfilePicture"] = userStatus.userProfilePicture
        userStatusHashMap["lastUpdatedTime"] = userStatus.lastUpdatedTime

        //make a status object also to add status in arraylist;

        val status = Status(selectedImagePath,userStatus.lastUpdatedTime)
        val statusId = FirebaseAuth.getInstance().currentUser!!.uid + status.timeStamp

        // now update the userStatus in database;
        firebaseDatabase.reference.child("Stories")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(userStatusHashMap)
        //now add status to the user's status array list in database;
        firebaseDatabase.reference.child("Stories")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child("allStatus")
            .child(statusId)
            .setValue(status)
        // push will add node of unique id;
        progressDialog.dismiss()
    }
    private fun handleStatusDialog(selectedImagePath : String){
        // make view for show status dialog;
        val view = LayoutInflater.from(this).inflate(R.layout.show_image_status_dialog,null)
        // make buttons and views that are in layout;
        val sendImage = view.findViewById<ImageView>(R.id.sendImage)
        val cancelButton = view.findViewById<ImageView>(R.id.cancelButton)
        val selectedImage = view.findViewById<ImageView>(R.id.selectedImage)
        // load image in image view;
        Glide.with(this).load(selectedImagePath).placeholder(R.drawable.image_placeholder).into(selectedImage)

        // create custom dialog with view;
        val statusImageShowDialog = AlertDialog.Builder(this)
            .setView(view).setCancelable(false).create()
        statusImageShowDialog.show()
        sendImage.setOnClickListener{
            // upload status;
            sendStatusImage(selectedImagePath)
            statusImageShowDialog.dismiss()
        }
        cancelButton.setOnClickListener{
            statusImageShowDialog.dismiss()
        }
    }
    private fun fetchDataInRecyclerView(){
        firebaseDatabase.reference.child("Users").addValueEventListener(object : ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                itemUsers.clear()
                snapshot.children.forEach{
                    val user = it.getValue(User::class.java)
                    if (user != null && user.userId!=FirebaseAuth.getInstance().currentUser!!.uid) {
                        itemUsers.add(user)
                    }
                    itemUsers.sortByDescending {it.lastMessageTime }
                    recyclerView.hideShimmerAdapter()
                    usersAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("signInSuccess","cancelled because of error")
            }
        })
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.search->{
                Toast.makeText(this,"search clicked!",Toast.LENGTH_SHORT).show()
            }
            R.id.groups->{
//                Toast.makeText(this,"groups clicked!",Toast.LENGTH_SHORT).show()
                val intent = Intent(this,GroupChatActivity::class.java)
                startActivity(intent)
            }
            R.id.settings->{
                Toast.makeText(this,"settings clicked!",Toast.LENGTH_SHORT).show()
            }
            R.id.invite->{
                Toast.makeText(this,"invite clicked!",Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onItemClicked(user: User) {
        val intent = Intent(this,ChatActivity::class.java)
        intent.putExtra(USERNAME_EXTRA,user.userName)
        intent.putExtra(USERID_EXTRA,user.userId)
        intent.putExtra(USERIMAGE_EXTRA,user.profileImageUrl)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        val currentUid = FirebaseAuth.getInstance().uid
        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("Online")
    }

    override fun onBackPressed() { // on back button click user will get offline
        super.onBackPressed()
        val currentUid = FirebaseAuth.getInstance().uid
        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("")
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
//    override fun onDestroy() {
//        super.onDestroy()
//        val currentUid = FirebaseAuth.getInstance().uid
//        firebaseDatabase.reference.child("Presence").child(currentUid!!).setValue("")
//    }
    private fun removeStatus(itemStatus : Status? , allStatus : ArrayList<Status>) {
        val date = Date()
        // imp;
        val currentTimeInHours = TimeUnit.MILLISECONDS.toHours(date.time)
        val statusTimeInHours = TimeUnit.MILLISECONDS.toHours(itemStatus!!.timeStamp)
        if(currentTimeInHours-statusTimeInHours>=23){
            allStatus.remove(itemStatus)
            removeStatusFromDatabase(itemStatus)
            removeStatusFromStorage(itemStatus);
            Log.d("signInSuccess","Status passed 24 hours, so it is removed!")
        }
    }

    private fun removeStatusFromDatabase(itemStatus : Status?){
        val currentUserid = FirebaseAuth.getInstance().currentUser!!.uid
        val statusId = currentUserid + itemStatus!!.timeStamp
        firebaseDatabase.reference.child("Stories")
            .child(currentUserid)
            .child("allStatus")
            .child(statusId)
            .removeValue()
            .addOnSuccessListener {
                Log.d("signInSuccess","deleted status from database successfully!")
            }.addOnFailureListener{
                Log.d("signInSuccess","can't delete status from database successfully!",it)
            }
    }
    private fun removeStatusFromStorage(itemStatus: Status?){
        val timeStamp  = itemStatus!!.timeStamp;
        val storageReference = FirebaseStorage.getInstance().reference.child("Status");
        storageReference.child(timeStamp.toString()).delete().addOnSuccessListener {
            Log.d("signInSuccess","deleted status picture from storage!");
        }.addOnFailureListener{
            Log.d("signInSuccess","can't delete status picture in storage because of exception!",it);
        }
    }
    private fun removeAllUserStatus(userStatus : UserStatus) {
        if (userStatus.allUserStatus!!.size ==0 ) {
            // first remove it from the array list;
            val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
            firebaseDatabase.reference.child("Stories")
                .child(currentUserId)
                .removeValue()
                .addOnSuccessListener {
                    Log.d("signInSuccess", "deleted  all  user status from database successfully!")
                }.addOnFailureListener {
                    Log.d(
                        "signInSuccess",
                        "can't delete all user status from database successfully!",
                        it
                    )
                }
        }
    }

}