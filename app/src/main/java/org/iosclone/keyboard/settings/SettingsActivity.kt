package org.iosclone.keyboard.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.iosclone.keyboard.settings.ui.IOSSettingsScreen
import org.iosclone.keyboard.settings.ui.IOSSettingsTheme

class SettingsActivity : ComponentActivity() {

    private lateinit var preferences: KeyboardPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = KeyboardPreferences(this)

        setContent {
            IOSSettingsTheme {
                IOSSettingsScreen(prefs = preferences)
            }
        }
    }
}
