package app.paletti.android.views

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.withStyledAttributes
import app.paletti.android.R

class ColorTile @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    var color: Int = Color.TRANSPARENT
        set(value) {
            field = value
            setBackgroundColor(value)
        }

    init {
        context.withStyledAttributes(attrs, R.styleable.ColorTile) {
            color = getColor(R.styleable.ColorTile_color, Color.TRANSPARENT)
        }
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        foreground = AppCompatResources.getDrawable(context, typedValue.resourceId)
        setOnClickListener { copyToClipboard() }
    }

    private fun copyToClipboard() {
        getSystemService(context, ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("Color", "#%06X".format(0xFFFFFF and color)))
        Toast.makeText(context, "#%06X".format(0xFFFFFF and color), Toast.LENGTH_SHORT).show()
    }
}
