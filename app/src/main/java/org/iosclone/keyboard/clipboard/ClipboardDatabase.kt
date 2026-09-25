package org.iosclone.keyboard.clipboard

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ClipboardDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ios_clipboard.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "clips"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TEXT = "text"
        private const val COLUMN_TIMESTAMP = "timestamp"
        private const val COLUMN_PINNED = "is_pinned"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val sql = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT NOT NULL,
                $COLUMN_TIMESTAMP INTEGER,
                $COLUMN_PINNED INTEGER DEFAULT 0
            )
        """.trimIndent()
        db.execSQL(sql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addClip(text: String): Long {
        if (text.isBlank()) return -1
        val db = writableDatabase

        // If identical text exists, update timestamp and move to top
        val cursor = db.query(TABLE_NAME, arrayOf(COLUMN_ID, COLUMN_PINNED), "$COLUMN_TEXT = ?", arrayOf(text), null, null, null)
        val id = if (cursor.moveToFirst()) {
            val existingId = cursor.getLong(0)
            cursor.close()
            val values = ContentValues().apply {
                put(COLUMN_TIMESTAMP, System.currentTimeMillis())
            }
            db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(existingId.toString()))
            existingId
        } else {
            cursor.close()
            val values = ContentValues().apply {
                put(COLUMN_TEXT, text)
                put(COLUMN_TIMESTAMP, System.currentTimeMillis())
                put(COLUMN_PINNED, 0)
            }
            db.insert(TABLE_NAME, null, values)
        }

        // Keep maximum 50 non-pinned items
        pruneOldClips(db)
        return id
    }

    private fun pruneOldClips(db: SQLiteDatabase) {
        db.execSQL("""
            DELETE FROM $TABLE_NAME 
            WHERE $COLUMN_PINNED = 0 
            AND $COLUMN_ID NOT IN (
                SELECT $COLUMN_ID FROM $TABLE_NAME 
                WHERE $COLUMN_PINNED = 0 
                ORDER BY $COLUMN_TIMESTAMP DESC 
                LIMIT 50
            )
        """.trimIndent())
    }

    fun getAllClips(): List<ClipboardEntry> {
        val result = mutableListOf<ClipboardEntry>()
        val db = readableDatabase
        // Pinned first, then newest to oldest
        val cursor = db.query(
            TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "$COLUMN_PINNED DESC, $COLUMN_TIMESTAMP DESC"
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow(COLUMN_ID)
            val textCol = it.getColumnIndexOrThrow(COLUMN_TEXT)
            val timeCol = it.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val pinnedCol = it.getColumnIndexOrThrow(COLUMN_PINNED)

            while (it.moveToNext()) {
                result.add(
                    ClipboardEntry(
                        id = it.getLong(idCol),
                        text = it.getString(textCol),
                        timestamp = it.getLong(timeCol),
                        isPinned = it.getInt(pinnedCol) == 1
                    )
                )
            }
        }
        return result
    }

    fun setPinned(id: Long, isPinned: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PINNED, if (isPinned) 1 else 0)
        }
        db.update(TABLE_NAME, values, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun deleteClip(id: Long) {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))
    }

    fun clearAll() {
        val db = writableDatabase
        db.delete(TABLE_NAME, "$COLUMN_PINNED = 0", null)
    }
}
