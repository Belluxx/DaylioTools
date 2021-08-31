package com.pietrobellodi.dayliotools.utils

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

/**
 * Provides useful tools to perform firebase tasks
 *
 * @param addUser specifies if the user just started using the app
 * @param listener a listener for firebase events
 */
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

    /**
     * Custom listener for firebase events
     */
    interface OnDataRetrievedListener {
        fun onRetrieved(versionCode: Int, updateUrl: String)
    }

}