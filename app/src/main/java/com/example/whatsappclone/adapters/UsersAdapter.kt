package com.example.whatsappclone.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.whatsappclone.R
import com.example.whatsappclone.models.User
import de.hdodenhof.circleimageview.CircleImageView

class UsersAdapter(private val context : Context, private val itemUser : ArrayList<User>) : RecyclerView.Adapter<UsersAdapter.UsersViewHolder>() {

    // make view holder;
    class UsersViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        val userProfilePicture : CircleImageView = itemView.findViewById(R.id.userProfilePicture)
        val userName : TextView =  itemView.findViewById(R.id.userName)
        val lastMessage : TextView = itemView.findViewById(R.id.lastMessage)
        val lastMessageTime : TextView = itemView.findViewById(R.id.lastMessageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsersViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_conversation,parent,false)
        return UsersViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsersViewHolder, position: Int) {
        val user = itemUser[position]
        // set user name;
        holder.userName.text = user.userName
        Glide.with(context).load(user.profileImageUrl)
            .placeholder(R.drawable.avatar)
            .into(holder.userProfilePicture)
    }

    override fun getItemCount(): Int {
        return itemUser.size
    }
}