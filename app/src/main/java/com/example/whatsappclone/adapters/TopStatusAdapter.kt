package com.example.whatsappclone.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.devlomi.circularstatusview.CircularStatusView
import com.example.whatsappclone.R
import com.example.whatsappclone.activity.MainActivity
import com.example.whatsappclone.models.UserStatus
import de.hdodenhof.circleimageview.CircleImageView
import omari.hamza.storyview.StoryView
import omari.hamza.storyview.callback.StoryClickListeners
import omari.hamza.storyview.model.MyStory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class TopStatusAdapter(private val context : Context , private val itemUserStatus : ArrayList<UserStatus>) : RecyclerView.Adapter<TopStatusAdapter.TopStatusViewHolder>(){

    class TopStatusViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView){
        val lastStatusImage : CircleImageView = itemView.findViewById(R.id.lastStatusImage)
        val circularStatusView : CircularStatusView = itemView.findViewById(R.id.circularStatusView)
        val userNameInStatus : TextView = itemView.findViewById(R.id.userNameInStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopStatusViewHolder {
        val itemView = LayoutInflater.from(context).inflate(R.layout.item_status,parent,false)
        return TopStatusViewHolder(itemView)
    }

    @SuppressLint("SimpleDateFormat")
    override fun onBindViewHolder(holder: TopStatusViewHolder, position: Int) {
        val userStatus = itemUserStatus[position]
        // get the last status;
        if(userStatus.allUserStatus!!.size!=0) {
            val lastStatus = userStatus.allUserStatus!![0]
            // now update last status into image view of status;
            Glide.with(context).load(lastStatus.imageUrl).into(holder.lastStatusImage)
        }
        //set user name;
        holder.userNameInStatus.text = userStatus.userName
        //now set the count of the status in circular status view;
        holder.circularStatusView.setPortionsCount(userStatus.allUserStatus!!.size)
        holder.circularStatusView.setOnClickListener{
            val myStories = ArrayList<MyStory>()
            // now get all statuses
            userStatus.allUserStatus!!.forEach{
                myStories.add(MyStory(it.imageUrl))
            }
            // error;
            // getting time;
            val simpleDateFormat = SimpleDateFormat("hh:mm a")
            val statusTime = simpleDateFormat.format(Date(userStatus.lastUpdatedTime))
            StoryView.Builder((context as MainActivity).supportFragmentManager)
                    .setStoriesList(myStories)
                    .setStoryDuration(5000)
                    .setTitleText(userStatus.userName)
                    .setSubtitleText(statusTime)
                    .setTitleLogoUrl(userStatus.userProfilePicture)
                    .setStoryClickListeners(object : StoryClickListeners{
                        override fun onDescriptionClickListener(position: Int) {
                            TODO("Not yet implemented")
                        }

                        override fun onTitleIconClickListener(position: Int) {
                            TODO("Not yet implemented")
                        }

                    }).build().show()
        }
    }

    override fun getItemCount(): Int {
        return itemUserStatus.size
    }
}