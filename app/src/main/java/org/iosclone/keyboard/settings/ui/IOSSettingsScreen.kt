package org.iosclone.keyboard.settings.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.iosclone.keyboard.clipboard.ClipboardDatabase
import org.iosclone.keyboard.layout.LanguageLayout
import org.iosclone.keyboard.settings.KeyboardPreferences
import org.iosclone.keyboard.theme.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IOSSettingsScreen(prefs: KeyboardPreferences) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    // State bindings
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var keyHeight by remember { mutableFloatStateOf(prefs.keyboardHeightFactor) }
    var keyPopups by remember { mutableStateOf(prefs.keyPopupsEnabled) }

    var soundEnabled by remember { mutableStateOf(prefs.soundEnabled) }
    var soundVolume by remember { mutableIntStateOf(prefs.soundVolume) }
    var hapticsEnabled by remember { mutableStateOf(prefs.hapticsEnabled) }
    var hapticsIntensity by remember { mutableIntStateOf(prefs.hapticsIntensity) }

    var autocorrect by remember { mutableStateOf(prefs.autocorrectEnabled) }
    var predictiveText by remember { mutableStateOf(prefs.predictiveTextEnabled) }
    var gestureTyping by remember { mutableStateOf(prefs.gestureTypingEnabled) }
    var doubleSpacePeriod by remember { mutableStateOf(prefs.doubleSpacePeriodEnabled) }
    var spacebarTrackpad by remember { mutableStateOf(prefs.spacebarTrackpadEnabled) }

    var activeLanguage by remember { mutableStateOf(prefs.activeLanguage) }

    var clipboardHistory by remember { mutableStateOf(prefs.clipboardHistoryEnabled) }
    var clipboardAutoSuggest by remember { mutableStateOf(prefs.clipboardAutoSuggest) }

    var translationEnabled by remember { mutableStateOf(prefs.translationEnabled) }
    var targetLanguage by remember { mutableStateOf(prefs.translationTargetLang) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "iOS Keyboard",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (dark) IOSDarkText else IOSLightText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (dark) IOSDarkBackground else IOSLightBackground
                )
            )
        },
        containerColor = if (dark) IOSDarkBackground else IOSLightBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Section 1: Activation
            IOSSectionHeader(title = "Activation")
            IOSGroupedCard {
                IOSSettingsActionRow(
                    title = "Enable Keyboard",
                    subtitle = "Enable iOS Keyboard in system settings",
                    onClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        context.startActivity(intent)
                    }
                )
                IOSSettingsActionRow(
                    title = "Switch Active Keyboard",
                    subtitle = "Select iOS Keyboard as default",
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    showDivider = false
                )
            }

            // Section 2: Appearance
            IOSSectionHeader(title = "Appearance")
            IOSGroupedCard {
                IOSSettingsActionRow(
                    title = "Theme",
                    trailingText = themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                    onClick = {
                        val options = arrayOf("Match System", "iOS Light", "iOS Dark")
                        val currentIdx = when (themeMode) {
                            ThemeMode.SYSTEM -> 0
                            ThemeMode.LIGHT -> 1
                            ThemeMode.DARK -> 2
                        }
                        AlertDialog.Builder(context)
                            .setTitle("Select Theme")
                            .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                                val newMode = when (which) {
                                    1 -> ThemeMode.LIGHT
                                    2 -> ThemeMode.DARK
                                    else -> ThemeMode.SYSTEM
                                }
                                themeMode = newMode
                                prefs.themeMode = newMode
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                )
                IOSSettingsSliderRow(
                    title = "Keyboard Height Scale",
                    value = keyHeight * 100f,
                    valueRange = 80f..130f,
                    onValueChange = {
                        val factor = it / 100f
                        keyHeight = factor
                        prefs.keyboardHeightFactor = factor
                    },
                    valueDisplay = "${(keyHeight * 100).toInt()}%"
                )
                IOSSettingsToggleRow(
                    title = "Key Popups",
                    subtitle = "Show enlarged magnifier bubble above finger",
                    checked = keyPopups,
                    onCheckedChange = {
                        keyPopups = it
                        prefs.keyPopupsEnabled = it
                    },
                    showDivider = false
                )
            }

            // Section 3: Audio & Haptics
            IOSSectionHeader(title = "Key Sounds & Haptics")
            IOSGroupedCard {
                IOSSettingsToggleRow(
                    title = "Keystroke Sounds",
                    checked = soundEnabled,
                    onCheckedChange = {
                        soundEnabled = it
                        prefs.soundEnabled = it
                    }
                )
                if (soundEnabled) {
                    IOSSettingsSliderRow(
                        title = "Sound Volume",
                        value = soundVolume.toFloat(),
                        valueRange = 10f..100f,
                        onValueChange = {
                            soundVolume = it.toInt()
                            prefs.soundVolume = soundVolume
                        },
                        valueDisplay = "$soundVolume%"
                    )
                }
                IOSSettingsToggleRow(
                    title = "Haptic Feedback",
                    subtitle = "Apple Taptic Engine micro-click replication",
                    checked = hapticsEnabled,
                    onCheckedChange = {
                        hapticsEnabled = it
                        prefs.hapticsEnabled = it
                    }
                )
                if (hapticsEnabled) {
                    IOSSettingsSliderRow(
                        title = "Haptic Intensity",
                        value = hapticsIntensity.toFloat(),
                        valueRange = 10f..100f,
                        onValueChange = {
                            hapticsIntensity = it.toInt()
                            prefs.hapticsIntensity = hapticsIntensity
                        },
                        valueDisplay = "$hapticsIntensity%",
                        showDivider = false
                    )
                }
            }

            // Section 4: Typing & Autocorrect
            IOSSectionHeader(title = "Typing & Autocorrect")
            IOSGroupedCard {
                IOSSettingsToggleRow(
                    title = "Auto-Correction",
                    subtitle = "Automatically corrects typos using offline dictionary",
                    checked = autocorrect,
                    onCheckedChange = {
                        autocorrect = it
                        prefs.autocorrectEnabled = it
                    }
                )
                IOSSettingsToggleRow(
                    title = "Predictive Text Bar",
                    subtitle = "Show 3-column iOS suggestion strip",
                    checked = predictiveText,
                    onCheckedChange = {
                        predictiveText = it
                        prefs.predictiveTextEnabled = it
                    }
                )
                IOSSettingsToggleRow(
                    title = "Glide / Gesture Typing",
                    subtitle = "Continuous swipe gestures across letters",
                    checked = gestureTyping,
                    onCheckedChange = {
                        gestureTyping = it
                        prefs.gestureTypingEnabled = it
                    }
                )
                IOSSettingsToggleRow(
                    title = "Double-Tap Space for Period",
                    checked = doubleSpacePeriod,
                    onCheckedChange = {
                        doubleSpacePeriod = it
                        prefs.doubleSpacePeriodEnabled = it
                    }
                )
                IOSSettingsToggleRow(
                    title = "Spacebar Cursor Trackpad",
                    subtitle = "Hold spacebar and drag to smoothly move cursor",
                    checked = spacebarTrackpad,
                    onCheckedChange = {
                        spacebarTrackpad = it
                        prefs.spacebarTrackpadEnabled = it
                    },
                    showDivider = false
                )
            }

            // Section 5: Languages & Layouts
            IOSSectionHeader(title = "Languages & Layouts")
            IOSGroupedCard {
                IOSSettingsActionRow(
                    title = "Primary Layout",
                    trailingText = activeLanguage.displayName,
                    onClick = {
                        val layouts = LanguageLayout.entries.toTypedArray()
                        val names = layouts.map { it.displayName }.toTypedArray()
                        val currentIdx = layouts.indexOf(activeLanguage)

                        AlertDialog.Builder(context)
                            .setTitle("Select Keyboard Layout")
                            .setSingleChoiceItems(names, currentIdx) { dialog, which ->
                                val selected = layouts[which]
                                activeLanguage = selected
                                prefs.activeLanguage = selected
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    },
                    showDivider = false
                )
            }

            // Section 6: Clipboard Manager
            IOSSectionHeader(title = "Clipboard Manager")
            IOSGroupedCard {
                IOSSettingsToggleRow(
                    title = "Clipboard History",
                    subtitle = "Save recent clips with pin and delete options",
                    checked = clipboardHistory,
                    onCheckedChange = {
                        clipboardHistory = it
                        prefs.clipboardHistoryEnabled = it
                    }
                )
                IOSSettingsToggleRow(
                    title = "Suggest Recent Clipboard in Strip",
                    checked = clipboardAutoSuggest,
                    onCheckedChange = {
                        clipboardAutoSuggest = it
                        prefs.clipboardAutoSuggest = it
                    }
                )
                IOSSettingsActionRow(
                    title = "Clear Clipboard History",
                    showChevron = false,
                    onClick = {
                        ClipboardDatabase(context).clearAll()
                        Toast.makeText(context, "Clipboard history cleared", Toast.LENGTH_SHORT).show()
                    },
                    showDivider = false
                )
            }

            // Section 7: Inline Translation
            IOSSectionHeader(title = "Inline Translation")
            IOSGroupedCard {
                IOSSettingsToggleRow(
                    title = "Inline Translation Bar",
                    subtitle = "Real-time translation inside keyboard strip",
                    checked = translationEnabled,
                    onCheckedChange = {
                        translationEnabled = it
                        prefs.translationEnabled = it
                    }
                )
                IOSSettingsActionRow(
                    title = "Target Language",
                    trailingText = targetLanguage.uppercase(),
                    onClick = {
                        val targets = arrayOf("Spanish (es)", "French (fr)", "German (de)", "Russian (ru)", "Arabic (ar)")
                        val codes = arrayOf("es", "fr", "de", "ru", "ar")
                        val currentIdx = codes.indexOf(targetLanguage).coerceAtLeast(0)

                        AlertDialog.Builder(context)
                            .setTitle("Select Target Language")
                            .setSingleChoiceItems(targets, currentIdx) { dialog, which ->
                                targetLanguage = codes[which]
                                prefs.translationTargetLang = codes[which]
                                dialog.dismiss()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    },
                    showDivider = false
                )
            }

            // Section 8: About & Privacy
            IOSSectionHeader(title = "About & Privacy")
            IOSGroupedCard {
                IOSSettingsActionRow(
                    title = "100% Offline Privacy Guarantee",
                    subtitle = "No keylogging, zero telemetry, full offline guarantee",
                    onClick = {
                        AlertDialog.Builder(context)
                            .setTitle("Privacy Guarantee")
                            .setMessage("iOS Keyboard for Android is engineered with privacy as a foundational principle. Keystrokes, passwords, and clipboard data are processed entirely on-device and never leave your phone. Zero network analytics or tracking.")
                            .setPositiveButton("OK", null)
                            .show()
                    }
                )
                IOSSettingsActionRow(
                    title = "Version",
                    trailingText = "1.0.0 (OSS Release)",
                    showChevron = false,
                    onClick = {}
                )
                IOSSettingsActionRow(
                    title = "Open Source License",
                    trailingText = "Apache 2.0",
                    showChevron = false,
                    onClick = {},
                    showDivider = false
                )
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}
