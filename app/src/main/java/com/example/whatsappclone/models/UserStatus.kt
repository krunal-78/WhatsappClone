package com.example.whatsappclone.models
// we also need status that are uploaded by user so we make a new class status;
data class UserStatus(var userName:String = "", var userProfilePicture : String = "", var lastUpdatedTime : Long){
    var allUserStatus : ArrayList<Status>? = null
    constructor() : this("","",0)
}
