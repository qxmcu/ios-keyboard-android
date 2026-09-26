package org.iosclone.keyboard.dictionary

import android.content.Context
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Manages backup and automatic restoration of learned user dictionaries, bigram habits,
 * and autocorrect rules. Stores backups in public Documents/iOSKeyboard directory
 * to ensure data is never lost even if the user uninstalls and later reinstalls the app.
 */
object UserDictionaryBackupHelper {

    private const val TAG = "UserDictBackup"
    private const val BACKUP_DIR_NAME = "iOSKeyboard"
    private const val BACKUP_FILE_NAME = "autocorrect_backup.json"

    fun getBackupFile(context: Context): File {
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val appDir = File(docsDir, BACKUP_DIR_NAME).apply { mkdirs() }
        return File(appDir, BACKUP_FILE_NAME)
    }

    private fun getInternalBackupFile(context: Context): File {
        val appDir = File(context.filesDir, BACKUP_DIR_NAME).apply { mkdirs() }
        return File(appDir, BACKUP_FILE_NAME)
    }

    fun backup(context: Context, userDb: UserDictionaryDb): Int {
        try {
            val root = JSONObject()
            root.put("version", 1)
            root.put("timestamp", System.currentTimeMillis())

            // 1. Export Words
            val wordsArray = JSONArray()
            val languages = listOf("QWERTY", "SPANISH", "AZERTY", "QWERTZ")
            var totalItems = 0

            for (lang in languages) {
                val words = userDb.getAllWordsForLanguage(lang)
                for ((word, freq) in words) {
                    val obj = JSONObject().apply {
                        put("word", word)
                        put("freq", freq)
                        put("lang", lang)
                    }
                    wordsArray.put(obj)
                    totalItems++
                }
            }
            root.put("user_words", wordsArray)

            // 2. Export Bigrams
            val bigramsArray = JSONArray()
            val allBigrams = userDb.getAllBigrams()
            for ((prev, nextList) in allBigrams) {
                for ((next, freq) in nextList) {
                    val obj = JSONObject().apply {
                        put("prev", prev)
                        put("next", next)
                        put("freq", freq)
                    }
                    bigramsArray.put(obj)
                    totalItems++
                }
            }
            root.put("user_bigrams", bigramsArray)

            // 3. Export Ignored Words
            val ignoredArray = JSONArray()
            val ignoredSet = userDb.getAllIgnoredWords()
            for (ignored in ignoredSet) {
                ignoredArray.put(ignored)
                totalItems++
            }
            root.put("ignored_words", ignoredArray)

            val jsonContent = root.toString(2)

            // Save to Public Documents directory (survives app uninstall)
            try {
                val publicFile = getBackupFile(context)
                publicFile.writeText(jsonContent)
                Log.d(TAG, "Backed up $totalItems items to public storage: ${publicFile.absolutePath}")
            } catch (e: Exception) {
                Log.w(TAG, "Public storage backup failed: ${e.message}")
            }

            // Also save to internal files as fallback
            try {
                val internalFile = getInternalBackupFile(context)
                internalFile.writeText(jsonContent)
            } catch (_: Exception) {}

            return totalItems
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed", e)
            return 0
        }
    }

    fun restoreIfAvailable(context: Context, userDb: UserDictionaryDb): Int {
        try {
            val publicFile = getBackupFile(context)
            val internalFile = getInternalBackupFile(context)
            val targetFile = when {
                publicFile.exists() && publicFile.length() > 0 -> publicFile
                internalFile.exists() && internalFile.length() > 0 -> internalFile
                else -> return 0
            }

            val jsonContent = targetFile.readText()
            if (jsonContent.isBlank()) return 0
            val root = JSONObject(jsonContent)

            var restoredCount = 0

            // Restore user words
            val wordsArray = root.optJSONArray("user_words")
            if (wordsArray != null) {
                for (i in 0 until wordsArray.length()) {
                    val obj = wordsArray.optJSONObject(i) ?: continue
                    val word = obj.optString("word", "")
                    val lang = obj.optString("lang", "QWERTY")
                    if (word.isNotEmpty()) {
                        userDb.insertOrIncrementWord(word, lang)
                        restoredCount++
                    }
                }
            }

            // Restore bigrams
            val bigramsArray = root.optJSONArray("user_bigrams")
            if (bigramsArray != null) {
                for (i in 0 until bigramsArray.length()) {
                    val obj = bigramsArray.optJSONObject(i) ?: continue
                    val prev = obj.optString("prev", "")
                    val next = obj.optString("next", "")
                    if (prev.isNotEmpty() && next.isNotEmpty()) {
                        userDb.learnBigram(prev, next)
                        restoredCount++
                    }
                }
            }

            // Restore ignored words
            val ignoredArray = root.optJSONArray("ignored_words")
            if (ignoredArray != null) {
                for (i in 0 until ignoredArray.length()) {
                    val word = ignoredArray.optString(i, "")
                    if (word.isNotEmpty()) {
                        userDb.addIgnoredWord(word)
                        restoredCount++
                    }
                }
            }

            Log.i(TAG, "Restored $restoredCount learned items from ${targetFile.absolutePath}")
            return restoredCount
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed", e)
            return 0
        }
    }

    fun backupAsync(context: Context, userDb: UserDictionaryDb) {
        CoroutineScope(Dispatchers.IO).launch {
            backup(context, userDb)
        }
    }
}
