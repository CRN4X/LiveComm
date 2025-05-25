package com.example.livecomm

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore("user_prefs")

object UserPreferences {
    private val USER_NAME_KEY = stringPreferencesKey("user_name")

    suspend fun saveUserName(context: Context, name: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME_KEY] = name
        }
    }

    suspend fun getUserName(context: Context): String? {
        return context.dataStore.data
            .map { prefs -> prefs[USER_NAME_KEY] }
            .first()
    }
}
