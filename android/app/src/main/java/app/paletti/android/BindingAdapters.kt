package app.paletti.android

import android.content.ClipData
import android.content.ClipboardManager
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.databinding.*
import app.paletti.android.databinding.ListItemColorTileBinding
import com.google.android.material.slider.Slider

fun interface OnValueChanged {
    fun onValueChanged(slider: Slider, value: Float, fromUser: Boolean)
}

@BindingAdapter("tooltipText")
fun setTooltipText(view: View, stringId: Int) {
    TooltipCompat.setTooltipText(view, view.context.getString(stringId))
}

@BindingAdapter("colors")
fun setColors(view: LinearLayout, colors: ObservableArrayList<Int>) {
    val currentCount = view.childCount
    val inflater = LayoutInflater.from(view.context)
    if (currentCount > colors.size) {
        view.removeViews(colors.size, currentCount - colors.size)
    } else if (currentCount < colors.size) {
        (currentCount until colors.size).forEach { _ ->
            val binding = DataBindingUtil.inflate<ListItemColorTileBinding>(
                inflater,
                R.layout.list_item_color_tile,
                view,
                true
            )
            binding.root.setOnClickListener { view ->
                getSystemService(view.context, ClipboardManager::class.java)
                    ?.setPrimaryClip(ClipData.newPlainText("Color", "#%06X".format(0xFFFFFF and view.tag as Int)))
                Toast.makeText(view.context, "#%06X".format(0xFFFFFF and view.tag as Int), Toast.LENGTH_SHORT).show()
            }
        }
    }
    colors.forEachIndexed { i, color ->
        view.getChildAt(i).apply {
            tag = color
            setBackgroundColor(color)
        }
    }
}

@BindingAdapter("value")
fun setSliderValue(slider: Slider, newValue: Float) {
    if (slider.value != newValue) {
        slider.value = newValue
    }
}

@InverseBindingAdapter(attribute = "android:value")
fun getSliderValue(slider: Slider) = slider.value

@BindingAdapter(value = ["onValueChanged", "android:valueAttrChanged"], requireAll = false)
fun setSliderListeners(slider: Slider, valueChanged: OnValueChanged, attrChange: InverseBindingListener) {
    slider.addOnChangeListener { v, value, fromUser ->
        valueChanged.onValueChanged(v, value, fromUser)
        attrChange.onChange()
    }
}
