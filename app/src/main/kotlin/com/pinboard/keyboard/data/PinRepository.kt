package com.pinboard.keyboard.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PinRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).pinDao()

    val allPins: Flow<List<Pin>> = dao.getAll()

    /** One-shot snapshot of the current pins, used for exporting a backup. */
    suspend fun snapshot(): List<Pin> = allPins.first()

    /** Inserts a batch of pins (used when restoring a backup). Existing pins are kept. */
    suspend fun insertAll(pins: List<Pin>) {
        pins.forEach { dao.insert(it) }
    }

    suspend fun insert(pin: Pin): Long = dao.insert(pin)

    suspend fun update(pin: Pin) = dao.update(pin)

    suspend fun delete(pin: Pin) = dao.delete(pin)

    /** Called whenever a pin's text is inserted into a text field via the keyboard. */
    suspend fun markUsed(pin: Pin) {
        dao.update(
            pin.copy(
                useCount = pin.useCount + 1,
                lastUsed = System.currentTimeMillis()
            )
        )
    }
}
