package org.iosclone.keyboard.dictionary

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite Persistent Storage for user vocabulary, bigrams, and autocorrect habits.
 * Stored locally in app database directory so app updates never erase learned habits.
 */
class UserDictionaryDb(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ios_user_dict.db"
        private const val DATABASE_VERSION = 2

        private const val TABLE_WORDS = "user_words"
        private const val COLUMN_WORD = "word"
        private const val COLUMN_FREQUENCY = "frequency"
        private const val COLUMN_LANGUAGE = "language"
        private const val COLUMN_LAST_USED = "last_used"

        private const val TABLE_BIGRAMS = "user_bigrams"
        private const val COLUMN_PREV_WORD = "prev_word"
        private const val COLUMN_NEXT_WORD = "next_word"
        private const val COLUMN_BIGRAM_FREQ = "frequency"

        private const val TABLE_IGNORED = "user_ignored_words"
        private const val COLUMN_IGNORED_WORD = "word"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Safe migration: preserve all existing user words and data across app upgrades
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_WORDS (
                $COLUMN_WORD TEXT PRIMARY KEY,
                $COLUMN_FREQUENCY INTEGER,
                $COLUMN_LANGUAGE TEXT,
                $COLUMN_LAST_USED INTEGER
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_BIGRAMS (
                $COLUMN_PREV_WORD TEXT,
                $COLUMN_NEXT_WORD TEXT,
                $COLUMN_BIGRAM_FREQ INTEGER,
                PRIMARY KEY ($COLUMN_PREV_WORD, $COLUMN_NEXT_WORD)
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS $TABLE_IGNORED (
                $COLUMN_IGNORED_WORD TEXT PRIMARY KEY,
                $COLUMN_TIMESTAMP INTEGER
            )
        """.trimIndent())
    }

    // --- User Words ---

    fun insertOrIncrementWord(word: String, language: String) {
        val lower = word.trim().lowercase()
        if (lower.isEmpty()) return

        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.query(
                TABLE_WORDS,
                arrayOf(COLUMN_FREQUENCY),
                "$COLUMN_WORD = ? AND $COLUMN_LANGUAGE = ?",
                arrayOf(lower, language),
                null, null, null
            )

            val now = System.currentTimeMillis()
            if (cursor.moveToFirst()) {
                val currentFreq = cursor.getInt(0)
                cursor.close()
                val values = ContentValues().apply {
                    put(COLUMN_FREQUENCY, currentFreq + 1)
                    put(COLUMN_LAST_USED, now)
                }
                db.update(
                    TABLE_WORDS,
                    values,
                    "$COLUMN_WORD = ? AND $COLUMN_LANGUAGE = ?",
                    arrayOf(lower, language)
                )
            } else {
                cursor.close()
                val values = ContentValues().apply {
                    put(COLUMN_WORD, lower)
                    put(COLUMN_FREQUENCY, 100) // Start with respectable user word frequency
                    put(COLUMN_LANGUAGE, language)
                    put(COLUMN_LAST_USED, now)
                }
                db.insertWithOnConflict(TABLE_WORDS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
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
            TABLE_WORDS,
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

    // --- Bigram Habit Learning ---

    fun learnBigram(prevWord: String, nextWord: String) {
        val p = prevWord.trim().lowercase()
        val n = nextWord.trim().lowercase()
        if (p.isEmpty() || n.isEmpty()) return

        val db = writableDatabase
        db.beginTransaction()
        try {
            val cursor = db.query(
                TABLE_BIGRAMS,
                arrayOf(COLUMN_BIGRAM_FREQ),
                "$COLUMN_PREV_WORD = ? AND $COLUMN_NEXT_WORD = ?",
                arrayOf(p, n),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                val freq = cursor.getInt(0)
                cursor.close()
                val values = ContentValues().apply {
                    put(COLUMN_BIGRAM_FREQ, freq + 1)
                }
                db.update(
                    TABLE_BIGRAMS,
                    values,
                    "$COLUMN_PREV_WORD = ? AND $COLUMN_NEXT_WORD = ?",
                    arrayOf(p, n)
                )
            } else {
                cursor.close()
                val values = ContentValues().apply {
                    put(COLUMN_PREV_WORD, p)
                    put(COLUMN_NEXT_WORD, n)
                    put(COLUMN_BIGRAM_FREQ, 1)
                }
                db.insertWithOnConflict(TABLE_BIGRAMS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllBigrams(): Map<String, List<Pair<String, Int>>> {
        val result = mutableMapOf<String, MutableList<Pair<String, Int>>>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BIGRAMS,
            arrayOf(COLUMN_PREV_WORD, COLUMN_NEXT_WORD, COLUMN_BIGRAM_FREQ),
            null, null, null, null, "$COLUMN_BIGRAM_FREQ DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val prev = it.getString(0)
                val next = it.getString(1)
                val freq = it.getInt(2)
                result.getOrPut(prev) { mutableListOf() }.add(Pair(next, freq))
            }
        }
        return result
    }

    // --- Ignored Words (Never Autocorrect) ---

    fun addIgnoredWord(word: String) {
        val lower = word.trim().lowercase()
        if (lower.isEmpty()) return

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_IGNORED_WORD, lower)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        db.insertWithOnConflict(TABLE_IGNORED, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun removeIgnoredWord(word: String) {
        val lower = word.trim().lowercase()
        if (lower.isEmpty()) return

        val db = writableDatabase
        db.delete(TABLE_IGNORED, "$COLUMN_IGNORED_WORD = ?", arrayOf(lower))
    }

    fun getAllIgnoredWords(): Set<String> {
        val set = mutableSetOf<String>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_IGNORED,
            arrayOf(COLUMN_IGNORED_WORD),
            null, null, null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                set.add(it.getString(0))
            }
        }
        return set
    }
}
