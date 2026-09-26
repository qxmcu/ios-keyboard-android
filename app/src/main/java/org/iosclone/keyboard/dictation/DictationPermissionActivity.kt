package org.iosclone.keyboard.dictation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Transparent helper activity that requests the RECORD_AUDIO runtime permission
 * on behalf of the InputMethodService (which cannot directly request runtime permissions).
 */
class DictationPermissionActivity : ComponentActivity() {

    companion object {
        const val REQUEST_CODE_AUDIO = 2001
        var onPermissionResult: ((Boolean) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            onPermissionResult?.invoke(true)
            finish()
            return
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_CODE_AUDIO
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_AUDIO) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (granted) {
                Toast.makeText(this, "🎙 Microphone enabled! Tap mic to dictate.", Toast.LENGTH_SHORT).show()
                onPermissionResult?.invoke(true)
            } else {
                Toast.makeText(this, "Microphone permission is required for voice dictation", Toast.LENGTH_LONG).show()
                onPermissionResult?.invoke(false)
            }
        }
        finish()
    }
}
