package io.github.nicechester.omok.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.nicechester.omok.data.model.RecentRoom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.recentRoomsDataStore by preferencesDataStore(name = "recent_rooms")

object RecentRoomsManager {
    private val RECENT_ROOMS_KEY = stringPreferencesKey("recentRooms")
    private const val MAX_COUNT = 10

    fun getRecentRooms(context: Context): Flow<List<RecentRoom>> {
        return context.recentRoomsDataStore.data.map { prefs ->
            val jsonString = prefs[RECENT_ROOMS_KEY] ?: return@map emptyList()
            try {
                parseRoomsJson(jsonString)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun recordPlay(
        context: Context,
        code: String,
        aiDifficulty: String? = null
    ) {
        context.recentRoomsDataStore.edit { prefs ->
            val jsonString = prefs[RECENT_ROOMS_KEY] ?: "[]"
            val rooms = try {
                parseRoomsJson(jsonString).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }

            rooms.removeAll { it.code == code }
            rooms.add(0, RecentRoom(code, System.currentTimeMillis(), aiDifficulty))
            if (rooms.size > MAX_COUNT) rooms.subList(MAX_COUNT, rooms.size).clear()

            prefs[RECENT_ROOMS_KEY] = roomsToJson(rooms)
        }
    }

    suspend fun remove(context: Context, code: String) {
        context.recentRoomsDataStore.edit { prefs ->
            val jsonString = prefs[RECENT_ROOMS_KEY] ?: return@edit
            val rooms = parseRoomsJson(jsonString).filter { it.code != code }
            prefs[RECENT_ROOMS_KEY] = roomsToJson(rooms)
        }
    }

    suspend fun restore(context: Context, room: RecentRoom, index: Int) {
        context.recentRoomsDataStore.edit { prefs ->
            val jsonString = prefs[RECENT_ROOMS_KEY] ?: "[]"
            val rooms = parseRoomsJson(jsonString).toMutableList()
            rooms.add(index.coerceIn(0, rooms.size), room)
            prefs[RECENT_ROOMS_KEY] = roomsToJson(rooms)
        }
    }

    private fun parseRoomsJson(jsonString: String): List<RecentRoom> {
        val array = JSONArray(jsonString)
        return (0 until array.length()).map { i ->
            val obj = array.getJSONObject(i)
            RecentRoom(
                code = obj.getString("code"),
                lastPlayedAt = obj.getLong("lastPlayedAt"),
                aiDifficulty = if (obj.has("aiDifficulty")) obj.optString("aiDifficulty") else null
            )
        }
    }

    private fun roomsToJson(rooms: List<RecentRoom>): String {
        return JSONArray(rooms.map { room ->
            JSONObject().apply {
                put("code", room.code)
                put("lastPlayedAt", room.lastPlayedAt)
                put("aiDifficulty", room.aiDifficulty)
            }
        }).toString()
    }
}
