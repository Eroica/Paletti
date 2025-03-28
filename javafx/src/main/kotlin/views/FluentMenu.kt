package views

import javafx.animation.TranslateTransition
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.util.Duration
import ui.FluentInterpolator

open class FluentMenu() : ContextMenu() {
    private val slideDown: TranslateTransition by lazy {
        TranslateTransition(Duration.millis(600.0), scene.root).apply {
            interpolator = FluentInterpolator
        }
    }

    constructor(vararg items: MenuItem) : this() {
        this.items.addAll(items)
    }

    override fun show() {
        super.show()
        slideDown.fromY = -height
        slideDown.toY = 4.0
        slideDown.play()
    }
}
