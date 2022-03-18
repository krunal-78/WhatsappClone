package com.example.whatsappclone.models

data class User(val userId:String = "", val userName:String = "", val phoneNumber:String ="", val profileImageUrl : String=""){
    var lastMessage : String = ""
    var lastMessageTime : Long = 0
    val token : String = ""
}