package com.example.whatsappclone.interfaces

import com.example.whatsappclone.models.User

interface IUsersAdapter {
    fun onItemClicked(user : User)
}