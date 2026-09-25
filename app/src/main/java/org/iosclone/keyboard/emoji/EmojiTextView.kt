package org.iosclone.keyboard.emoji

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Custom TextView that applies the bundled Apple Color Emoji font
 * to ensure authentic iOS glyph rendering across all Android OEMs.
 */
class EmojiTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    companion object {
        private var bundledEmojiTypeface: Typeface? = null
        private var typefaceLoaded = false

        fun getBundledTypeface(context: Context): Typeface? {
            if (!typefaceLoaded) {
                try {
                    bundledEmojiTypeface = Typeface.createFromAsset(context.assets, "fonts/AppleColorEmoji.ttf")
                } catch (e: Exception) {
                    // Fall back to default system typeface if font is unavailable or unsupported on older API
                    bundledEmojiTypeface = Typeface.DEFAULT
                }
                typefaceLoaded = true
            }
            return bundledEmojiTypeface
        }
    }

    init {
        applyBundledTypeface()
    }

    private fun applyBundledTypeface() {
        val tf = getBundledTypeface(context)
        if (tf != null && tf != Typeface.DEFAULT) {
            typeface = tf
        }
    }
}
