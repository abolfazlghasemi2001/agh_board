package com.pinboard.keyboard.util

import com.pinboard.keyboard.data.Pin
import org.json.JSONArray
import org.json.JSONObject

/**
 * Handles converting the pin list to/from JSON for backup and restore.
 * Uses the built-in org.json classes so no extra dependency is required.
 */
object BackupManager {

    private const val KEY_TITLE = "title"
    private const val KEY_TEXT = "text"
    private const val KEY_CATEGORY = "category"
    private const val KEY_FAVORITE = "isFavorite"
    private const val KEY_USE_COUNT = "useCount"
    private const val KEY_LAST_USED = "lastUsed"
    private const val KEY_CREATED_AT = "createdAt"

    fun pinsToJson(pins: List<Pin>): String {
        val array = JSONArray()
        pins.forEach { pin ->
            val obj = JSONObject()
            obj.put(KEY_TITLE, pin.title)
            obj.put(KEY_TEXT, pin.text)
            obj.put(KEY_CATEGORY, pin.category)
            obj.put(KEY_FAVORITE, pin.isFavorite)
            obj.put(KEY_USE_COUNT, pin.useCount)
            obj.put(KEY_LAST_USED, pin.lastUsed)
            obj.put(KEY_CREATED_AT, pin.createdAt)
            array.put(obj)
        }
        val root = JSONObject()
        root.put("version", 1)
        root.put("pins", array)
        return root.toString(2)
    }

    /** Throws if the JSON is malformed; caller should catch and show an error. */
    fun jsonToPins(json: String): List<Pin> {
        val root = JSONObject(json)
        val array = root.getJSONArray("pins")
        val result = mutableListOf<Pin>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                Pin(
                    // id is intentionally left at default (0) so Room treats every
                    // restored pin as a brand-new row and avoids id collisions.
                    title = obj.getString(KEY_TITLE),
                    text = obj.getString(KEY_TEXT),
                    category = obj.optString(KEY_CATEGORY, DEFAULT_CATEGORY),
                    isFavorite = obj.optBoolean(KEY_FAVORITE, false),
                    useCount = obj.optInt(KEY_USE_COUNT, 0),
                    lastUsed = obj.optLong(KEY_LAST_USED, System.currentTimeMillis()),
                    createdAt = obj.optLong(KEY_CREATED_AT, System.currentTimeMillis())
                )
            )
        }
        return result
    }
}
