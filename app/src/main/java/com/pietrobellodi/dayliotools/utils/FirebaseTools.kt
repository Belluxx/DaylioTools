package com.pietrobellodi.dayliotools.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener


class FirebaseTools(private val addUser: Boolean, private val listener: OnDataRetrievedListener) {

    var usersCount = -1
    var versionCode = -1
    var updateUrl = ""

    init {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("dayliotools")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(data: DataSnapshot) {
                usersCount = data.child("launchs").getValue(Int::class.java)!!
                versionCode = data.child("version").getValue(Int::class.java)!!
                updateUrl = data.child("update_url").getValue(String::class.java)!!
                listener.onRetrieved(versionCode, updateUrl)

                if (addUser) {
                    ref.child("launchs").setValue(usersCount + 1)
                }
            }

            override fun onCancelled(data: DatabaseError) {
            }

        })
    }

    interface OnDataRetrievedListener {
        fun onRetrieved(versionCode: Int, updateUrl: String)
    }

}