package com.example.whatsappclone.activity

import android.app.ProgressDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.example.whatsappclone.R
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsappclone.adapters.TopStatusAdapter
import com.example.whatsappclone.adapters.UsersAdapter
import com.example.whatsappclone.interfaces.IUsersAdapter
import com.example.whatsappclone.models.Status
import com.example.whatsappclone.models.User
import com.example.whatsappclone.models.UserStatus
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(),IUsersAdapter {
    private lateinit var bottomNavigationBar : BottomNavigationView
    private lateinit var recyclerView : RecyclerView
    private lateinit var usersAdapter : UsersAdapter
    private lateinit var itemUsers : ArrayList<User>
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var topStatusRecyclerView: RecyclerView
    private lateinit var itemUserStatus : ArrayList<UserStatus>
    private lateinit var topStatusAdapter: TopStatusAdapter
    private lateinit var progressDialog : ProgressDialog
    private lateinit var currentUser : User
    companion object{
        const val USERNAME_EXTRA = "com.example.whatsappclone.activity.userName"
        const val USERID_EXTRA = "com.example.whatsappclone.activity.userId"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // get id's
        bottomNavigationBar = findViewById(R.id.bottomNavigationBar)
        recyclerView = findViewById(R.id.recyclerView)
        topStatusRecyclerView = findViewById(R.id.topStatusRecyclerView)

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

        // set the status' adapter;
        topStatusAdapter = TopStatusAdapter(this,itemUserStatus)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        topStatusRecyclerView.layoutManager = layoutManager
        topStatusRecyclerView.adapter = topStatusAdapter
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
                            it.child("allStatus").children.forEach{
                                val itemStatus = it.getValue(Status::class.java)
                                allStatus.add(itemStatus!!)
                            }
                            userStatus.allUserStatus = allStatus // set user status' array list to this array list;
                            itemUserStatus.add(userStatus)
                        }
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
                progressDialog.show()
                val firebaseStorage = FirebaseStorage.getInstance()
                // for unique id node we will pass current time;
                val date = Date()
                val storageReference = firebaseStorage.reference.child("Status").child(date.time.toString())
                // upload it to database;
                storageReference.putFile(data.data!!).addOnCompleteListener{
                    //if file is uploaded successfully then download;
                    if(it.isSuccessful){
                        storageReference.downloadUrl.addOnSuccessListener {
                            // make object of userStatus to store it in database;
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
                            val imageUrl = it.toString() // image url that is downloaded from database successfully;
                            val status = Status(imageUrl,userStatus.lastUpdatedTime)

                            // now update the userStatus in database;
                            firebaseDatabase.reference.child("Stories")
                                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                                .updateChildren(userStatusHashMap)
                            //now add status to the user's status array list in database;
                            firebaseDatabase.reference.child("Stories")
                                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                                .child("allStatus")
                                .push()
                                .setValue(status)
                            // push will add node of unique id;

                            progressDialog.dismiss()
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
    private fun fetchDataInRecyclerView(){
        firebaseDatabase.reference.child("Users").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                itemUsers.clear()
                snapshot.children.forEach{
                    val user = it.getValue(User::class.java)
                    if (user != null && user.userId!=FirebaseAuth.getInstance().currentUser!!.uid) {
                        itemUsers.add(user)
                    }
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
                Toast.makeText(this,"groups clicked!",Toast.LENGTH_SHORT).show()
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
        startActivity(intent)
    }
}