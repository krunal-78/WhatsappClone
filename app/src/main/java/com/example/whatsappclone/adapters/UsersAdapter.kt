package com.example.whatsappclone.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsappclone.R
import com.example.whatsappclone.interfaces.IUsersAdapter
import com.example.whatsappclone.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class UsersAdapter(private val context : Context, private val iUsersAdapter : IUsersAdapter, private val itemUser : ArrayList<User>) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    // make view holder;
    class UsersViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val userProfilePicture : CircleImageView = itemView.findViewById(R.id.userProfilePicture)
        val userName : TextView =  itemView.findViewById(R.id.userName)
        val lastMessage : TextView = itemView.findViewById(R.id.lastMessage)
        var lastMessageTime : TextView = itemView.findViewById(R.id.lastMessageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_conversation,parent,false)
        return UsersViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = itemUser[position]
        //for setting last msg we need data from database for this we need senderRoom;
        val senderId = FirebaseAuth.getInstance().currentUser!!.uid
        val receiverId = user.userId
        val senderRoom = senderId+receiverId

        //now get last msg and last time from database and update in conversation ;
        FirebaseDatabase.getInstance().reference.child("Chats")
            .child(senderRoom)
            .addValueEventListener(object : ValueEventListener{
                @SuppressLint("SimpleDateFormat", "SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
                        val lastMsg = snapshot.child("lastMessage").getValue(String::class.java)
                        val  lastMsgTime = snapshot.child("lastMessageTime").getValue(Long::class.java)
                        val simpleDateFormat = SimpleDateFormat("hh:mm a")
                        holder.lastMessage.text = lastMsg
                        holder.lastMessageTime.text = simpleDateFormat.format(Date(lastMsgTime!!))
                    }

                    else{
                        holder.lastMessage.text = "Tap to Chat"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.d("signInSuccess","can't update last msg because of error!")
                }

            })
        // set user name;
        holder.userName.text = user.userName
        Glide.with(context).load(user.profileImageUrl)
            .placeholder(R.drawable.avatar)
            .into(holder.userProfilePicture)

        holder.itemView.setOnClickListener{
            iUsersAdapter.onItemClicked(itemUser[position])
        }
    }

    override fun getItemCount(): Int {
        return itemUser.size
    }
}