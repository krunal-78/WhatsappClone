package com.example.whatsappclone.models

data class Message( val messageText : String? = "", val senderId :String = "", val sentTime : Long){
    var messageId : String = ""
    var emojiReact : Int = -1
    constructor() : this("","",0)
}