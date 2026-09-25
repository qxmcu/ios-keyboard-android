package org.iosclone.keyboard.dictionary

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDictionaryDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ios_user_dict.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "user_words"
        private const val COLUMN_WORD = "word"
        private const val COLUMN_FREQUENCY = "frequency"
        private const val COLUMN_LANGUAGE = "language"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createSql = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_WORD TEXT PRIMARY KEY,
                $COLUMN_FREQUENCY INTEGER,
                $COLUMN_LANGUAGE TEXT
            )
        """.trimIndent()
        db.execSQL(createSql)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertOrIncrementWord(word: String, language: String) {
        val db = writableDatabase
        val lower = word.trim().lowercase()
        if (lower.isEmpty()) return

        db.beginTransaction()
        try {
            val cursor = db.query(
                TABLE_NAME,
                arrayOf(COLUMN_FREQUENCY),
                "$COLUMN_WORD = ? AND $COLUMN_LANGUAGE = ?",
                arrayOf(lower, language),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                val currentFreq = cursor.getInt(0)
                cursor.close()
                val values = ContentValues().apply {
                    put(COLUMN_FREQUENCY, currentFreq + 1)
                }
                db.update(
                    TABLE_NAME,
                    values,
                    "$COLUMN_WORD = ? AND $COLUMN_LANGUAGE = ?",
                    arrayOf(lower, language)
                )
            } else {
                cursor.close()
                val values = ContentValues().apply {
                    put(COLUMN_WORD, lower)
                    put(COLUMN_FREQUENCY, 100) // start with respectable user word frequency
                    put(COLUMN_LANGUAGE, language)
                }
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllWordsForLanguage(language: String): List<Pair<String, Int>> {
        val result = mutableListOf<Pair<String, Int>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_NAME,
            arrayOf(COLUMN_WORD, COLUMN_FREQUENCY),
            "$COLUMN_LANGUAGE = ?",
            arrayOf(language),
            null, null, "$COLUMN_FREQUENCY DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val word = it.getString(0)
                val freq = it.getInt(1)
                result.add(Pair(word, freq))
            }
        }
        return result
    }
}
