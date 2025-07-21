package com.ubicate.ubicate.repository

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class UserRepository(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("UserPreferences", Context.MODE_PRIVATE)

    fun generateUniqueUserId(): String {
        return UUID.randomUUID().toString()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString("userId", null)
    }

    fun isBus(): Boolean {
        return sharedPreferences.getBoolean("isBus", false)
    }

    fun saveUser(userId: String, isBus: Boolean) {
        sharedPreferences.edit().putString("userId", userId).apply()
        sharedPreferences.edit().putBoolean("isBus", isBus).apply()
    }
}
