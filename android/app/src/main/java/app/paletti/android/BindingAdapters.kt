package app.paletti.android

import android.content.res.Resources
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.children
import androidx.databinding.*
import app.paletti.android.views.ColorTile
import com.google.android.material.slider.Slider

fun interface OnValueChanged {
    fun onValueChanged(slider: Slider, value: Float, fromUser: Boolean)
}

fun Int.dpToPx(): Int = TypedValue.applyDimension(
    TypedValue.COMPLEX_UNIT_DIP,
    this.toFloat(),
    Resources.getSystem().displayMetrics
).toInt()

@BindingAdapter("tooltipText")
fun setTooltipText(view: View, stringId: Int) {
    TooltipCompat.setTooltipText(view, view.context.getString(stringId))
}

@BindingAdapter("colors")
fun setColors(view: LinearLayout, colors: ObservableArrayList<Int>) {
    val currentCount = view.childCount
    if (currentCount > colors.size) {
        view.removeViews(colors.size, currentCount - colors.size)
    } else if (currentCount < colors.size) {
        (currentCount until colors.size).forEach { _ ->
            view.addView(ColorTile(view.context).apply {
                val params = LinearLayout.LayoutParams(0, 64.dpToPx()).apply {
                    weight = 1f
                }
                layoutParams = params
            })
        }
    }
    view.children.filterIsInstance<ColorTile>()
        .forEachIndexed { i, colorTile ->
            colorTile.color = colors[i]
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
