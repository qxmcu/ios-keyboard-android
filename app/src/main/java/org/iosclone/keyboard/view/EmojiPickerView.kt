package org.iosclone.keyboard.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
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
    var onGlobeClicked: (() -> Unit)? = null
    var onDictationClicked: (() -> Unit)? = null

    private val searchEditText: EditText
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

        val searchPill = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val padPillH = (10 * density).toInt()
            val padPillV = (6 * density).toInt()
            setPadding(padPillH, padPillV, padPillH, padPillV)
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (34 * density).toInt())
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#E3E5E8"))
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

        // 2. Ultra-Smooth RecyclerView Emoji Grid
        recyclerView = RecyclerView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1.0f)
            layoutManager = GridLayoutManager(context, 8)
            setHasFixedSize(true)
            setItemViewCacheSize(64)
            val gridPad = (4 * density).toInt()
            setPadding(gridPad, 0, gridPad, 0)
        }

        adapter = EmojiRecyclerAdapter(
            onItemClick = { emoji -> onEmojiClicked(emoji) },
            onItemLongClick = { item, anchor -> showSkinToneSelector(item, anchor) }
        )
        recyclerView.adapter = adapter
        addView(recyclerView)

        // 3. Bottom Bar: ABC, Category Strip, Backspace
        bottomNav = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, (42 * density).toInt())
            val navPad = (6 * density).toInt()
            setPadding(navPad, 0, navPad, 0)
        }

        backToAlphaBtn = TextView(context).apply {
            text = "ABC"
            textSize = 15f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            layoutParams = LayoutParams((46 * density).toInt(), (34 * density).toInt()).apply {
                marginEnd = (4 * density).toInt()
            }
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B2B6BE"))
                cornerRadius = 5f * density
            }
            setOnClickListener { onBackToAlphaClicked?.invoke() }
        }
        bottomNav.addView(backToAlphaBtn)

        val catScrollView = HorizontalScrollView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f)
            isHorizontalScrollBarEnabled = false
        }

        val categoryIconsLayout = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        }

        val catIconSize = (20 * density).toInt()
        val catIconPad = (7 * density).toInt()

        for (cat in EmojiCategory.entries) {
            val iconView = ImageView(context).apply {
                layoutParams = LayoutParams(catIconSize + (catIconPad * 2), LayoutParams.MATCH_PARENT)
                setPadding(catIconPad, catIconPad, catIconPad, catIconPad)
                val id = context.resources.getIdentifier(cat.iconResName, "drawable", context.packageName)
                if (id != 0) setImageResource(id)
                setOnClickListener { selectCategory(cat) }
            }
            categoryIconViews[cat] = iconView
            categoryIconsLayout.addView(iconView)
        }
        catScrollView.addView(categoryIconsLayout)
        bottomNav.addView(catScrollView)

        backspaceBtn = ImageView(context).apply {
            layoutParams = LayoutParams((42 * density).toInt(), (34 * density).toInt()).apply {
                marginStart = (4 * density).toInt()
            }
            val bsPad = (7 * density).toInt()
            setPadding(bsPad, bsPad, bsPad, bsPad)
            val id = context.resources.getIdentifier("ic_backspace", "drawable", context.packageName)
            if (id != 0) setImageResource(id)
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#B2B6BE"))
                cornerRadius = 5f * density
            }
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
            icon.alpha = if (isSelected) 1.0f else 0.40f
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
            background = GradientDrawable().apply {
                setColor(if (currentTheme.isDark) Color.parseColor("#3A3A3C") else Color.parseColor("#FFFFFF"))
                cornerRadius = 12f * density
                setStroke((1 * density).toInt(), if (currentTheme.isDark) 0x33FFFFFF else 0x22000000)
            }
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
        currentTheme = theme
        setBackgroundColor(theme.keyboardBackground)
        recyclerView.setBackgroundColor(theme.keyboardBackground)

        searchEditText.setTextColor(theme.textPrimary)
        searchEditText.setHintTextColor(theme.textSecondary)

        backToAlphaBtn.setTextColor(theme.textPrimary)
        backToAlphaBtn.background = GradientDrawable().apply {
            setColor(theme.modifierKeyBackground)
            cornerRadius = 5f * resources.displayMetrics.density
        }

        backspaceBtn.setColorFilter(theme.textPrimary)
        backspaceBtn.background = GradientDrawable().apply {
            setColor(theme.modifierKeyBackground)
            cornerRadius = 5f * resources.displayMetrics.density
        }

        globeIconView.setColorFilter(theme.textPrimary)
        micIconView.setColorFilter(theme.textPrimary)

        for ((_, icon) in categoryIconViews) {
            icon.setColorFilter(theme.textPrimary)
        }
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
            holder.tv.setOnClickListener { onItemClick(item.unicode) }
            if (item.supportsSkinTone) {
                holder.tv.setOnLongClickListener {
                    onItemLongClick(item, holder.tv)
                    true
                }
            } else {
                holder.tv.setOnLongClickListener(null)
            }
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(val tv: EmojiTextView) : RecyclerView.ViewHolder(tv)
    }
}
