package com.example.unison2

class User (val uid: String, val email: String, val profile_pic: String, var events: String, val group1: String,
            val group2: String, val group1members: String, val group2members: String, val message: String, val name: String){

    constructor() : this("", "", "", "", "", "", "", "", "", "")


}