package io.github.nicechester.omok.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("preferences")

object PreferencesManager {
    private val PLAYER_NAME_KEY = stringPreferencesKey("playerName")
    private val TIMER_PREFERENCE_KEY = intPreferencesKey("timerDuration")
    private val NOTIFICATIONS_KEY = booleanPreferencesKey("notificationsEnabled")

    fun getPlayerName(context: Context): Flow<String> =
        context.dataStore.data.map { it[PLAYER_NAME_KEY] ?: "" }

    suspend fun setPlayerName(context: Context, name: String) {
        context.dataStore.edit { it[PLAYER_NAME_KEY] = name }
    }

    fun getTimerPreference(context: Context): Flow<Int> =
        context.dataStore.data.map { it[TIMER_PREFERENCE_KEY] ?: 0 }

    suspend fun setTimerPreference(context: Context, seconds: Int) {
        context.dataStore.edit { it[TIMER_PREFERENCE_KEY] = seconds }
    }

    fun getNotificationsEnabled(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[NOTIFICATIONS_KEY] ?: true }

    suspend fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_KEY] = enabled }
    }

    fun isFirstRun(context: Context): Flow<Boolean> =
        getPlayerName(context).map { it.isEmpty() }

    suspend fun getPlayerNameOnce(context: Context): String =
        getPlayerName(context).first()
}
