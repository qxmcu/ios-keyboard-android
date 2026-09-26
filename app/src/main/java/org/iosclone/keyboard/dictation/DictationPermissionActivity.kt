package org.iosclone.keyboard.dictation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * Transparent helper activity that requests the RECORD_AUDIO runtime permission
 * using the modern AndroidX Activity Result API on behalf of the InputMethodService.
 */
class DictationPermissionActivity : ComponentActivity() {

    companion object {
        var onPermissionResult: ((Boolean) -> Unit)? = null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "🎙 Microphone enabled! Tap mic to dictate.", Toast.LENGTH_SHORT).show()
            onPermissionResult?.invoke(true)
        } else {
            Toast.makeText(this, "Microphone permission is required for voice dictation", Toast.LENGTH_LONG).show()
            onPermissionResult?.invoke(false)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onPermissionResult?.invoke(true)
            finish()
            return
        }

        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
}
