package views

import javafx.animation.Interpolator
import javafx.animation.TranslateTransition
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.util.Duration

private val FluentInterpolator: Interpolator = Interpolator.SPLINE(0.05, 0.965, 0.005, 0.955)

class FluentMenu(vararg items: MenuItem) : ContextMenu(*items) {
    private val slideDown: TranslateTransition by lazy {
        TranslateTransition(Duration.millis(600.0), scene.root).apply {
            interpolator = FluentInterpolator
        }
    }

    override fun show() {
        super.show()
        slideDown.fromY = -height
        slideDown.toY = 4.0
        slideDown.play()
    }
}
