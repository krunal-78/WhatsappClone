package com.example.whatsappclone.models
// we also need status that are uploaded by user so we make a new class status;
data class UserStatus(val userName:String = "",val userProfilePicture : String = "",val lastUpdatedTime : Long,val allUserStatus : ArrayList<Status>){
    constructor() : this("","",0, emptyList<Status>() as ArrayList<Status>)
}