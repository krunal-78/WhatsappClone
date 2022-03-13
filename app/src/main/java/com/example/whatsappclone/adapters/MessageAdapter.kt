package com.example.whatsappclone.adapters


import android.annotation.SuppressLint
import android.content.Context
import android.media.Image
import android.provider.Telephony
import android.text.Layout
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.bumptech.glide.Glide

import com.example.whatsappclone.R
import com.example.whatsappclone.models.Message
import com.github.pgreze.reactions.*
import com.github.pgreze.reactions.dsl.reactionConfig
import com.github.pgreze.reactions.dsl.reactionPopup
import com.github.pgreze.reactions.dsl.reactions
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import org.w3c.dom.Text
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MessageAdapter(private val context : Context,private val itemMessages : ArrayList<Message>,private val senderRoom:String,private val receiverRoom:String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SEND = 1;
    private val ITEM_RECEIVE = 2;

    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val sentMessage: TextView = itemView.findViewById(R.id.sentMessage)
        val emojiReactSent: ImageView = itemView.findViewById(R.id.emojiReactSent)
        val imageSent : ImageView = itemView.findViewById(R.id.imageSent)
        val sendMessageTime : TextView = itemView.findViewById(R.id.sendMessageTime)
    }

    class ReceiveViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val receiveMessage: TextView = itemView.findViewById(R.id.receiveMessage)
        val emojiReactReceive: ImageView = itemView.findViewById(R.id.emojiReactReceive)
        val imageReceive : ImageView = itemView.findViewById(R.id.imageReceive)
        val receiveMessageTime : TextView = itemView.findViewById(R.id.receiveMessageTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        // we have to check if message is sent or received ,so override method getItemViewType and from that return type of the view;
        if (viewType == ITEM_SEND) {
            val itemView =
                LayoutInflater.from(context).inflate(R.layout.item_send_message, parent, false)
            return SentViewHolder(itemView)
        } else {
            val itemView =
                LayoutInflater.from(context).inflate(R.layout.item_receive_message, parent, false)
            return ReceiveViewHolder(itemView)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val message = itemMessages[position]
        if (FirebaseAuth.getInstance().uid == message.senderId) {
            return ITEM_SEND;
        } else {
            return ITEM_RECEIVE
        }
        return super.getItemViewType(position)
    }

    @SuppressLint("ClickableViewAccessibility", "SimpleDateFormat")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        //create object of Message class at position;
        val message = itemMessages[position]
        // for reactions we have to make a configuration;
        val reactionsArray = intArrayOf(
            R.drawable.heart_react,
            R.drawable.laughing,
            R.drawable.kiss,
            R.drawable.smile,
            R.drawable.smile_with_heart,
            R.drawable.angry
        )
        val config: ReactionsConfig =
            ReactionsConfigBuilder(context).withReactions(reactionsArray).build()

        //now make pop up which will take context , configuration and position;
        val popUp = ReactionPopup(context, config, object : ReactionSelectedListener {
            override fun invoke(position: Int): Boolean {
                if (holder.javaClass == SentViewHolder::class.java) {
                    val viewHolder = holder as SentViewHolder
                    viewHolder.emojiReactSent.setImageResource(reactionsArray[position])
                    viewHolder.emojiReactSent.visibility = View.VISIBLE
                } else {
                    val viewHolder = holder as ReceiveViewHolder
                    viewHolder.emojiReactReceive.setImageResource(reactionsArray[position])
                    viewHolder.emojiReactReceive.visibility = View.VISIBLE
                }
                // set the feeling to the message;
                message.emojiReact = position
                //now make changes to the database and set the new message with reactions;
                FirebaseDatabase.getInstance().reference // for sender msg;
                    .child("Chats")
                    .child(senderRoom)
                    .child("userMessages")
                    .child(message.messageId)
                    .setValue(message)
                // for receiver msg;
                FirebaseDatabase.getInstance().reference
                    .child("Chats")
                    .child(receiverRoom)
                    .child("userMessages")
                    .child(message.messageId)
                    .setValue(message)

                return true // true for closing popup and false for requesting another reaction;
            }

        })



        // check for the holder class and bind data to the holder in which class matches;
        if (holder.javaClass == SentViewHolder::class.java) {
            val viewHolder = holder as SentViewHolder
            if(message.imageUrl.isNotEmpty()){
                Glide.with(context).load(message.imageUrl).placeholder(R.drawable.image_placeholder).into(viewHolder.imageSent)
                viewHolder.imageSent.visibility = View.VISIBLE
                if(message.messageText=="")
                viewHolder.sentMessage.visibility = View.GONE
                else viewHolder.sentMessage.visibility = View.VISIBLE
            }
            else viewHolder.imageSent.visibility = View.GONE
            viewHolder.sentMessage.text = message.messageText

            // set send message time;
            val simpleDateFormat = SimpleDateFormat("hh:mm a")
            viewHolder.sendMessageTime.text = simpleDateFormat.format(Date(message.sentTime))


            if(message.emojiReact>=0){
//                message.emojiReact = reactionsArray[message.emojiReact]
                viewHolder.emojiReactSent.setImageResource(reactionsArray[message.emojiReact])
                viewHolder.emojiReactSent.visibility = View.VISIBLE
            }
            else{
                viewHolder.emojiReactSent.visibility = View.GONE
            }
            var isLongClicked = false
            var isLongClickedImg = false
            // converting on touch listener into on long click listener; imp
            viewHolder.sentMessage.setOnLongClickListener(object : View.OnLongClickListener{
                override fun onLongClick(v: View?): Boolean {
                    isLongClicked = true
                    return false
                }

            })
            viewHolder.imageSent.setOnLongClickListener(object :View.OnLongClickListener{
                override fun onLongClick(v: View?): Boolean {
                    isLongClickedImg = true
                    return false
                }
            })
            viewHolder.sentMessage.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if(isLongClicked){
                        popUp.onTouch(v!!,event!!)
                    }
                    return false
                }
            })
            viewHolder.imageSent.setOnTouchListener(object :View.OnTouchListener{
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if(isLongClickedImg){
                        popUp.onTouch(v!!,event!!)
                    }
                    return false
                }
            })

        } else {
            val viewHolder = holder as ReceiveViewHolder
            if(message.imageUrl.isNotEmpty()){
                Glide.with(context).load(message.imageUrl).placeholder(R.drawable.image_placeholder).into(viewHolder.imageReceive)
                viewHolder.imageReceive.visibility = View.VISIBLE
                if(message.messageText=="")
                viewHolder.receiveMessage.visibility  = View.GONE
                else viewHolder.receiveMessage.visibility = View.GONE
            }
            else viewHolder.imageReceive.visibility = View.GONE
            viewHolder.receiveMessage.text = message.messageText
            if(message.emojiReact>=0){

//                message.emojiReact = reactionsArray[message.emojiReact]
                viewHolder.emojiReactReceive.setImageResource(reactionsArray[message.emojiReact])
                viewHolder.emojiReactReceive.visibility = View.VISIBLE
            }
            else{
                viewHolder.emojiReactReceive.visibility = View.GONE
            }

            // set receive message time;
            val simpleDateFormat = SimpleDateFormat("hh:mm a")
            viewHolder.receiveMessageTime.text = simpleDateFormat.format(Date(message.sentTime))

            var isLongclicked = false
            var isLongClickedImg = false
            viewHolder.receiveMessage.setOnLongClickListener(object : View.OnLongClickListener{
                override fun onLongClick(v: View?): Boolean {
                    isLongclicked = true
                    return false
                }
            })
            viewHolder.imageReceive.setOnLongClickListener(object :View.OnLongClickListener{
                override fun onLongClick(v: View?): Boolean {
                    isLongClickedImg = true
                    return false;
                }
            })
            viewHolder.receiveMessage.setOnTouchListener(object : View.OnTouchListener {
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if(isLongclicked){
                        popUp.onTouch(v!!,event!!)
                    }
                    return false
                }
            })
            viewHolder.imageReceive.setOnTouchListener(object :View.OnTouchListener{
                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if(isLongClickedImg){
                        popUp.onTouch(v!!,event!!)
                    }
                    return false
                }
            })
        }
    }

        override fun getItemCount(): Int {
            return itemMessages.size
        }

    }