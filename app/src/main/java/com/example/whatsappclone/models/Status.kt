package com.example.whatsappclone.models

data class Status(val imageUrl : String = "",val timeStamp : Long){
    constructor() : this("",0)
}
