package org.iosclone.keyboard.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.content.ContextCompat
import org.iosclone.keyboard.emoji.EmojiCategory
import org.iosclone.keyboard.emoji.EmojiData
import org.iosclone.keyboard.emoji.EmojiItem
import org.iosclone.keyboard.emoji.EmojiSkinTone
import org.iosclone.keyboard.emoji.EmojiTextView
import org.iosclone.keyboard.theme.ThemeColors

class EmojiPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onEmojiSelected: ((String) -> Unit)? = null
    var onBackspaceClicked: (() -> Unit)? = null
    var onBackToAlphaClicked: (() -> Unit)? = null

    private val searchEditText: EditText
    private val emojiScrollView: ScrollView
    private val emojiGrid: GridLayout
    private val categoryIconsLayout: LinearLayout
    private val backToAlphaBtn: TextView
    private val backspaceBtn: ImageView

    private var currentCategory: EmojiCategory = EmojiCategory.SMILEYS
    private val categoryIconViews = mutableMapOf<EmojiCategory, ImageView>()
    private val recentEmojis = mutableListOf<String>()

    init {
        orientation = VERTICAL
        val density = resources.displayMetrics.density

        // 1. Top Search Bar
        val searchContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (12 * density).toInt()
            val padV = (6 * density).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        searchEditText = EditText(context).apply {
            hint = "Search Emoji"
            textSize = 14f
            maxLines = 1
            isSingleLine = true
            val pad = (8 * density).toInt()
            setPadding(pad, pad, pad, pad)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (34 * density).toInt())
            background = ContextCompat.getDrawable(context, android.R.drawable.editbox_background_normal)
        }
        searchContainer.addView(searchEditText)
        addView(searchContainer)

        // 2. Emoji Grid Container (Scrollable)
        emojiScrollView = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
            isVerticalScrollBarEnabled = true
        }

        emojiGrid = GridLayout(context).apply {
            columnCount = 8
            alignmentMode = GridLayout.ALIGN_BOUNDS
            useDefaultMargins = false
            layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            val gridPad = (4 * density).toInt()
            setPadding(gridPad, gridPad, gridPad, gridPad)
        }
        emojiScrollView.addView(emojiGrid)
        addView(emojiScrollView)

        // 3. Bottom Navigation Bar
        val bottomNav = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (44 * density).toInt())
            val navPad = (6 * density).toInt()
            setPadding(navPad, 0, navPad, 0)
        }

        backToAlphaBtn = TextView(context).apply {
            text = "ABC"
            textSize = 15f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams((50 * density).toInt(), LayoutParams.MATCH_PARENT)
            setOnClickListener { onBackToAlphaClicked?.invoke() }
        }
        bottomNav.addView(backToAlphaBtn)

        val catScrollView = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            isHorizontalScrollBarEnabled = false
        }

        categoryIconsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        }

        // Add category icon buttons
        val catIconSize = (22 * density).toInt()
        val catIconPad = (8 * density).toInt()

        for (cat in EmojiCategory.entries) {
            val iconView = ImageView(context).apply {
                layoutParams = LayoutParams(catIconSize + (catIconPad * 2), LayoutParams.MATCH_PARENT)
                setPadding(catIconPad, catIconPad, catIconPad, catIconPad)
                val id = context.resources.getIdentifier(cat.iconResName, "drawable", context.packageName)
                if (id != 0) setImageResource(id)
                setOnClickListener {
                    selectCategory(cat)
                }
            }
            categoryIconViews[cat] = iconView
            categoryIconsLayout.addView(iconView)
        }
        catScrollView.addView(categoryIconsLayout)
        bottomNav.addView(catScrollView)

        // Backspace key
        backspaceBtn = ImageView(context).apply {
            layoutParams = LayoutParams((44 * density).toInt(), LayoutParams.MATCH_PARENT)
            val bsPad = (10 * density).toInt()
            setPadding(bsPad, bsPad, bsPad, bsPad)
            val id = context.resources.getIdentifier("ic_backspace", "drawable", context.packageName)
            if (id != 0) setImageResource(id)
            setOnClickListener { onBackspaceClicked?.invoke() }
        }
        bottomNav.addView(backspaceBtn)
        addView(bottomNav)

        // Setup search filter listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    displayEmojis(EmojiData.search(query))
                } else {
                    selectCategory(currentCategory)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Initial populate
        selectCategory(EmojiCategory.SMILEYS)
    }

    private fun selectCategory(category: EmojiCategory) {
        currentCategory = category
        updateCategoryIconHighlights()

        val emojis = if (category == EmojiCategory.RECENTS) {
            if (recentEmojis.isEmpty()) EmojiData.getEmojisForCategory(EmojiCategory.SMILEYS).take(32)
            else recentEmojis.mapNotNull { u -> EmojiData.allEmojis.firstOrNull { it.unicode == u } }
        } else {
            EmojiData.getEmojisForCategory(category)
        }

        displayEmojis(emojis)
        emojiScrollView.scrollTo(0, 0)
    }

    private fun updateCategoryIconHighlights() {
        for ((cat, icon) in categoryIconViews) {
            val isSelected = (cat == currentCategory)
            icon.alpha = if (isSelected) 1.0f else 0.45f
        }
    }

    private fun displayEmojis(items: List<EmojiItem>) {
        emojiGrid.removeAllViews()
        val density = resources.displayMetrics.density
        val screenWidth = resources.displayMetrics.widthPixels
        val itemSize = (screenWidth / 8).coerceAtLeast((40 * density).toInt())

        for (item in items) {
            val emojiTv = EmojiTextView(context).apply {
                text = item.unicode
                textSize = 28f
                gravity = Gravity.CENTER
                layoutParams = GridLayout.LayoutParams().apply {
                    width = itemSize
                    height = itemSize
                }

                setOnClickListener {
                    onEmojiClicked(item.unicode)
                }

                if (item.supportsSkinTone) {
                    setOnLongClickListener {
                        showSkinToneSelector(item, this)
                        true
                    }
                }
            }
            emojiGrid.addView(emojiTv)
        }
    }

    private fun onEmojiClicked(unicode: String) {
        if (!recentEmojis.contains(unicode)) {
            recentEmojis.add(0, unicode)
            if (recentEmojis.size > 40) recentEmojis.removeLast()
        }
        onEmojiSelected?.invoke(unicode)
    }

    private fun showSkinToneSelector(item: EmojiItem, anchor: View) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val container = LinearLayout(context).apply {
            orientation = HORIZONTAL
            val pad = (6 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = ContextCompat.getDrawable(context, android.R.drawable.dialog_holo_light_frame)
        }

        for (tone in EmojiSkinTone.entries) {
            val variant = tone.applyTo(item.unicode)
            val btn = EmojiTextView(context).apply {
                text = variant
                textSize = 26f
                gravity = Gravity.CENTER
                val size = (42 * density).toInt()
                layoutParams = LayoutParams(size, size)
                setOnClickListener {
                    onEmojiClicked(variant)
                    dialog.dismiss()
                }
            }
            container.addView(btn)
        }

        dialog.setContentView(container)
        dialog.show()
    }

    fun applyTheme(theme: ThemeColors) {
        setBackgroundColor(theme.keyboardBackground)
        emojiScrollView.setBackgroundColor(theme.keyboardBackground)

        searchEditText.setTextColor(theme.textPrimary)
        searchEditText.setHintTextColor(theme.textSecondary)

        backToAlphaBtn.setTextColor(theme.textPrimary)
        backspaceBtn.setColorFilter(theme.textPrimary)

        for ((_, icon) in categoryIconViews) {
            icon.setColorFilter(theme.textPrimary)
        }
        updateCategoryIconHighlights()
    }
}
