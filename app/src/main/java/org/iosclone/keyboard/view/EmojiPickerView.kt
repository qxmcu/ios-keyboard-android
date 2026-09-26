package org.iosclone.keyboard.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.iosclone.keyboard.emoji.DeviceDetector
import org.iosclone.keyboard.emoji.EmojiCategory
import org.iosclone.keyboard.emoji.EmojiData
import org.iosclone.keyboard.emoji.EmojiItem
import org.iosclone.keyboard.emoji.EmojiSkinTone
import org.iosclone.keyboard.emoji.EmojiTextView
import org.iosclone.keyboard.emoji.ZFontAutomator
import org.iosclone.keyboard.theme.ThemeColors

class EmojiPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    var onEmojiSelected: ((String) -> Unit)? = null
    var onEmojiLarpRequested: ((String) -> Unit)? = null
    var onBackspaceClicked: (() -> Unit)? = null
    var onBackToAlphaClicked: (() -> Unit)? = null
    var onGlobeClicked: (() -> Unit)? = null
    var onDictationClicked: (() -> Unit)? = null

    private val searchEditText: EditText
    private val searchPill: LinearLayout
    private val searchContainer: LinearLayout
    private val recyclerView: RecyclerView
    private val bottomNav: LinearLayout
    private val backToAlphaBtn: TextView
    private val backspaceBtn: ImageView
    private val bottomBarLayout: LinearLayout
    private val globeIconView: ImageView
    private val micIconView: ImageView

    private var currentCategory: EmojiCategory = EmojiCategory.SMILEYS
    private val categoryIconViews = mutableMapOf<EmojiCategory, ImageView>()
    private val recentEmojis = mutableListOf<String>()

    private var currentTheme: ThemeColors = ThemeColors.Light
    private val adapter: EmojiRecyclerAdapter

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.TRANSPARENT)
        val density = resources.displayMetrics.density

        // 1. Top Search Bar
        searchContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padH = (12 * density).toInt()
            val padV = (5 * density).toInt()
            setPadding(padH, padV, padH, padV)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (44 * density).toInt())
        }

        searchPill = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padPillH = (10 * density).toInt()
            val padPillV = (6 * density).toInt()
            setPadding(padPillH, padPillV, padPillH, padPillV)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (34 * density).toInt())
            background = GradientDrawable().apply {
                setColor(0x1F000000)
                cornerRadius = 17f * density
            }
        }

        val searchIcon = TextView(context).apply {
            text = "🔍"
            textSize = 13f
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = (6 * density).toInt()
            }
        }

        searchEditText = EditText(context).apply {
            hint = "Search Emoji"
            textSize = 14f
            maxLines = 1
            isSingleLine = true
            background = null
            setPadding(0, 0, 0, 0)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        }

        searchPill.addView(searchIcon)
        searchPill.addView(searchEditText)
        searchContainer.addView(searchPill)
        addView(searchContainer)

        // 1b. Smart 1-Tap iOS Emoji Setup Banner for Non-Nothing Devices (Samsung, Xiaomi, Oppo, etc.)
        val isNothing = DeviceDetector.isNothingPhone()
        if (!isNothing) {
            val shortBrand = DeviceDetector.getShortBrandName()
            val bannerLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                val padH = (12 * density).toInt()
                val padV = (5 * density).toInt()
                setPadding(padH, padV, padH, padV)
                val mH = (12 * density).toInt()
                val mB = (4 * density).toInt()
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (30 * density).toInt()).apply {
                    setMargins(mH, 0, mH, mB)
                }
                background = GradientDrawable().apply {
                    setColor(0x1F007AFF)
                    cornerRadius = 15f * density
                    setStroke((1 * density).toInt(), 0x33007AFF)
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    ZFontAutomator.autoApplyIOSFont(context, windowToken)
                }
            }

            val bannerTv = TextView(context).apply {
                text = "✨ 1-Tap Apply iOS 26.4 Emojis ($shortBrand)"
                textSize = 12f
                setTextColor(Color.parseColor("#007AFF"))
                typeface = Typeface.DEFAULT_BOLD
            }
            bannerLayout.addView(bannerTv)
            addView(bannerLayout)
        }

        // 2. Ultra-Smooth RecyclerView Emoji Grid (7 columns matching iOS 27)
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
            layoutManager = GridLayoutManager(context, 7)
            setHasFixedSize(true)
            setItemViewCacheSize(64)
            val gridPad = (4 * density).toInt()
            setPadding(gridPad, 0, gridPad, 0)
        }

        adapter = EmojiRecyclerAdapter(
            onItemClick = { emoji -> onEmojiClicked(emoji) },
            onItemLongClick = { item, anchor -> showEmojiLarpPopup(item, anchor) }
        )
        recyclerView.adapter = adapter
        addView(recyclerView)

        // 3. Bottom Bar: ABC, Category Strip, Backspace (Evenly distributed for all screens)
        bottomNav = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (42 * density).toInt())
            val navPad = (8 * density).toInt()
            setPadding(navPad, 0, navPad, 0)
        }

        backToAlphaBtn = TextView(context).apply {
            text = "ABC"
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                val pad = (6 * density).toInt()
                setPadding(pad, 0, pad, 0)
                marginEnd = (4 * density).toInt()
            }
            background = null
            setOnClickListener { onBackToAlphaClicked?.invoke() }
        }
        bottomNav.addView(backToAlphaBtn)

        val categoryIconsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
        }

        val catIconPad = (4 * density).toInt()
        val catTargetH = (32 * density).toInt()

        for (cat in EmojiCategory.entries) {
            val iconView = ImageView(context).apply {
                layoutParams = LayoutParams(0, catTargetH, 1.0f)
                setPadding(catIconPad, catIconPad, catIconPad, catIconPad)
                scaleType = ImageView.ScaleType.FIT_CENTER
                val id = context.resources.getIdentifier(cat.iconResName, "drawable", context.packageName)
                if (id != 0) setImageResource(id)
                setOnClickListener { selectCategory(cat) }
            }
            categoryIconViews[cat] = iconView
            categoryIconsLayout.addView(iconView)
        }
        bottomNav.addView(categoryIconsLayout)

        backspaceBtn = ImageView(context).apply {
            layoutParams = LayoutParams((34 * density).toInt(), (32 * density).toInt()).apply {
                marginStart = (4 * density).toInt()
            }
            val bsPad = (5 * density).toInt()
            setPadding(bsPad, bsPad, bsPad, bsPad)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val id = context.resources.getIdentifier("ic_backspace", "drawable", context.packageName)
            if (id != 0) setImageResource(id)
            background = null
            setOnClickListener { onBackspaceClicked?.invoke() }
        }
        bottomNav.addView(backspaceBtn)
        addView(bottomNav)

        // 4. Floating Bottom Inset Bar: Globe on left, Dictation on right
        bottomBarLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (38 * density).toInt())
            val padH = (14 * density).toInt()
            setPadding(padH, 0, padH, 0)
        }

        globeIconView = ImageView(context).apply {
            val size = (24 * density).toInt()
            layoutParams = LayoutParams(size, size)
            val id = context.resources.getIdentifier("ic_globe", "drawable", context.packageName)
            if (id != 0) setImageResource(id)
            setOnClickListener { onGlobeClicked?.invoke() ?: onBackToAlphaClicked?.invoke() }
        }

        val spaceHolder = View(context).apply {
            layoutParams = LayoutParams(0, 1, 1.0f)
        }

        micIconView = ImageView(context).apply {
            val size = (24 * density).toInt()
            layoutParams = LayoutParams(size, size)
            val id = context.resources.getIdentifier("ic_dictation", "drawable", context.packageName)
            if (id != 0) setImageResource(id)
            setOnClickListener { onDictationClicked?.invoke() }
        }

        bottomBarLayout.addView(globeIconView)
        bottomBarLayout.addView(spaceHolder)
        bottomBarLayout.addView(micIconView)
        addView(bottomBarLayout)

        // Setup real-time search
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    adapter.setItems(EmojiData.search(query))
                } else {
                    selectCategory(currentCategory)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

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

        adapter.setItems(emojis)
        recyclerView.scrollToPosition(0)
    }

    private fun updateCategoryIconHighlights() {
        for ((cat, icon) in categoryIconViews) {
            val isSelected = (cat == currentCategory)
            if (isSelected) {
                icon.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (currentTheme.isDark) 0x40FFFFFF else 0x26000000)
                }
                icon.alpha = 1.0f
                icon.setColorFilter(currentTheme.textPrimary)
            } else {
                icon.background = null
                icon.alpha = 0.55f
                icon.setColorFilter(currentTheme.textSecondary)
            }
        }
    }

    private fun onEmojiClicked(unicode: String) {
        if (!recentEmojis.contains(unicode)) {
            recentEmojis.add(0, unicode)
            if (recentEmojis.size > 40) recentEmojis.removeLast()
        }
        onEmojiSelected?.invoke(unicode)
    }

    private fun showEmojiLarpPopup(item: EmojiItem, anchor: View) {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val density = resources.displayMetrics.density
        val rootContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
            background = GradientDrawable().apply {
                setColor(if (currentTheme.isDark) Color.parseColor("#E62C2C2E") else Color.parseColor("#F2FFFFFF"))
                cornerRadius = 16f * density
                setStroke((1.5f * density).toInt(), if (currentTheme.isDark) 0x33FFFFFF else 0x1A000000)
            }
        }

        // Large Preview of Apple Emoji
        val previewTv = EmojiTextView(context).apply {
            text = item.unicode
            textSize = 38f
            gravity = Gravity.CENTER
            layoutParams = LayoutParams((64 * density).toInt(), (64 * density).toInt()).apply {
                bottomMargin = (8 * density).toInt()
            }
        }
        rootContainer.addView(previewTv)

        var currentSelectedVariant = item.unicode

        // If supports skin tone, show horizontal tone picker
        if (item.supportsSkinTone) {
            val tonesRow = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (10 * density).toInt()
                }
            }

            for (tone in EmojiSkinTone.entries) {
                val variant = tone.applyTo(item.unicode)
                val btn = EmojiTextView(context).apply {
                    text = variant
                    textSize = 24f
                    gravity = Gravity.CENTER
                    val size = (38 * density).toInt()
                    layoutParams = LayoutParams(size, size).apply {
                        marginEnd = (4 * density).toInt()
                    }
                    setOnClickListener {
                        currentSelectedVariant = variant
                        previewTv.text = variant
                        onEmojiClicked(variant)
                        dialog.dismiss()
                    }
                }
                tonesRow.addView(btn)
            }
            rootContainer.addView(tonesRow)
        }

        try {
            val token = anchor.windowToken ?: windowToken
            if (token != null) {
                dialog.window?.attributes?.token = token
                dialog.window?.setType(android.view.WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG)
            }
        } catch (_: Throwable) {}

        val isNothing = DeviceDetector.isNothingPhone()
        val shortBrand = DeviceDetector.getShortBrandName()

        if (isNothing) {
            // Nothing Phone: "Larp?" Action Button (Inserts authentic Apple iOS PNG sticker)
            val larpBtn = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                val padH = (16 * density).toInt()
                val padV = (8 * density).toInt()
                setPadding(padH, padV, padH, padV)
                background = GradientDrawable().apply {
                    setColor(currentTheme.accentBlue)
                    cornerRadius = 12f * density
                }
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, (38 * density).toInt())
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    onEmojiLarpRequested?.invoke(currentSelectedVariant)
                    dialog.dismiss()
                }
            }

            val larpText = TextView(context).apply {
                text = "🍎 Larp? (iOS Sticker)"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            larpBtn.addView(larpText)
            rootContainer.addView(larpBtn)
        } else {
            // Non-Nothing device: 1-Tap zFont 3 Auto-Apply button + Insert Sticker option
            val applyBtn = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                val padH = (16 * density).toInt()
                val padV = (8 * density).toInt()
                setPadding(padH, padV, padH, padV)
                background = GradientDrawable().apply {
                    setColor(currentTheme.accentBlue)
                    cornerRadius = 12f * density
                }
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, (38 * density).toInt()).apply {
                    bottomMargin = (8 * density).toInt()
                }
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    dialog.dismiss()
                    ZFontAutomator.autoApplyIOSFont(context, anchor.windowToken ?: windowToken)
                }
            }

            val applyText = TextView(context).apply {
                text = "🚀 Auto-Apply iOS Emojis ($shortBrand)"
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            }
            applyBtn.addView(applyText)
            rootContainer.addView(applyBtn)

            val stickerBtn = TextView(context).apply {
                text = "🍎 Insert as iOS Sticker"
                textSize = 13f
                setTextColor(currentTheme.accentBlue)
                val pad = (6 * density).toInt()
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    onEmojiLarpRequested?.invoke(currentSelectedVariant)
                    dialog.dismiss()
                }
            }
            rootContainer.addView(stickerBtn)
        }

        dialog.setContentView(rootContainer)
        try {
            dialog.show()
        } catch (_: Exception) {}
    }

    fun applyTheme(theme: ThemeColors) {
        currentTheme = theme
        setBackgroundColor(Color.TRANSPARENT)
        recyclerView.setBackgroundColor(Color.TRANSPARENT)
        val density = resources.displayMetrics.density

        searchPill.background = GradientDrawable().apply {
            setColor(if (theme.isDark) 0x33FFFFFF else 0x1F000000)
            cornerRadius = 17f * density
        }
        searchEditText.setTextColor(theme.textPrimary)
        searchEditText.setHintTextColor(theme.textSecondary)

        backToAlphaBtn.setTextColor(theme.textSecondary)
        backToAlphaBtn.background = null

        backspaceBtn.setColorFilter(theme.textSecondary)
        backspaceBtn.background = null

        val bottomIconTint = if (theme.isDark) Color.parseColor("#AEAEB2") else Color.parseColor("#48484A")
        globeIconView.setColorFilter(bottomIconTint)
        micIconView.setColorFilter(bottomIconTint)

        updateCategoryIconHighlights()
    }

    // RecyclerView Adapter for high-performance 60fps emoji rendering
    private class EmojiRecyclerAdapter(
        private val onItemClick: (String) -> Unit,
        private val onItemLongClick: (EmojiItem, View) -> Unit
    ) : RecyclerView.Adapter<EmojiRecyclerAdapter.ViewHolder>() {

        private val items = mutableListOf<EmojiItem>()

        fun setItems(newItems: List<EmojiItem>) {
            items.clear()
            items.addAll(newItems)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val density = parent.context.resources.displayMetrics.density
            val tv = EmojiTextView(parent.context).apply {
                textSize = 28f
                gravity = Gravity.CENTER
                val cellHeight = (44 * density).toInt()
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellHeight)
            }
            return ViewHolder(tv)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tv.text = item.unicode

            val isNothing = DeviceDetector.isNothingPhone()
            if (isNothing) {
                // On Nothing Phone: tap types standard unicode; holding for 2.5-3 seconds pops up "Larp?" bubble
                val holdThresholdMs = 2500L
                var downTime = 0L
                var downX = 0f
                var downY = 0f
                var longPressTriggered = false
                val handler = holder.tv.handler ?: android.os.Handler(android.os.Looper.getMainLooper())
                val longPressRunnable = Runnable {
                    longPressTriggered = true
                    holder.tv.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                    onItemLongClick(item, holder.tv)
                }

                holder.tv.setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        android.view.MotionEvent.ACTION_DOWN -> {
                            downTime = System.currentTimeMillis()
                            downX = event.rawX
                            downY = event.rawY
                            longPressTriggered = false
                            handler.postDelayed(longPressRunnable, holdThresholdMs)
                            false
                        }
                        android.view.MotionEvent.ACTION_MOVE -> {
                            val dx = Math.abs(event.rawX - downX)
                            val dy = Math.abs(event.rawY - downY)
                            val touchSlop = 16f * holder.tv.resources.displayMetrics.density
                            if (dx > touchSlop || dy > touchSlop) {
                                handler.removeCallbacks(longPressRunnable)
                            }
                            false
                        }
                        android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                            handler.removeCallbacks(longPressRunnable)
                            false
                        }
                        else -> false
                    }
                }
                holder.tv.setOnClickListener {
                    if (!longPressTriggered) {
                        onItemClick(item.unicode)
                    }
                }
                holder.tv.setOnLongClickListener(null)
            } else {
                holder.tv.setOnTouchListener(null)
                holder.tv.setOnClickListener { onItemClick(item.unicode) }
                holder.tv.setOnLongClickListener {
                    onItemLongClick(item, holder.tv)
                    true
                }
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(val tv: EmojiTextView) : RecyclerView.ViewHolder(tv)
    }
}
