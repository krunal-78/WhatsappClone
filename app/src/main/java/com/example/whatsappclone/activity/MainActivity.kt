package com.example.whatsappclone.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import com.example.whatsappclone.R
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.whatsappclone.adapters.UsersAdapter
import com.example.whatsappclone.interfaces.IUsersAdapter
import com.example.whatsappclone.models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class MainActivity : AppCompatActivity(),IUsersAdapter {
    private lateinit var bottomNavigationBar : BottomNavigationView
    private lateinit var recyclerView : RecyclerView
    private lateinit var usersAdapter : UsersAdapter
    private lateinit var itemUsers : ArrayList<User>
    private lateinit var firebaseDatabase: FirebaseDatabase
    
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

        firebaseDatabase = FirebaseDatabase.getInstance()
        itemUsers = ArrayList<User>()
        usersAdapter = UsersAdapter(this,this,itemUsers)
        recyclerView.adapter = usersAdapter
        recyclerView.addItemDecoration(DividerItemDecoration(this,DividerItemDecoration.VERTICAL))

        // now fetch data in recycler view from database;
        fetchDataInRecyclerView()
    }

    private fun fetchDataInRecyclerView(){
        firebaseDatabase.reference.child("Users").addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                itemUsers.clear()
                snapshot.children.forEach{
                    val user = it.getValue(User::class.java)
                    if (user != null) {
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